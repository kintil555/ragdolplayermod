package com.ragdollmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class RagdollKeyBindings {

    public static final KeyMapping TOGGLE_RAGDOLL = new KeyMapping(
        "key.playerragdoll.toggle",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,        // default: R
        "key.playerragdoll.category"
    );

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_RAGDOLL);
    }
}
