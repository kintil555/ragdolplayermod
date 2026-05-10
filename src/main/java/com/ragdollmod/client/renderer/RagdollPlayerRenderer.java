package com.ragdollmod.client.renderer;

import com.ragdollmod.client.ClientRagdollHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@OnlyIn(Dist.CLIENT)
public class RagdollPlayerRenderer {

    @SubscribeEvent
    public static void onPreRender(RenderPlayerEvent.Pre event) {
        if (!ClientRagdollHandler.isActive()) return;
        if (ClientRagdollHandler.clientPhysics == null) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer)) return;

        @SuppressWarnings("unchecked")
        PlayerModel<AbstractClientPlayer> model =
            (PlayerModel<AbstractClientPlayer>) event.getRenderer().getModel();

        applyPose(model);
    }

    @SubscribeEvent
    public static void onPostRender(RenderPlayerEvent.Post event) {
        if (!ClientRagdollHandler.isActive()) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer)) return;

        @SuppressWarnings("unchecked")
        PlayerModel<AbstractClientPlayer> model =
            (PlayerModel<AbstractClientPlayer>) event.getRenderer().getModel();

        resetPose(model);
    }

    private static void applyPose(PlayerModel<?> model) {
        float bx = ClientRagdollHandler.smoothBodyX;
        float bz = ClientRagdollHandler.smoothBodyZ;
        float hx = ClientRagdollHandler.smoothHeadX;
        float lax = ClientRagdollHandler.smoothLeftArmX;
        float rax = ClientRagdollHandler.smoothRightArmX;
        float llx = ClientRagdollHandler.smoothLeftLegX;
        float rlx = ClientRagdollHandler.smoothRightLegX;

        // Body
        model.body.xRot = bx;
        model.body.zRot = bz;
        model.jacket.xRot = bx;
        model.jacket.zRot = bz;

        // Head — relative to body
        model.head.xRot = hx;
        model.head.zRot = bz * 0.5f;
        model.hat.xRot  = hx;
        model.hat.zRot  = bz * 0.5f;

        // Arms — flop outward + rotate from physics
        model.leftArm.xRot   = lax;
        model.leftArm.zRot   = 0.4f + bz * 0.3f;   // flop slightly left
        model.leftSleeve.xRot = lax;
        model.leftSleeve.zRot = 0.4f + bz * 0.3f;

        model.rightArm.xRot   = rax;
        model.rightArm.zRot   = -0.4f + bz * 0.3f;  // flop slightly right
        model.rightSleeve.xRot = rax;
        model.rightSleeve.zRot = -0.4f + bz * 0.3f;

        // Legs — spread out
        model.leftLeg.xRot   = llx;
        model.leftLeg.zRot   = 0.2f;
        model.leftPants.xRot  = llx;
        model.leftPants.zRot  = 0.2f;

        model.rightLeg.xRot   = rlx;
        model.rightLeg.zRot   = -0.2f;
        model.rightPants.xRot  = rlx;
        model.rightPants.zRot  = -0.2f;
    }

    private static void resetPose(PlayerModel<?> model) {
        model.head.xRot = 0;      model.head.zRot = 0;
        model.hat.xRot  = 0;      model.hat.zRot  = 0;
        model.body.xRot = 0;      model.body.zRot = 0;
        model.jacket.xRot = 0;    model.jacket.zRot = 0;
        model.leftArm.xRot = 0;   model.leftArm.zRot = 0;
        model.rightArm.xRot = 0;  model.rightArm.zRot = 0;
        model.leftSleeve.xRot = 0; model.leftSleeve.zRot = 0;
        model.rightSleeve.xRot = 0; model.rightSleeve.zRot = 0;
        model.leftLeg.xRot = 0;   model.leftLeg.zRot = 0;
        model.rightLeg.xRot = 0;  model.rightLeg.zRot = 0;
        model.leftPants.xRot = 0;  model.leftPants.zRot = 0;
        model.rightPants.xRot = 0; model.rightPants.zRot = 0;
    }
}
