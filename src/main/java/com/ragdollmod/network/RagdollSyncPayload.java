package com.ragdollmod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RagdollSyncPayload(boolean active) implements CustomPayload {
    public static final Id<RagdollSyncPayload> ID =
        new Id<>(Identifier.of("playerragdoll", "ragdoll_sync"));
    public static final PacketCodec<PacketByteBuf, RagdollSyncPayload> CODEC = PacketCodec.of(
        (buf, pkt) -> buf.writeBoolean(pkt.active()),
        buf -> new RagdollSyncPayload(buf.readBoolean())
    );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
