package com.ragdollmod;

import com.mojang.logging.LogUtils;
import com.ragdollmod.common.capability.RagdollCapabilityAttacher;
import com.ragdollmod.event.RagdollCommonEvents;
import com.ragdollmod.network.RagdollNetwork;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(PlayerRagdollMod.MOD_ID)
public class PlayerRagdollMod {

    public static final String MOD_ID = "playerragdoll";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlayerRagdollMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        // Network payload registration is on the mod bus (done client-side for client mod,
        // but we also need server-side registration):
        modEventBus.addListener(RagdollNetwork::onRegisterPayloads);

        // Register NeoForge event bus listeners
        NeoForge.EVENT_BUS.register(RagdollCommonEvents.class);
        NeoForge.EVENT_BUS.register(RagdollCapabilityAttacher.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("[PlayerRagdoll] Common setup complete.");
        });
    }
}
