package com.ragdollmod.client;

import com.ragdollmod.network.RagdollNetwork;
import com.ragdollmod.physics.RagdollPhysics;
import com.ragdollmod.physics.RagdollStateManager;
import com.ragdollmod.physics.XPBDSegment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Drives the client-side ragdoll simulation.
 *
 * The client runs its own physics copy in parallel with the server.
 * This eliminates the visual lag that would appear if we only showed
 * server-authoritative positions (which arrive 1–2 ticks late).
 *
 * The server is authoritative for gameplay (position, fall damage, etc.).
 * The client simulation is purely for visuals — it is reset every time
 * the server sends a sync packet.
 *
 * Rendering state exposed as public static fields so the Mixin can read
 * them without passing references.
 */
@Environment(EnvType.CLIENT)
public class ClientRagdollHandler {

    private static boolean active = false;

    /** Client-side physics instance (may be null if not yet initialized). */
    public static RagdollPhysics physics = null;

    // ── Smooth interpolated render pose ─────────────────────────────────────
    // All values in radians, updated each tick, read by the renderer mixin.

    public static float bodyPitch  = 0f;   // body forward tilt
    public static float bodyRoll   = 0f;   // body sideways lean
    public static float headPitch  = 0f;
    public static float headYaw    = 0f;   // head twist relative to body
    public static float lArmPitch  = 0f;
    public static float rArmPitch  = 0f;
    public static float lArmRoll   = 0f;
    public static float rArmRoll   = 0f;
    public static float lLegPitch  = 0f;
    public static float rLegPitch  = 0f;
    public static float lForeAngle = 0f;
    public static float rForeAngle = 0f;
    public static float lShinAngle = 0f;
    public static float rShinAngle = 0f;

    /** Position offset used by renderer to shift the model to the ragdoll location. */
    public static double renderOffsetX = 0;
    public static double renderOffsetY = 0;
    public static double renderOffsetZ = 0;

    // ── Internal ─────────────────────────────────────────────────────────────
    private static int inputSendTimer = 0;

    // ── Public API ────────────────────────────────────────────────────────────

    public static boolean isActive() { return active; }

