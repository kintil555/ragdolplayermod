package com.ragdollmod.client.mixin;

import com.ragdollmod.client.ClientRagdollHandler;
import com.ragdollmod.client.renderer.RagdollPlayerRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void ragdoll_preRender(AbstractClientPlayerEntity player, float yaw, float tickDelta,
                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                    int light, CallbackInfo ci) {
        if (!ClientRagdollHandler.isActive() || ClientRagdollHandler.clientPhysics == null) return;
        PlayerEntityRenderer self = (PlayerEntityRenderer)(Object)this;
        RagdollPlayerRenderer.applyPose((PlayerEntityModel<?>)self.getModel());
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ragdoll_postRender(AbstractClientPlayerEntity player, float yaw, float tickDelta,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, CallbackInfo ci) {
        if (!ClientRagdollHandler.isActive()) return;
        PlayerEntityRenderer self = (PlayerEntityRenderer)(Object)this;
        RagdollPlayerRenderer.resetPose((PlayerEntityModel<?>)self.getModel());
    }
}
