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
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@OnlyIn(Dist.CLIENT)
public class RagdollPlayerRenderer {

    @SubscribeEvent
    public static void onPreRender(RenderPlayerEvent.Pre event) {
        if (!ClientRagdollHandler.isActive()) return;
        RagdollPhysics phys = ClientRagdollHandler.clientPhysics;
        if (phys == null) return;

        // Only handle AbstractClientPlayer instances
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;

        event.setCanceled(true);

        PoseStack poseStack   = event.getPoseStack();
        MultiBufferSource buf = event.getMultiBufferSource();
        int packedLight       = event.getPackedLight();
        float partialTick     = event.getPartialTick(); // float in NeoForge 1.21.1

        PlayerRenderer renderer = event.getRenderer();
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        for (int i = 0; i < phys.segments.length; i++) {
            drawSegment(phys, i, player, renderer, model, poseStack, buf, packedLight, camera, partialTick);
        }
    }

    private static void drawSegment(
        RagdollPhysics phys, int segIdx,
        AbstractClientPlayer player, PlayerRenderer renderer,
        PlayerModel<AbstractClientPlayer> model,
        PoseStack poseStack, MultiBufferSource buffer,
        int packedLight, Vec3 camera, float partialTick
    ) {
        RagdollSegment seg = phys.segments[segIdx];

        double relX = seg.x - camera.x;
        double relY = seg.y - camera.y;
        double relZ = seg.z - camera.z;

        Vec3 vel = seg.velocity();
        float tiltZ = (float)(vel.x * 4.0);
        float tiltX = (float)(vel.z * 4.0);

        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);
        poseStack.mulPose(Axis.ZP.rotation(tiltZ));
        poseStack.mulPose(Axis.XP.rotation(tiltX));
        poseStack.scale(-1f, -1f, 1f);

        setAllPartsVisible(model, false);
        showPart(model, segIdx);

        renderer.render(player, 0f, partialTick, poseStack, buffer, packedLight);

        setAllPartsVisible(model, true);
        poseStack.popPose();
    }

    private static void setAllPartsVisible(PlayerModel<?> model, boolean v) {
        model.head.visible        = v;
        model.hat.visible         = v;
        model.body.visible        = v;
        model.leftArm.visible     = v;
        model.rightArm.visible    = v;
        model.leftLeg.visible     = v;
        model.rightLeg.visible    = v;
        model.leftSleeve.visible  = v;
        model.rightSleeve.visible = v;
        model.leftPants.visible   = v;
        model.rightPants.visible  = v;
        model.jacket.visible      = v;
    }

    private static void showPart(PlayerModel<?> model, int segIdx) {
        switch (segIdx) {
            case RagdollPhysics.HEAD    -> { model.head.visible = true;      model.hat.visible = true; }
            case RagdollPhysics.TORSO   -> { model.body.visible = true;      model.jacket.visible = true; }
            case RagdollPhysics.L_ARM   -> { model.leftArm.visible = true;   model.leftSleeve.visible = true; }
            case RagdollPhysics.R_ARM   -> { model.rightArm.visible = true;  model.rightSleeve.visible = true; }
            case RagdollPhysics.L_FORE  ->   model.leftArm.visible = true;
            case RagdollPhysics.R_FORE  ->   model.rightArm.visible = true;
            case RagdollPhysics.L_THIGH -> { model.leftLeg.visible = true;   model.leftPants.visible = true; }
            case RagdollPhysics.R_THIGH -> { model.rightLeg.visible = true;  model.rightPants.visible = true; }
            case RagdollPhysics.L_SHIN  ->   model.leftLeg.visible = true;
            case RagdollPhysics.R_SHIN  ->   model.rightLeg.visible = true;
        }
    }
}
