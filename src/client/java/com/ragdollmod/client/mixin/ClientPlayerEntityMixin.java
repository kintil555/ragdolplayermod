package com.ragdollmod.client.mixin;

import com.ragdollmod.client.ClientRagdollHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    /**
     * Cancel vanilla tick() while ragdoll is active.
     * In Yarn 1.21.1, ClientPlayerEntity overrides tick() (not tickMovement()).
     * The old mod targeted tickMovement which doesn't exist on ClientPlayerEntity — that was the crash.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ragdoll_onTick(CallbackInfo ci) {
        if (ClientRagdollHandler.isActive()) {
            ClientRagdollHandler.tick();
            ci.cancel();
        }
    }
}
