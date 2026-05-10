package com.ragdollmod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToggleRagdollPayload() implements CustomPayload {
    public static final Id<ToggleRagdollPayload> ID =
        new Id<>(Identifier.of("playerragdoll", "toggle_ragdoll"));
    public static final PacketCodec<PacketByteBuf, ToggleRagdollPayload> CODEC =
        PacketCodec.unit(new ToggleRagdollPayload());

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
