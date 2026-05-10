package com.ragdollmod;

import com.ragdollmod.client.ClientRagdollHandler;
import com.ragdollmod.client.RagdollKeyBindings;
import com.ragdollmod.client.renderer.RagdollPlayerRenderer;
import com.ragdollmod.network.RagdollNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = PlayerRagdollMod.MOD_ID, dist = Dist.CLIENT)
public class PlayerRagdollClientMod {

    public PlayerRagdollClientMod(IEventBus modEventBus) {
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(RagdollKeyBindings::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.register(ClientRagdollHandler.class);
        NeoForge.EVENT_BUS.register(RagdollPlayerRenderer.class);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        PlayerRagdollMod.LOGGER.info("[PlayerRagdoll] Client setup complete.");
    }
}
