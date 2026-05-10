package com.ragdollmod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RagdollInputPayload(double moveX, double moveZ, boolean jumping, float yaw) implements CustomPayload {
    public static final Id<RagdollInputPayload> ID =
        new Id<>(Identifier.of("playerragdoll", "ragdoll_input"));
    public static final PacketCodec<PacketByteBuf, RagdollInputPayload> CODEC = PacketCodec.of(
        (buf, pkt) -> { buf.writeDouble(pkt.moveX()); buf.writeDouble(pkt.moveZ()); buf.writeBoolean(pkt.jumping()); buf.writeFloat(pkt.yaw()); },
        buf -> new RagdollInputPayload(buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readFloat())
    );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
