package com.ragdollmod.network;

import com.ragdollmod.PlayerRagdollMod;
import com.ragdollmod.common.capability.RagdollCapability;
import com.ragdollmod.common.capability.RagdollCapabilityAttacher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RagdollNetwork {

    // ── Toggle Ragdoll Packet (C → S) ─────────────────────────────
    public record ToggleRagdollPacket() implements CustomPacketPayload {
        public static final Type<ToggleRagdollPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlayerRagdollMod.MOD_ID, "toggle_ragdoll"));
        public static final StreamCodec<FriendlyByteBuf, ToggleRagdollPacket> CODEC =
            StreamCodec.unit(new ToggleRagdollPacket());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── Input State Packet (C → S) ────────────────────────────────
    public record RagdollInputPacket(double moveX, double moveZ, boolean jumping, float yaw)
        implements CustomPacketPayload {
        public static final Type<RagdollInputPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlayerRagdollMod.MOD_ID, "ragdoll_input"));
        public static final StreamCodec<FriendlyByteBuf, RagdollInputPacket> CODEC =
            StreamCodec.of(
                (buf, pkt) -> {
                    buf.writeDouble(pkt.moveX());
                    buf.writeDouble(pkt.moveZ());
                    buf.writeBoolean(pkt.jumping());
                    buf.writeFloat(pkt.yaw());
                },
                buf -> new RagdollInputPacket(buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readFloat())
            );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── Sync State Packet (S → C) ─────────────────────────────────
    public record RagdollSyncPacket(boolean active) implements CustomPacketPayload {
        public static final Type<RagdollSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlayerRagdollMod.MOD_ID, "ragdoll_sync"));
        public static final StreamCodec<FriendlyByteBuf, RagdollSyncPacket> CODEC =
            StreamCodec.of(
                (buf, pkt) -> buf.writeBoolean(pkt.active()),
                buf -> new RagdollSyncPacket(buf.readBoolean())
            );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── Event registration (called from mod bus) ───────────────────
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");
        reg.playToServer(ToggleRagdollPacket.TYPE, ToggleRagdollPacket.CODEC, RagdollNetwork::handleToggle);
        reg.playToServer(RagdollInputPacket.TYPE,  RagdollInputPacket.CODEC,  RagdollNetwork::handleInput);
        reg.playToClient(RagdollSyncPacket.TYPE,   RagdollSyncPacket.CODEC,   RagdollNetwork::handleSync);
    }

    // ── Server-side handlers ───────────────────────────────────────
    private static void handleToggle(ToggleRagdollPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            RagdollCapability cap = RagdollCapabilityAttacher.get(sp);

            if (cap.isActive()) {
                cap.disable();
                sp.setNoGravity(false);
                PlayerRagdollMod.LOGGER.info("[Ragdoll] Disabled for {}", sp.getName().getString());
            } else {
                cap.enable(sp.getX(), sp.getY(), sp.getZ());
                sp.setNoGravity(true);
                PlayerRagdollMod.LOGGER.info("[Ragdoll] Enabled for {}", sp.getName().getString());
            }

            // Send state back to client
            boolean nowActive = cap.isActive();
            PlayerRagdollMod.LOGGER.info("[Ragdoll] Sending sync to client: active={}", nowActive);
            PacketDistributor.sendToPlayer(sp, new RagdollSyncPacket(nowActive));
        });
    }

    private static void handleInput(RagdollInputPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            RagdollCapability cap = RagdollCapabilityAttacher.get(sp);
            if (!cap.isActive()) return;
            cap.moveX   = pkt.moveX();
            cap.moveZ   = pkt.moveZ();
            cap.jumping = pkt.jumping();
            cap.yaw     = pkt.yaw();
        });
    }

    // ── Client-side handler ────────────────────────────────────────
    private static void handleSync(RagdollSyncPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            PlayerRagdollMod.LOGGER.info("[Ragdoll] Client received sync: active={}", pkt.active());
            com.ragdollmod.client.ClientRagdollHandler.setLocalRagdollActive(pkt.active());
        });
    }

    // ── Client helpers ─────────────────────────────────────────────
    public static void sendToggle() {
        PacketDistributor.sendToServer(new ToggleRagdollPacket());
    }

    public static void sendInput(double mx, double mz, boolean jump, float yaw) {
        PacketDistributor.sendToServer(new RagdollInputPacket(mx, mz, jump, yaw));
    }
}
