package com.ragdollmod.mixin;

import com.ragdollmod.physics.RagdollPhysics;
import com.ragdollmod.physics.RagdollState;
import com.ragdollmod.physics.RagdollStateManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerMixin {

    /**
     * After the vanilla player tick, run ragdoll physics if active.
     * We hook POST-tick so vanilla movement has already been processed and we
     * can override the final position cleanly.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void ragdoll_onTick(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity)(Object) this;
        RagdollState state = RagdollStateManager.get(self);
        if (!state.isActive()) return;

        RagdollPhysics phys = state.getPhysics();
        if (phys == null) return;

        // 1. Feed locomotion input from latest client packet
        phys.applyLocomotionImpulse(state.moveX, state.moveZ, state.yaw);

        // 2. Advance simulation
        phys.tick(self.getWorld());

        // 3. Move actual player entity to follow ragdoll torso
        //    (keeps chunk-loading, hitbox and server position correct)
        Vec3d root = phys.rootPosition();
        self.requestTeleport(root.x, phys.feetY(), root.z);

        // 4. Zero out vanilla momentum so server never fights physics
        self.setVelocity(Vec3d.ZERO);

        // 5. Suppress vanilla fall damage while ragdolling
        self.fallDistance = 0;
    }
}
