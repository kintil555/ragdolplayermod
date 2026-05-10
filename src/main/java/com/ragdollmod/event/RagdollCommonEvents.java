package com.ragdollmod.event;

import com.ragdollmod.common.capability.RagdollCapability;
import com.ragdollmod.common.capability.RagdollCapabilityAttacher;
import com.ragdollmod.common.physics.RagdollPhysics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class RagdollCommonEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        RagdollCapability cap = RagdollCapabilityAttacher.get(sp);
        if (!cap.isActive()) return;

        RagdollPhysics phys = cap.getPhysics();
        if (phys == null) return;

        // 1. Apply player input as locomotion impulse
        phys.applyLocomotionImpulse(cap.moveX, cap.moveZ, cap.jumping, cap.yaw);

        // 2. Step the physics simulation
        phys.tick(sp.level());

        // 3. Move the actual player entity to follow the ragdoll torso
        //    so hitbox, chunk loading, etc. stay correct.
        Vec3 root = phys.rootPosition();
        // Teleport without triggering portal checks
        sp.moveTo(root.x, phys.feetY(), root.z, sp.getYRot(), sp.getXRot());

        // 4. Prevent vanilla movement from interfering
        sp.setDeltaMovement(Vec3.ZERO);
    }
}
