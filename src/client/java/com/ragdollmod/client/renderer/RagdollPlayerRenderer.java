package com.ragdollmod.client.renderer;

import com.ragdollmod.client.ClientRagdollHandler;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

/**
 * Utility called from the render mixin to apply ragdoll pose angles to the player model.
 */
public class RagdollPlayerRenderer {

    public static void applyPose(PlayerEntityModel<?> model) {
        float bx = ClientRagdollHandler.smoothBodyX;
        float bz = ClientRagdollHandler.smoothBodyZ;
        float hx = ClientRagdollHandler.smoothHeadX;
        float lax = ClientRagdollHandler.smoothLeftArmX;
        float rax = ClientRagdollHandler.smoothRightArmX;
        float llx = ClientRagdollHandler.smoothLeftLegX;
        float rlx = ClientRagdollHandler.smoothRightLegX;

        // Body
        model.body.pitch = bx;
        model.body.roll  = bz;
        model.jacket.pitch = bx;
        model.jacket.roll  = bz;

        // Head
        model.head.pitch = hx;
        model.head.roll  = bz * 0.5f;
        model.hat.pitch  = hx;
        model.hat.roll   = bz * 0.5f;

        // Arms
        model.leftArm.pitch  = lax;
        model.leftArm.roll   = 0.4f + bz * 0.3f;
        model.leftSleeve.pitch = lax;
        model.leftSleeve.roll  = 0.4f + bz * 0.3f;

        model.rightArm.pitch  = rax;
        model.rightArm.roll   = -0.4f + bz * 0.3f;
        model.rightSleeve.pitch = rax;
        model.rightSleeve.roll  = -0.4f + bz * 0.3f;

        // Legs
        model.leftLeg.pitch  = llx;
        model.leftLeg.roll   = 0.2f;
        model.leftPants.pitch = llx;
        model.leftPants.roll  = 0.2f;

        model.rightLeg.pitch  = rlx;
        model.rightLeg.roll   = -0.2f;
        model.rightPants.pitch = rlx;
        model.rightPants.roll  = -0.2f;
    }

    public static void resetPose(PlayerEntityModel<?> model) {
        model.head.pitch=0; model.head.roll=0;
        model.hat.pitch=0;  model.hat.roll=0;
        model.body.pitch=0; model.body.roll=0;
        model.jacket.pitch=0; model.jacket.roll=0;
        model.leftArm.pitch=0; model.leftArm.roll=0;
        model.rightArm.pitch=0; model.rightArm.roll=0;
        model.leftSleeve.pitch=0; model.leftSleeve.roll=0;
        model.rightSleeve.pitch=0; model.rightSleeve.roll=0;
        model.leftLeg.pitch=0; model.leftLeg.roll=0;
        model.rightLeg.pitch=0; model.rightLeg.roll=0;
        model.leftPants.pitch=0; model.leftPants.roll=0;
        model.rightPants.pitch=0; model.rightPants.roll=0;
    }
}
