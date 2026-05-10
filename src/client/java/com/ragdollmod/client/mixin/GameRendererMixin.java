package com.ragdollmod.client.mixin;

import com.ragdollmod.client.ClientRagdollHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GameRenderer mixin — guards the first-person hand render against the ragdoll state.
 *
 * When ragdolling, the first-person hand render reads the arm model which may
 * be in an unusual pose; this mixin suppresses the hand render entirely during
 * ragdoll to prevent visual glitches and potential NPE crashes in F5.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(
        method = "renderHand",
        at = @At("HEAD"),
        cancellable = true
    )
    private void ragdoll_suppressHand(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (ClientRagdollHandler.isActive()) {
            ci.cancel(); // No floating hand while you're lying on the ground
        }
    }
}
