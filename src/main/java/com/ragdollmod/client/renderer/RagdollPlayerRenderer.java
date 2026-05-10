package com.ragdollmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ragdollmod.client.ClientRagdollHandler;
import com.ragdollmod.common.physics.RagdollPhysics;
import com.ragdollmod.common.physics.RagdollSegment;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@OnlyIn(Dist.CLIENT)
public class RagdollPlayerRenderer {

    /**
     * Before vanilla renders: manipulate model part rotations to match ragdoll physics.
     * We do NOT cancel the event — we let vanilla render but with our rotations applied.
     * This avoids F5 crashes from PoseStack corruption.
     */
    @SubscribeEvent
    public static void onPreRender(RenderPlayerEvent.Pre event) {
        if (!ClientRagdollHandler.isActive()) return;
        RagdollPhysics phys = ClientRagdollHandler.clientPhysics;
        if (phys == null) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer)) return;

        @SuppressWarnings("unchecked")
        PlayerModel<AbstractClientPlayer> model =
            (PlayerModel<AbstractClientPlayer>) event.getRenderer().getModel();

        applyRagdollPose(model, phys);
    }

    /**
     * After vanilla renders: reset model rotations back to zero
     * so normal players are not affected.
     */
    @SubscribeEvent
    public static void onPostRender(RenderPlayerEvent.Post event) {
        if (!ClientRagdollHandler.isActive()) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer)) return;

        @SuppressWarnings("unchecked")
        PlayerModel<AbstractClientPlayer> model =
            (PlayerModel<AbstractClientPlayer>) event.getRenderer().getModel();

        resetPose(model);
    }

    private static void applyRagdollPose(PlayerModel<?> model, RagdollPhysics phys) {
        RagdollSegment torso  = phys.segments[RagdollPhysics.TORSO];
        RagdollSegment head   = phys.segments[RagdollPhysics.HEAD];
        RagdollSegment lArm   = phys.segments[RagdollPhysics.L_ARM];
        RagdollSegment rArm   = phys.segments[RagdollPhysics.R_ARM];
        RagdollSegment lThigh = phys.segments[RagdollPhysics.L_THIGH];
        RagdollSegment rThigh = phys.segments[RagdollPhysics.R_THIGH];
        RagdollSegment lShin  = phys.segments[RagdollPhysics.L_SHIN];
        RagdollSegment rShin  = phys.segments[RagdollPhysics.R_SHIN];

        // Torso tilt from velocity
        Vec3 torsoVel = torso.velocity();
        float bodyZ = clamp((float)(torsoVel.x * 8.0), -1.2f, 1.2f);
        float bodyX = clamp((float)(torsoVel.z * 8.0), -1.2f, 1.2f);
        float bodyY = clamp((float)(torsoVel.x * 4.0), -0.8f, 0.8f);

        model.body.xRot = bodyX;
        model.body.zRot = bodyZ;
        model.body.yRot = bodyY;

        // Head relative to torso
        Vec3 headVel = head.velocity();
        float headDX = (float)(head.x - torso.x);
        float headDY = (float)(head.y - torso.y - 0.4);
        model.head.xRot = clamp((float)(headVel.z * 6.0) + bodyX, -1.5f, 1.5f);
        model.head.zRot = clamp(headDX * 2.0f + bodyZ, -1.2f, 1.2f);
        model.head.yRot = 0;

        // Arms — swing based on segment offset from torso
        float lArmDY = (float)(lArm.y - torso.y);
        float rArmDY = (float)(rArm.y - torso.y);
        float lArmDX = (float)(lArm.x - torso.x);
        float rArmDX = (float)(rArm.x - torso.x);

        model.leftArm.xRot  = clamp(lArmDY * 3.0f, -2.0f, 2.0f);
        model.leftArm.zRot  = clamp(lArmDX * 4.0f - 0.5f, -2.0f, 0.5f);
        model.rightArm.xRot = clamp(rArmDY * 3.0f, -2.0f, 2.0f);
        model.rightArm.zRot = clamp(rArmDX * 4.0f + 0.5f, -0.5f, 2.0f);

        // Legs
        float lThighDY = (float)(lThigh.y - torso.y + 0.5);
        float rThighDY = (float)(rThigh.y - torso.y + 0.5);
        float lShinDY  = (float)(lShin.y  - lThigh.y + 0.4);
        float rShinDY  = (float)(rShin.y  - rThigh.y + 0.4);

        model.leftLeg.xRot   = clamp(lThighDY * 4.0f, -2.0f, 2.0f);
        model.leftLeg.zRot   = clamp((float)(lThigh.x - torso.x) * 3.0f, -0.8f, 0.8f);
        model.rightLeg.xRot  = clamp(rThighDY * 4.0f, -2.0f, 2.0f);
        model.rightLeg.zRot  = clamp((float)(rThigh.x - torso.x) * 3.0f, -0.8f, 0.8f);

        // Sleeves/pants mirror their parent
        model.leftSleeve.xRot  = model.leftArm.xRot;
        model.leftSleeve.zRot  = model.leftArm.zRot;
        model.rightSleeve.xRot = model.rightArm.xRot;
        model.rightSleeve.zRot = model.rightArm.zRot;
        model.leftPants.xRot   = model.leftLeg.xRot;
        model.leftPants.zRot   = model.leftLeg.zRot;
        model.rightPants.xRot  = model.rightLeg.xRot;
        model.rightPants.zRot  = model.rightLeg.zRot;
        model.jacket.xRot      = model.body.xRot;
        model.jacket.zRot      = model.body.zRot;
        model.hat.xRot         = model.head.xRot;
        model.hat.zRot         = model.head.zRot;
    }

    private static void resetPose(PlayerModel<?> model) {
        model.head.xRot = 0; model.head.yRot = 0; model.head.zRot = 0;
        model.hat.xRot  = 0; model.hat.yRot  = 0; model.hat.zRot  = 0;
        model.body.xRot = 0; model.body.yRot = 0; model.body.zRot = 0;
        model.jacket.xRot = 0; model.jacket.zRot = 0;
        model.leftArm.xRot  = 0; model.leftArm.zRot  = 0;
        model.rightArm.xRot = 0; model.rightArm.zRot = 0;
        model.leftSleeve.xRot  = 0; model.leftSleeve.zRot  = 0;
        model.rightSleeve.xRot = 0; model.rightSleeve.zRot = 0;
        model.leftLeg.xRot  = 0; model.leftLeg.zRot  = 0;
        model.rightLeg.xRot = 0; model.rightLeg.zRot = 0;
        model.leftPants.xRot  = 0; model.leftPants.zRot  = 0;
        model.rightPants.xRot = 0; model.rightPants.zRot = 0;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
