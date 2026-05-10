package com.ragdollmod.client;

import com.ragdollmod.PlayerRagdollMod;
import com.ragdollmod.network.RagdollNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class PlayerRagdollClientMod implements ClientModInitializer {

    public static KeyBinding TOGGLE_KEY;

    @Override
    public void onInitializeClient() {
        PlayerRagdollMod.LOGGER.info("[PlayerRagdoll] Client init...");

        // Register keybind: R
        TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.playerragdoll.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.playerragdoll"
        ));

        // Register S→C packet handler (sync active state)
        ClientPlayNetworking.registerGlobalReceiver(
            RagdollNetwork.SyncPayload.ID,
            (payload, context) -> context.client().execute(() ->
                ClientRagdollHandler.setActive(payload.active())
            )
        );

        // Tick event: key polling + physics + input sending
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Key press → send toggle to server
            while (TOGGLE_KEY.wasPressed()) {
                ClientPlayNetworking.send(new RagdollNetwork.TogglePayload());
            }

            // Drive client-side physics and send input
            ClientRagdollHandler.tick(client);
        });

        PlayerRagdollMod.LOGGER.info("[PlayerRagdoll] Client init complete.");
    }
}
