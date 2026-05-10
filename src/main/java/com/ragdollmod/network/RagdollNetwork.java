package com.ragdollmod.network;

import com.ragdollmod.PlayerRagdollMod;
import com.ragdollmod.physics.RagdollState;
import com.ragdollmod.physics.RagdollStateManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class RagdollNetwork {

    // ── Payload: client → server: toggle ragdoll ─────────────────────────────

    public record TogglePayload() implements CustomPayload {
        public static final Id<TogglePayload> ID =
                new Id<>(Identifier.of(PlayerRagdollMod.MOD_ID, "toggle_ragdoll"));
        public static final PacketCodec<PacketByteBuf, TogglePayload> CODEC =
                PacketCodec.unit(new TogglePayload());

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ── Payload: client → server: WASD input while ragdolling ────────────────

    public record InputPayload(double moveX, double moveZ, float yaw)
            implements CustomPayload {
        public static final Id<InputPayload> ID =
                new Id<>(Identifier.of(PlayerRagdollMod.MOD_ID, "ragdoll_input"));
        public static final PacketCodec<PacketByteBuf, InputPayload> CODEC =
                PacketCodec.of(
                    (buf, pkt) -> {
                        buf.writeDouble(pkt.moveX());
                        buf.writeDouble(pkt.moveZ());
                        buf.writeFloat(pkt.yaw());
                    },
                    buf -> new InputPayload(buf.readDouble(), buf.readDouble(), buf.readFloat())
                );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ── Payload: server → client: sync active state ───────────────────────────

    public record SyncPayload(boolean active) implements CustomPayload {
        public static final Id<SyncPayload> ID =
                new Id<>(Identifier.of(PlayerRagdollMod.MOD_ID, "ragdoll_sync"));
        public static final PacketCodec<PacketByteBuf, SyncPayload> CODEC =
                PacketCodec.of(
                    (buf, pkt) -> buf.writeBoolean(pkt.active()),
                    buf -> new SyncPayload(buf.readBoolean())
                );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /** Call from common ModInitializer — registers payload types for both sides. */
    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(TogglePayload.ID, TogglePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(InputPayload.ID,  InputPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID,   SyncPayload.CODEC);
    }

    /** Call from common ModInitializer — registers server-side receive handlers. */
    public static void registerServerHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(TogglePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                RagdollState state = RagdollStateManager.get(player);
                if (state.isActive()) {
                    state.disable();
                    PlayerRagdollMod.LOGGER.debug("[Ragdoll] OFF for {}", player.getName().getString());
                } else {
                    state.enable(player.getX(), player.getY(), player.getZ());
                    PlayerRagdollMod.LOGGER.debug("[Ragdoll] ON for {}", player.getName().getString());
                }
                // Sync state back to the triggering client
                ServerPlayNetworking.send(player, new SyncPayload(state.isActive()));
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(InputPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                RagdollState state = RagdollStateManager.get(player);
                if (!state.isActive()) return;
                state.moveX = payload.moveX();
                state.moveZ = payload.moveZ();
                state.yaw   = payload.yaw();
            });
        });
    }
}
