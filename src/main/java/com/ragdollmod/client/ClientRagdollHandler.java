package com.ragdollmod.client;

import com.ragdollmod.common.physics.RagdollPhysics;
import com.ragdollmod.network.RagdollNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@OnlyIn(Dist.CLIENT)
public class ClientRagdollHandler {

    // Tracks whether the server confirmed ragdoll is on
    private static boolean localActive = false;

    // Client-side physics mirror for smooth rendering (interpolated)
    public static RagdollPhysics clientPhysics = null;

    // Tick counter to throttle network sends
    private static int sendTimer = 0;

    public static void setLocalRagdollActive(boolean active) {
        localActive = active;
        if (!active) {
            clientPhysics = null;
        }
        // Show feedback
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                Component.translatable(active
                    ? "message.playerragdoll.ragdoll_on"
                    : "message.playerragdoll.ragdoll_off"),
                true // action bar
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
        }

        if (!localActive) return;

        // ── Gather input from Minecraft's own key state ────────────
        float yaw = (float) Math.toRadians(player.getYRot());

        double moveX = 0, moveZ = 0;
        boolean moving = false;

        // Read raw key states
        boolean fwd   = mc.options.keyUp.isDown();
        boolean back  = mc.options.keyDown.isDown();
        boolean left  = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        boolean jump  = mc.options.keyJump.isDown();

        if (fwd || back || left || right) {
            // Compute world-space direction from player facing
            double sinYaw = Math.sin(yaw);
            double cosYaw = Math.cos(yaw);

            double fwdX = -sinYaw, fwdZ = cosYaw;   // forward vector
            double sideX = cosYaw,  sideZ = sinYaw;  // right vector

            if (fwd)   { moveX += fwdX;  moveZ += fwdZ; }
            if (back)  { moveX -= fwdX;  moveZ -= fwdZ; }
            if (right) { moveX += sideX; moveZ += sideZ; }
            if (left)  { moveX -= sideX; moveZ -= sideZ; }

            // Normalize
            double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
            if (len > 1e-6) { moveX /= len; moveZ /= len; }
            moving = true;
        }

        // ── Suppress vanilla movement while ragdoll is active ──────
        // We zero out the vanilla inputs so the server doesn't fight our positions
        player.input.forwardImpulse = 0;
        player.input.leftImpulse    = 0;
        player.input.jumping        = false;

        // ── Client-side physics mirror for smooth rendering ────────
        if (clientPhysics == null) {
            clientPhysics = new RagdollPhysics(player.getX(), player.getY(), player.getZ());
        }
        clientPhysics.applyLocomotionImpulse(moveX, moveZ, jump, yaw);
        clientPhysics.tick(mc.level);

        // ── Send input to server (every 2 ticks to save bandwidth) ─
        sendTimer++;
        if (sendTimer >= 2) {
            sendTimer = 0;
            final double fx = moveX, fz = moveZ;
            RagdollNetwork.sendInput(fx, fz, jump, yaw);
        }
    }
}
