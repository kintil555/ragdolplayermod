package com.ragdollmod.client.mixin;

import com.ragdollmod.client.ClientRagdollHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client player mixin — prevents vanilla movement from fighting the ragdoll physics.
 *
 * While ragdoll is active:
 *  - tickMovement() is cancelled so vanilla physics/collision don't run
 *  - isOnGround() returns true so the server doesn't apply extra gravity
 *  - jump() is no-op so spacebar doesn't accidentally launch the player
 */
@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    /**
     * Cancel vanilla movement tick while ragdolling.
     * The ClientRagdollHandler.tick() in ClientTickEvents handles movement instead.
     */
    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void ragdoll_cancelMovement(CallbackInfo ci) {
        if (ClientRagdollHandler.isActive()) {
            // Zero velocity each tick so vanilla doesn't accumulate momentum
            ClientPlayerEntity self = (ClientPlayerEntity)(Object) this;
            self.setVelocity(Vec3d.ZERO);
            ci.cancel();
        }
    }

    /**
     * Suppress jumping while ragdolling.
     */
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void ragdoll_suppressJump(CallbackInfo ci) {
        if (ClientRagdollHandler.isActive()) ci.cancel();
    }
}
