package com.ragdollmod;

import com.ragdollmod.common.capability.RagdollCapability;
import com.ragdollmod.common.capability.RagdollCapabilityAttacher;
import com.ragdollmod.common.physics.RagdollPhysics;
import com.ragdollmod.network.RagdollInputPayload;
import com.ragdollmod.network.RagdollSyncPayload;
import com.ragdollmod.network.ToggleRagdollPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerRagdollMod implements ModInitializer {

    public static final String MOD_ID = "playerragdoll";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register payload types for both directions
        PayloadTypeRegistry.playC2S().register(ToggleRagdollPayload.ID, ToggleRagdollPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RagdollInputPayload.ID, RagdollInputPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RagdollSyncPayload.ID, RagdollSyncPayload.CODEC);

        // Handle toggle (client → server)
        ServerPlayNetworking.registerGlobalReceiver(ToggleRagdollPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity sp = ctx.player();
            RagdollCapability cap = RagdollCapabilityAttacher.get(sp);
            if (cap.isActive()) {
                cap.disable();
                LOGGER.info("[Ragdoll] Disabled for {}", sp.getName().getString());
            } else {
                cap.enable(sp.getX(), sp.getY(), sp.getZ());
                LOGGER.info("[Ragdoll] Enabled for {}", sp.getName().getString());
            }
            ServerPlayNetworking.send(sp, new RagdollSyncPayload(cap.isActive()));
        });

        // Handle input (client → server)
        ServerPlayNetworking.registerGlobalReceiver(RagdollInputPayload.ID, (payload, ctx) -> {
            RagdollCapability cap = RagdollCapabilityAttacher.get(ctx.player());
            if (!cap.isActive()) return;
            cap.moveX   = payload.moveX();
            cap.moveZ   = payload.moveZ();
            cap.jumping = payload.jumping();
            cap.yaw     = payload.yaw();
        });

        // Server tick: advance physics for every player
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                RagdollCapability cap = RagdollCapabilityAttacher.get(sp);
                if (!cap.isActive()) continue;
                RagdollPhysics phys = cap.getPhysics();
                if (phys == null) continue;
                phys.applyLocomotionImpulse(cap.moveX, cap.moveZ, cap.jumping, cap.yaw);
                phys.tick(sp.getServerWorld());
                Vec3d root = phys.rootPosition();
                sp.requestTeleport(root.x, phys.feetY(), root.z);
                sp.setVelocity(Vec3d.ZERO);
            }
        });

        LOGGER.info("[PlayerRagdoll] Initialized.");
    }
}
