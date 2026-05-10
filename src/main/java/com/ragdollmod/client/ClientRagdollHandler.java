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

    public static void setLocalRagdollActive(boolean active) {
        localActive = active;
        clientPhysics = null; // always reset physics on state change
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                Component.literal(active ? "§aRagdoll ON §7(R to toggle)" : "§cRagdoll OFF"),
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

        // Toggle key
        while (RagdollKeyBindings.TOGGLE_RAGDOLL.consumeClick()) {
            RagdollNetwork.sendToggle();
            player.displayClientMessage(Component.literal("§eSending ragdoll toggle..."), true);
        }

        if (!localActive) return;

        // Init physics at player position if needed
        if (clientPhysics == null) {
            clientPhysics = new RagdollPhysics(player.getX(), player.getY(), player.getZ());
            // Give initial downward velocity so it falls immediately
            for (RagdollSegment seg : clientPhysics.segments) {
                seg.applyImpulse(0, -0.3, 0);
            }
        }

        // Gather movement input
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
            if (fwd)   { moveX += -sinYaw; moveZ += cosYaw; }
            if (back)  { moveX -=  -sinYaw; moveZ -= cosYaw; }
            if (right) { moveX +=  cosYaw; moveZ += sinYaw; }
            if (left)  { moveX -=  cosYaw; moveZ -= sinYaw; }
            double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
            if (len > 1e-6) { moveX /= len; moveZ /= len; }
        }

        // Step physics
        clientPhysics.applyLocomotionImpulse(moveX, moveZ, jump, yaw);
        clientPhysics.tick(mc.level);

        // Move player entity to torso position so camera follows
        Vec3 root = clientPhysics.rootPosition();
        double feetY = clientPhysics.feetY();
        // Smoothly move player entity toward ragdoll torso
        player.setPosRaw(
            root.x * 0.5 + player.getX() * 0.5,
            feetY,
            root.z * 0.5 + player.getZ() * 0.5
        );
        // Kill vanilla movement completely
        player.setDeltaMovement(Vec3.ZERO);

        // Send to server every 2 ticks
        sendTimer++;
        if (sendTimer >= 2) {
            sendTimer = 0;
            RagdollNetwork.sendInput(moveX, moveZ, jump, yaw);
        }
    }
}