    public static void setActive(boolean newActive) {
        active = newActive;
        if (!newActive) {
            physics = null;
            resetPose();
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(
                Text.literal(newActive ? "§aRagdoll ON §7(R to stand up)" : "§cStanding up..."),
                true
            );
        }
    }

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || !active) return;

        // ── Init physics if needed ──────────────────────────────────────────
        if (physics == null) {
            physics = new RagdollPhysics(player.getX(), player.getY(), player.getZ());
            // Apply tumble impulse matching server-side RagdollState.enable()
            for (XPBDSegment seg : physics.segments) {
                double rx = (Math.random() - 0.5) * 0.12;
                double rz = (Math.random() - 0.5) * 0.12;
                seg.applyImpulse(rx, -0.35, rz);
                seg.wx = (Math.random() - 0.5) * 0.8;
                seg.wz = (Math.random() - 0.5) * 0.8;
            }
        }

        // ── Read WASD input ─────────────────────────────────────────────────
        float yaw = (float) Math.toRadians(player.getYaw());
        double moveX = 0, moveZ = 0;

        boolean fwd   = mc.options.forwardKey.isPressed();
        boolean back  = mc.options.backKey.isPressed();
        boolean left  = mc.options.leftKey.isPressed();
        boolean right = mc.options.rightKey.isPressed();

        if (fwd || back || left || right) {
            double sin = Math.sin(yaw), cos = Math.cos(yaw);
            if (fwd)   { moveX -= sin; moveZ += cos; }
            if (back)  { moveX += sin; moveZ -= cos; }
            if (right) { moveX += cos; moveZ += sin; }
            if (left)  { moveX -= cos; moveZ -= sin; }
            double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
            if (len > 1e-6) { moveX /= len; moveZ /= len; }
        }

        // ── Tick physics ────────────────────────────────────────────────────
        physics.applyLocomotionImpulse(moveX, moveZ, yaw);
        physics.tick(mc.world);

        // ── Suppress vanilla movement ───────────────────────────────────────
        player.setVelocity(Vec3d.ZERO);

        // ── Update render pose ──────────────────────────────────────────────
        updateRenderPose(player);

        // ── Send input to server every 2 ticks ─────────────────────────────
        if (++inputSendTimer >= 2) {
            inputSendTimer = 0;
            if (moveX != 0 || moveZ != 0) {
                ClientPlayNetworking.send(new RagdollNetwork.InputPayload(moveX, moveZ, yaw));
            }
        }
    }

    // ── Render pose derivation ────────────────────────────────────────────────

    /**
     * Derives per-limb render angles from the physics segment positions.
     *
     * Strategy: each limb angle is derived from the *relative offset* between
     * the limb's segment and its parent attachment point on the torso.
     * This maps physical displacement → visual rotation, so what you see
     * matches where the physics says the limb actually is.
     */
    private static void updateRenderPose(ClientPlayerEntity player) {
        XPBDSegment torso  = physics.segments[RagdollPhysics.TORSO];
        XPBDSegment head   = physics.segments[RagdollPhysics.HEAD];
        XPBDSegment lArm   = physics.segments[RagdollPhysics.L_ARM];
        XPBDSegment rArm   = physics.segments[RagdollPhysics.R_ARM];
        XPBDSegment lFore  = physics.segments[RagdollPhysics.L_FORE];
        XPBDSegment rFore  = physics.segments[RagdollPhysics.R_FORE];
        XPBDSegment lThigh = physics.segments[RagdollPhysics.L_THIGH];
        XPBDSegment rThigh = physics.segments[RagdollPhysics.R_THIGH];
        XPBDSegment lShin  = physics.segments[RagdollPhysics.L_SHIN];
        XPBDSegment rShin  = physics.segments[RagdollPhysics.R_SHIN];

        Vec3d tv = new Vec3d(torso.velX(), torso.velY(), torso.velZ());

        // ── Body ─────────────────────────────────────────────────────────────
        // Body pitch: forward fall → pitch forward (~-PI/2 when lying flat)
        float targetBodyPitch = clamp(
            (float)(torso.pitch + tv.y * -8.0 + tv.z * 4.0),
            -(float)Math.PI, (float)Math.PI
        );
        float targetBodyRoll = clamp((float)(torso.roll + tv.x * 6.0), -2.0f, 2.0f);

        bodyPitch = lerp(bodyPitch, targetBodyPitch, 0.35f);
        bodyRoll  = lerp(bodyRoll,  targetBodyRoll,  0.35f);

        // ── Render offset (shift model to physics position) ──────────────────
        renderOffsetX = torso.x - player.getX();
        renderOffsetY = physics.feetY() - player.getY();
        renderOffsetZ = torso.z - player.getZ();

        // ── Head ──────────────────────────────────────────────────────────────
        double dhx = head.x - torso.x;
        double dhy = head.y - (torso.y + 0.4);
        headPitch = lerp(headPitch, clamp((float)(head.pitch + dhy * 3.5f), -1.8f, 1.8f), 0.3f);
        headYaw   = lerp(headYaw,   clamp((float)(dhx * 5.0f),              -1.2f, 1.2f), 0.3f);

        // ── Arms ──────────────────────────────────────────────────────────────
        lArmPitch  = lerp(lArmPitch,  segAngle(lArm,  torso, 0.25, true),  0.3f);
        rArmPitch  = lerp(rArmPitch,  segAngle(rArm,  torso, 0.25, true),  0.3f);
        lArmRoll   = lerp(lArmRoll,   (float)(lArm.roll  * 0.8), 0.3f);
        rArmRoll   = lerp(rArmRoll,   (float)(rArm.roll  * 0.8), 0.3f);
        lForeAngle = lerp(lForeAngle, segAngle(lFore, lArm,  0.0,  true),  0.3f);
        rForeAngle = lerp(rForeAngle, segAngle(rFore, rArm,  0.0,  true),  0.3f);

        // ── Legs ──────────────────────────────────────────────────────────────
        lLegPitch  = lerp(lLegPitch,  segAngle(lThigh, torso, 0.55, true),  0.3f);
        rLegPitch  = lerp(rLegPitch,  segAngle(rThigh, torso, 0.55, true),  0.3f);
        lShinAngle = lerp(lShinAngle, segAngle(lShin,  lThigh, 0.0, true),  0.3f);
        rShinAngle = lerp(rShinAngle, segAngle(rShin,  rThigh, 0.0, true),  0.3f);
    }

    /**
     * Compute a pitch angle for a segment relative to its parent.
     *
     * @param seg      child segment
     * @param parent   parent segment
     * @param yOffset  expected Y offset at rest (used to measure deviation)
     * @param useOwn   whether to blend in the segment's own physical pitch
     */
    private static float segAngle(XPBDSegment seg, XPBDSegment parent,
                                  double yOffset, boolean useOwn) {
        double dy = seg.y - (parent.y - yOffset);
        float base = clamp((float)(dy * 4.5), -2.5f, 2.5f);
        if (useOwn) base = base * 0.6f + (float)(seg.pitch * 0.4);
        return clamp(base, -2.5f, 2.5f);
    }

    private static void resetPose() {
        bodyPitch = bodyRoll = 0;
        headPitch = headYaw = 0;
        lArmPitch = rArmPitch = lArmRoll = rArmRoll = 0;
        lForeAngle = rForeAngle = 0;
        lLegPitch = rLegPitch = 0;
        lShinAngle = rShinAngle = 0;
        renderOffsetX = renderOffsetY = renderOffsetZ = 0;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float clamp(float v, float mn, float mx) { return Math.max(mn, Math.min(mx, v)); }
}
