package com.ragdollmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ragdollmod.client.ClientRagdollHandler;
import com.ragdollmod.common.physics.RagdollPhysics;
import com.ragdollmod.common.physics.RagdollSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Hooks into RenderPlayerEvent to replace the standard player render with
 * a ragdoll pose when the physics simulation is active.
 *
 * Each segment is drawn at its simulated position by temporarily pushing
 * a transform onto the PoseStack.
 */
@OnlyIn(Dist.CLIENT)
public class RagdollPlayerRenderer {

    @SubscribeEvent
    public static void onPreRender(RenderPlayerEvent.Pre event) {
        if (!ClientRagdollHandler.isActive()) return;
        RagdollPhysics phys = ClientRagdollHandler.clientPhysics;
        if (phys == null) return;

        // Cancel vanilla render — we draw everything ourselves
        event.setCanceled(true);

        AbstractClientPlayer player = event.getEntity();
        PoseStack poseStack          = event.getPoseStack();
        MultiBufferSource buffer     = event.getMultiBufferSource();
        int packedLight              = event.getPackedLight();
        float partialTick            = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        PlayerRenderer renderer = event.getRenderer();
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        // Draw each segment independently using the segment's simulated position
        drawSegment(phys, RagdollPhysics.HEAD,    player, renderer, model,
            poseStack, buffer, packedLight, camera, partialTick);
        drawSegment(phys, RagdollPhysics.TORSO,   player, renderer, model,
            poseStack, buffer, packedLight, camera, partialTick);
        drawSegment(phys, RagdollPhysics.L_ARM,   player, renderer, model,
            poseStack, buffer, packedLight, camera, partialTick);
        drawSegment(phys, RagdollPhysics.R_ARM,   player, renderer, model,
            poseStack, buffer, packedLight, camera, partialTick);
        drawSegment(phys, RagdollPhysics.L_FORE,  player, renderer, model,
            poseStack, buffer, packedLight, camera, partialTick);
        drawSegment(phys, RagdollPhysics.R_FORE,  player, renderer, model,
            poseStack, buffer, packedLight, camera, partialTick);
        drawSegment(phys, RagdollPhysics.L_THIGH, player, renderer, model,
            poseStack, buffer, packedLight, camera, partialTick);
        drawSegment(phys, RagdollPhysics.R_THIGH, player, renderer, model,
            poseStack, buffer, packedLight, camera, partialTick);
        drawSegment(phys, RagdollPhysics.L_SHIN,  player, renderer, model,
            poseStack, buffer, packedLight, camera, partialTick);
        drawSegment(phys, RagdollPhysics.R_SHIN,  player, renderer, model,
            poseStack, buffer, packedLight, camera, partialTick);
    }

    /**
     * Draws one segment by temporarily showing only the relevant model part.
     *
     * Because vanilla PlayerModel doesn't have a clean per-part render API,
     * we hide all parts, show just the one we want, render, then restore.
     */
    private static void drawSegment(
        RagdollPhysics phys,
        int segIdx,
        AbstractClientPlayer player,
        PlayerRenderer renderer,
        PlayerModel<AbstractClientPlayer> model,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        Vec3 camera,
        float partialTick
    ) {
        RagdollSegment seg = phys.segments[segIdx];

        // World-space position relative to camera
        double relX = seg.x - camera.x;
        double relY = seg.y - camera.y;
        double relZ = seg.z - camera.z;

        // Compute tilt from velocity (makes the body tumble)
        Vec3 vel = seg.velocity();
        float tiltZ = (float)(vel.x * 4.0);
        float tiltX = (float)(vel.z * 4.0);

        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);
        poseStack.mulPose(Axis.ZP.rotation(tiltZ));
        poseStack.mulPose(Axis.XP.rotation(tiltX));
        poseStack.scale(-1f, -1f, 1f); // Minecraft flips player scale

        // Hide all parts, show only the desired one
        setAllPartsVisible(model, false);
        showPart(model, segIdx);

        renderer.render(player, 0f, partialTick, poseStack, buffer, packedLight);

        setAllPartsVisible(model, true);
        poseStack.popPose();
    }

    private static void setAllPartsVisible(PlayerModel<?> model, boolean visible) {
        model.head.visible          = visible;
        model.hat.visible           = visible;
        model.body.visible          = visible;
        model.leftArm.visible       = visible;
        model.rightArm.visible      = visible;
        model.leftLeg.visible       = visible;
        model.rightLeg.visible      = visible;
        // slim model forearms
        model.leftSleeve.visible    = visible;
        model.rightSleeve.visible   = visible;
        model.leftPants.visible     = visible;
        model.rightPants.visible    = visible;
        model.jacket.visible        = visible;
    }

    private static void showPart(PlayerModel<?> model, int segIdx) {
        switch (segIdx) {
            case RagdollPhysics.HEAD    -> { model.head.visible = true;  model.hat.visible = true; }
            case RagdollPhysics.TORSO   -> { model.body.visible = true;  model.jacket.visible = true; }
            case RagdollPhysics.L_ARM   -> { model.leftArm.visible  = true; model.leftSleeve.visible  = true; }
            case RagdollPhysics.R_ARM   -> { model.rightArm.visible = true; model.rightSleeve.visible = true; }
            case RagdollPhysics.L_FORE  -> model.leftArm.visible   = true; // reuse arm mesh for forearm
            case RagdollPhysics.R_FORE  -> model.rightArm.visible  = true;
            case RagdollPhysics.L_THIGH -> { model.leftLeg.visible  = true; model.leftPants.visible  = true; }
            case RagdollPhysics.R_THIGH -> { model.rightLeg.visible = true; model.rightPants.visible = true; }
            case RagdollPhysics.L_SHIN  -> model.leftLeg.visible   = true;
            case RagdollPhysics.R_SHIN  -> model.rightLeg.visible  = true;
        }
    }
}
