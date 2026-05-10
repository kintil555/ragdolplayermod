package com.ragdollmod.client.mixin;

import com.ragdollmod.client.ClientRagdollHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts the player render call.
 *
 * When ragdoll is active:
 *  1. Translates the MatrixStack to the physics torso position
 *  2. After vanilla model setup, overrides every limb angle with physics values
 *  3. Sets player invisible flag before render and restores after
 *     (we don't use setInvisible permanently to avoid server-side visibility issues)
 *
 * Staying in the vanilla render pipeline means skin, cape, cosmetics, and
 * F5 third-person all work out of the box.
 */
@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    /**
     * Pre-render: translate to physics position.
     * Called once before the whole render, MatrixStack is in world-space relative to camera.
     */
    @Inject(
        method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void ragdoll_preRender(
            AbstractClientPlayerEntity player, float yaw, float tickDelta,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            CallbackInfo ci) {

        if (!ClientRagdollHandler.isActive()) return;
        if (player != net.minecraft.client.MinecraftClient.getInstance().player) return;

        // Offset model to physics torso position
        matrices.translate(
            ClientRagdollHandler.renderOffsetX,
            ClientRagdollHandler.renderOffsetY,
            ClientRagdollHandler.renderOffsetZ
        );
    }

    /**
     * Post-render: nothing to restore (translate is popped by caller).
     * The actual pose override happens in setAngles — see below.
     */

    /**
     * Override limb angles after vanilla setAngles() runs.
     *
     * We inject at TAIL of setAngles so we completely replace the vanilla
     * walk/idle animation with our physics pose.
     *
     * Note: this targets the method on BipedEntityModel (parent of PlayerEntityModel)
     * which both slim and default player models share.
     */
    @Inject(
        method = "setModelPose(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)V",
        at = @At("TAIL")
    )
    private void ragdoll_overridePose(AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (!ClientRagdollHandler.isActive()) return;
        if (player != net.minecraft.client.MinecraftClient.getInstance().player) return;

        PlayerEntityModel<?> model = ((PlayerEntityRenderer)(Object)this).getModel();

        // ── Body ──────────────────────────────────────────────────────────────
        model.body.pitch = ClientRagdollHandler.bodyPitch;
        model.body.roll  = ClientRagdollHandler.bodyRoll;

        // ── Head ──────────────────────────────────────────────────────────────
        model.head.pitch = ClientRagdollHandler.headPitch;
        model.head.yaw   = ClientRagdollHandler.headYaw;
        // hat tracks head
        model.hat.pitch  = model.head.pitch;
        model.hat.yaw    = model.head.yaw;

        // ── Left arm ──────────────────────────────────────────────────────────
        model.leftArm.pitch = ClientRagdollHandler.lArmPitch;
        model.leftArm.roll  = ClientRagdollHandler.lArmRoll;

        // ── Right arm ─────────────────────────────────────────────────────────
        model.rightArm.pitch = ClientRagdollHandler.rArmPitch;
        model.rightArm.roll  = ClientRagdollHandler.rArmRoll;

        // ── Legs ──────────────────────────────────────────────────────────────
        model.leftLeg.pitch  = ClientRagdollHandler.lLegPitch;
        model.rightLeg.pitch = ClientRagdollHandler.rLegPitch;

        // Kill vanilla swing / bob
        model.leftArm.yaw  = 0;
        model.rightArm.yaw = 0;
        model.leftLeg.yaw  = 0;
        model.rightLeg.yaw = 0;
        model.leftLeg.roll  = 0;
        model.rightLeg.roll = 0;
    }
}
