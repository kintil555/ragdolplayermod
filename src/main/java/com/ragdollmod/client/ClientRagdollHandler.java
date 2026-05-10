package com.ragdollmod.client;

import com.ragdollmod.common.physics.RagdollPhysics;
import com.ragdollmod.common.physics.RagdollSegment;
import com.ragdollmod.network.RagdollNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@OnlyIn(Dist.CLIENT)
public class ClientRagdollHandler {

    private static boolean localActive = false;
    public static RagdollPhysics clientPhysics = null;
    private static int sendTimer = 0;

    // Smooth interpolated values for rendering
    public static float smoothBodyX = 0f;
    public static float smoothBodyZ = 0f;
    public static float smoothHeadX = 0f;
    public static float smoothLeftArmX = 0f;
    public static float smoothRightArmX = 0f;
    public static float smoothLeftLegX = 0f;
    public static float smoothRightLegX = 0f;

    public static void setLocalRagdollActive(boolean active) {
        localActive = active;
        clientPhysics = null;
        resetSmooth();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                Component.literal(active ? "§aRagdoll ON §7(R)" : "§cRagdoll OFF"),
                true
            );
        }
    }

    private static void resetSmooth() {
        smoothBodyX = smoothBodyZ = smoothHeadX = 0f;
        smoothLeftArmX = smoothRightArmX = 0f;
        smoothLeftLegX = smoothRightLegX = 0f;
    }

    public static boolean isActive() { return localActive; }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        while (RagdollKeyBindings.TOGGLE_RAGDOLL.consumeClick()) {
            RagdollNetwork.sendToggle();
            player.displayClientMessage(Component.literal("§eToggling ragdoll..."), true);
        }

        if (!localActive) return;

        // Init physics
        if (clientPhysics == null) {
            clientPhysics = new RagdollPhysics(player.getX(), player.getY(), player.getZ());
            // Immediately apply a strong fall + random tumble
            for (RagdollSegment seg : clientPhysics.segments) {
                seg.applyImpulse(
                    (Math.random() - 0.5) * 0.15,
                    -0.5,
                    (Math.random() - 0.5) * 0.15
                );
            }
        }

        // NO jump impulse when space held — only allow WASD crawl
        float yaw = (float) Math.toRadians(player.getYRot());
        double moveX = 0, moveZ = 0;
        boolean fwd   = mc.options.keyUp.isDown();
        boolean back  = mc.options.keyDown.isDown();
        boolean left  = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        // Intentionally ignore jump key — no flying

        if (fwd || back || left || right) {
            double sin = Math.sin(yaw), cos = Math.cos(yaw);
            if (fwd)   { moveX += -sin; moveZ += cos; }
            if (back)  { moveX +=  sin; moveZ -= cos; }
            if (right) { moveX +=  cos; moveZ += sin; }
            if (left)  { moveX -=  cos; moveZ -= sin; }
            double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
            if (len > 1e-6) { moveX /= len; moveZ /= len; }
        }

        // Tick physics — pass false for jumping to prevent space bug
        clientPhysics.applyLocomotionImpulse(moveX, moveZ, false, yaw);
        clientPhysics.tick(mc.level);

        // Update smooth pose values (lerp toward target)
        RagdollSegment torso  = clientPhysics.segments[RagdollPhysics.TORSO];
        RagdollSegment head   = clientPhysics.segments[RagdollPhysics.HEAD];
        RagdollSegment lArm   = clientPhysics.segments[RagdollPhysics.L_ARM];
        RagdollSegment rArm   = clientPhysics.segments[RagdollPhysics.R_ARM];
        RagdollSegment lThigh = clientPhysics.segments[RagdollPhysics.L_THIGH];
        RagdollSegment rThigh = clientPhysics.segments[RagdollPhysics.R_THIGH];

        Vec3 tv = torso.velocity();
        // Target angles: when torso falls forward (vy<0), body tips forward ~90 deg
        // Map vy to body pitch: falling = lean forward strongly
        float targetBodyX = clamp((float)(-tv.y * 12.0 + tv.z * 6.0), -(float)Math.PI, (float)Math.PI);
        float targetBodyZ = clamp((float)(tv.x * 8.0), -1.5f, 1.5f);

        float dxHeadTorso = (float)(head.x - torso.x);
        float dyHeadTorso = (float)(head.y - torso.y - 0.35);
        float targetHeadX = clamp(dyHeadTorso * 4.0f + targetBodyX * 0.3f, -1.8f, 1.8f);
        float targetHeadZ = clamp(dxHeadTorso * 5.0f, -1.2f, 1.2f);

        float targetLArmX = clamp((float)(lArm.y - torso.y + 0.2) * 5.0f, -2.5f, 2.5f);
        float targetRArmX = clamp((float)(rArm.y - torso.y + 0.2) * 5.0f, -2.5f, 2.5f);
        float targetLLegX = clamp((float)(lThigh.y - torso.y + 0.6) * 6.0f, -2.5f, 2.5f);
        float targetRLegX = clamp((float)(rThigh.y - torso.y + 0.6) * 6.0f, -2.5f, 2.5f);

        float lerp = 0.3f;
        smoothBodyX     = lerp(smoothBodyX, targetBodyX, lerp);
        smoothBodyZ     = lerp(smoothBodyZ, targetBodyZ, lerp);
        smoothHeadX     = lerp(smoothHeadX, targetHeadX, lerp);
        smoothLeftArmX  = lerp(smoothLeftArmX, targetLArmX, lerp);
        smoothRightArmX = lerp(smoothRightArmX, targetRArmX, lerp);
        smoothLeftLegX  = lerp(smoothLeftLegX, targetLLegX, lerp);
        smoothRightLegX = lerp(smoothRightLegX, targetRLegX, lerp);

        // Kill vanilla movement — do NOT move entity position (prevents flickering)
        player.setDeltaMovement(Vec3.ZERO);

        // Send to server
        sendTimer++;
        if (sendTimer >= 2) {
            sendTimer = 0;
            RagdollNetwork.sendInput(moveX, moveZ, false, yaw);
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
