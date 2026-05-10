package com.ragdollmod.network;

import com.ragdollmod.PlayerRagdollMod;
import com.ragdollmod.common.capability.RagdollCapability;
import com.ragdollmod.common.capability.RagdollCapabilityAttacher;
import com.ragdollmod.common.physics.RagdollPhysics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RagdollNetwork {

    public static void register() {
        // Registration happens through the event below
    }

    // ── Toggle Ragdoll Packet (C → S) ─────────────────────────────

    public record ToggleRagdollPacket() implements CustomPacketPayload {
        public static final Type<ToggleRagdollPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlayerRagdollMod.MOD_ID, "toggle_ragdoll"));

        public static final StreamCodec<FriendlyByteBuf, ToggleRagdollPacket> CODEC =
            StreamCodec.unit(new ToggleRagdollPacket());

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── Input State Packet (C → S, every tick) ────────────────────

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
                buf -> new RagdollInputPacket(
                    buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readFloat()
                )
            );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── Sync State Packet (S → C) — optional HUD / rendering ──────

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

        reg.playToServer(ToggleRagdollPacket.TYPE, ToggleRagdollPacket.CODEC,
            RagdollNetwork::handleToggle);

        reg.playToServer(RagdollInputPacket.TYPE, RagdollInputPacket.CODEC,
            RagdollNetwork::handleInput);

        reg.playToClient(RagdollSyncPacket.TYPE, RagdollSyncPacket.CODEC,
            RagdollNetwork::handleSync);
    }

    // ── Server-side handlers ───────────────────────────────────────

    private static void handleToggle(ToggleRagdollPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            RagdollCapability cap = RagdollCapabilityAttacher.get(sp);

            if (cap.isActive()) {
                cap.disable();
                // Restore normal movement
                sp.setNoGravity(false);
                PlayerRagdollMod.LOGGER.debug("[Ragdoll] Disabled for {}", sp.getName().getString());
            } else {
                cap.enable(sp.getX(), sp.getY(), sp.getZ());
                sp.setNoGravity(true); // physics simulation handles gravity now
                PlayerRagdollMod.LOGGER.debug("[Ragdoll] Enabled for {}", sp.getName().getString());
            }

            // Inform client
            PacketDistributor.sendToPlayer(sp, new RagdollSyncPacket(cap.isActive()));
        });
    }

    private static void handleInput(RagdollInputPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            RagdollCapability cap = RagdollCapabilityAttacher.get(sp);
            if (!cap.isActive()) return;
            cap.moveX  = pkt.moveX();
            cap.moveZ  = pkt.moveZ();
            cap.jumping = pkt.jumping();
            cap.yaw    = pkt.yaw();
        });
    }

    private static void handleSync(RagdollSyncPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Client-side: update local cache (used by ClientRagdollHandler)
            com.ragdollmod.client.ClientRagdollHandler.setLocalRagdollActive(pkt.active());
        });
    }

    // ── Helper: send toggle from client ───────────────────────────
    public static void sendToggle() {
        PacketDistributor.sendToServer(new ToggleRagdollPacket());
    }

    public static void sendInput(double mx, double mz, boolean jump, float yaw) {
        PacketDistributor.sendToServer(new RagdollInputPacket(mx, mz, jump, yaw));
    }
}
