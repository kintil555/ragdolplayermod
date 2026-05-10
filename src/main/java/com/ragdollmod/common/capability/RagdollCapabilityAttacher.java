package com.ragdollmod.common.capability;

import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RagdollCapabilityAttacher {
    private static final Map<UUID, RagdollCapability> STORE = new HashMap<>();

    public static RagdollCapability get(PlayerEntity player) {
        return STORE.computeIfAbsent(player.getUuid(), id -> new RagdollCapability());
    }

    public static void remove(PlayerEntity player) {
        STORE.remove(player.getUuid());
    }
}
