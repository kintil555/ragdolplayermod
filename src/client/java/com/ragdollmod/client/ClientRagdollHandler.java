package com.ragdollmod.client;

import com.ragdollmod.common.physics.RagdollPhysics;
import com.ragdollmod.common.physics.RagdollSegment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class ClientRagdollHandler {

    private static boolean localActive = false;
    public static RagdollPhysics clientPhysics = null;

    public static float smoothBodyX=0, smoothBodyZ=0;
    public static float smoothHeadX=0;
    public static float smoothLeftArmX=0, smoothRightArmX=0;
    public static float smoothLeftLegX=0, smoothRightLegX=0;

    public static void setLocalRagdollActive(boolean active) {
        localActive = active;
        clientPhysics = null;
        resetSmooth();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(
                Text.literal(active ? "§aRagdoll ON §7(R)" : "§cRagdoll OFF"), true);
        }
    }

    private static void resetSmooth() {
        smoothBodyX=smoothBodyZ=smoothHeadX=0;
        smoothLeftArmX=smoothRightArmX=smoothLeftLegX=smoothRightLegX=0;
    }

    public static boolean isActive() { return localActive; }

    /** Called each client tick when ragdoll is active. */
    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        if (clientPhysics == null) {
            clientPhysics = new RagdollPhysics(player.getX(), player.getY(), player.getZ());
            for (RagdollSegment seg : clientPhysics.segments) {
                seg.applyImpulse((Math.random()-0.5)*0.15, -0.5, (Math.random()-0.5)*0.15);
            }
        }

        float yaw = (float) Math.toRadians(player.getYaw());
        double moveX=0, moveZ=0;
        boolean fwd   = mc.options.forwardKey.isPressed();
        boolean back  = mc.options.backKey.isPressed();
        boolean left  = mc.options.leftKey.isPressed();
        boolean right = mc.options.rightKey.isPressed();
        if (fwd||back||left||right) {
            double sin=Math.sin(yaw), cos=Math.cos(yaw);
            if (fwd)  { moveX+=-sin; moveZ+=cos; }
            if (back) { moveX+=sin;  moveZ-=cos; }
            if (right){ moveX+=cos;  moveZ+=sin; }
            if (left) { moveX-=cos;  moveZ-=sin; }
            double len=Math.sqrt(moveX*moveX+moveZ*moveZ);
            if (len>1e-6){ moveX/=len; moveZ/=len; }
        }

        clientPhysics.applyLocomotionImpulse(moveX, moveZ, false, yaw);
        clientPhysics.tick(mc.world);

        RagdollSegment torso=clientPhysics.segments[RagdollPhysics.TORSO];
        RagdollSegment head =clientPhysics.segments[RagdollPhysics.HEAD];
        RagdollSegment lArm =clientPhysics.segments[RagdollPhysics.L_ARM];
        RagdollSegment rArm =clientPhysics.segments[RagdollPhysics.R_ARM];
        RagdollSegment lThigh=clientPhysics.segments[RagdollPhysics.L_THIGH];
        RagdollSegment rThigh=clientPhysics.segments[RagdollPhysics.R_THIGH];

        Vec3d tv=torso.velocity();
        float targetBodyX=clamp((float)(-tv.y*12.0+tv.z*6.0),-(float)Math.PI,(float)Math.PI);
        float targetBodyZ=clamp((float)(tv.x*8.0),-1.5f,1.5f);
        float dxHT=(float)(head.x-torso.x);
        float dyHT=(float)(head.y-torso.y-0.35);
        float targetHeadX=clamp(dyHT*4f+targetBodyX*0.3f,-1.8f,1.8f);
        float targetLArmX=clamp((float)(lArm.y-torso.y+0.2)*5f,-2.5f,2.5f);
        float targetRArmX=clamp((float)(rArm.y-torso.y+0.2)*5f,-2.5f,2.5f);
        float targetLLegX=clamp((float)(lThigh.y-torso.y+0.6)*6f,-2.5f,2.5f);
        float targetRLegX=clamp((float)(rThigh.y-torso.y+0.6)*6f,-2.5f,2.5f);

        float t=0.3f;
        smoothBodyX=lerp(smoothBodyX,targetBodyX,t);
        smoothBodyZ=lerp(smoothBodyZ,targetBodyZ,t);
        smoothHeadX=lerp(smoothHeadX,targetHeadX,t);
        smoothLeftArmX=lerp(smoothLeftArmX,targetLArmX,t);
        smoothRightArmX=lerp(smoothRightArmX,targetRArmX,t);
        smoothLeftLegX=lerp(smoothLeftLegX,targetLLegX,t);
        smoothRightLegX=lerp(smoothRightLegX,targetRLegX,t);

        player.setVelocity(Vec3d.ZERO);
    }

    private static float lerp(float a,float b,float t){ return a+(b-a)*t; }
    private static float clamp(float v,float min,float max){ return Math.max(min,Math.min(max,v)); }
}
