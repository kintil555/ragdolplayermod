package com.ragdollmod.physics;

import net.minecraft.entity.player.PlayerEntity;

import java.util.WeakHashMap;

/**
 * Global registry of per-player RagdollState.
 * Uses WeakHashMap so states are GC'd when players disconnect.
 */
public class RagdollStateManager {

    private static final WeakHashMap<PlayerEntity, RagdollState> MAP = new WeakHashMap<>();

    public static RagdollState get(PlayerEntity player) {
        return MAP.computeIfAbsent(player, k -> new RagdollState());
    }

    public static boolean isActive(PlayerEntity player) {
        RagdollState state = MAP.get(player);
        return state != null && state.isActive();
    }

    /** Called on player logout to clean up eagerly. */
    public static void remove(PlayerEntity player) {
        MAP.remove(player);
    }
}
