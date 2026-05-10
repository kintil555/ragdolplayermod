package com.ragdollmod;

import com.ragdollmod.network.RagdollNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerRagdollMod implements ModInitializer {

    public static final String MOD_ID = "playerragdoll";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[PlayerRagdoll] Initializing server side...");

        // Register network payloads (both directions)
        RagdollNetwork.registerPayloads();

        // Register server-side packet handlers
        RagdollNetwork.registerServerHandlers();

        LOGGER.info("[PlayerRagdoll] Server init complete.");
    }
}
