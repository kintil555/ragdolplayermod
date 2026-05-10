package com.ragdollmod.common.capability;

import com.ragdollmod.PlayerRagdollMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NeoForge 1.21 does not use the old AttachCapabilitiesEvent system.
 * Instead, we keep a simple server-side map of player UUID → RagdollCapability.
 *
 * This is intentionally lightweight — no reflection tricks, no capability providers.
 */
public class RagdollCapabilityAttacher {

    // Global store: works server-side; client mirrors via network packets
    private static final Map<UUID, RagdollCapability> STORE = new HashMap<>();

    public static RagdollCapability get(Player player) {
        return STORE.computeIfAbsent(player.getUUID(), id -> new RagdollCapability());
    }

    public static boolean has(Player player) {
        return STORE.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Ensure entry exists
            STORE.computeIfAbsent(player.getUUID(), id -> new RagdollCapability());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        // Copy capability across death (keep toggle state)
        RagdollCapability oldCap = STORE.get(event.getOriginal().getUUID());
        if (oldCap != null) {
            RagdollCapability newCap = get(event.getEntity());
            // Do NOT re-enable physics here; let the player respawn normally
            // then they can re-toggle if desired.
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Clean up to avoid memory leak on server
        STORE.remove(event.getEntity().getUUID());
    }
}
