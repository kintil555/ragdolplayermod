package com.ragdollmod.client;

import com.ragdollmod.common.physics.RagdollPhysics;
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

    public static void setLocalRagdollActive(boolean active) {
        localActive = active;
        if (!active) {
            clientPhysics = null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                Component.literal(active ? "§aRagdoll ON (R to toggle)" : "§cRagdoll OFF"),
                true
            );
        }
    }

    public static boolean isActive() { return localActive; }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        // ── Key press: toggle ragdoll ──────────────────────────────
        while (RagdollKeyBindings.TOGGLE_RAGDOLL.consumeClick()) {
            RagdollNetwork.sendToggle();
            // Show immediate local feedback before server responds
            player.displayClientMessage(Component.literal("§eSending ragdoll toggle..."), true);
        }

        if (!localActive) return;

        // ── Gather input ───────────────────────────────────────────
        float yaw = (float) Math.toRadians(player.getYRot());
        double moveX = 0, moveZ = 0;
        boolean fwd   = mc.options.keyUp.isDown();
        boolean back  = mc.options.keyDown.isDown();
        boolean left  = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        boolean jump  = mc.options.keyJump.isDown();

        if (fwd || back || left || right) {
            double sinYaw = Math.sin(yaw);
            double cosYaw = Math.cos(yaw);
            double fwdX = -sinYaw, fwdZ = cosYaw;
            double sideX = cosYaw,  sideZ = sinYaw;
            if (fwd)   { moveX += fwdX;  moveZ += fwdZ; }
            if (back)  { moveX -= fwdX;  moveZ -= fwdZ; }
            if (right) { moveX += sideX; moveZ += sideZ; }
            if (left)  { moveX -= sideX; moveZ -= sideZ; }
            double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
            if (len > 1e-6) { moveX /= len; moveZ /= len; }
        }

        // ── Suppress vanilla movement ──────────────────────────────
        // Use setDeltaMovement instead of touching input fields directly
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.setOnGround(true); // prevent fall damage accumulation

        // ── Client-side physics mirror ─────────────────────────────
        if (clientPhysics == null) {
            clientPhysics = new RagdollPhysics(player.getX(), player.getY(), player.getZ());
        }
        clientPhysics.applyLocomotionImpulse(moveX, moveZ, jump, yaw);
        clientPhysics.tick(mc.level);

        // ── Send input to server every 2 ticks ────────────────────
        sendTimer++;
        if (sendTimer >= 2) {
            sendTimer = 0;
            RagdollNetwork.sendInput(moveX, moveZ, jump, yaw);
        }
    }
}
