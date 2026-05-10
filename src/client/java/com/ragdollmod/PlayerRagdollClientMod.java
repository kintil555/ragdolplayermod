package com.ragdollmod;

import com.ragdollmod.client.ClientRagdollHandler;
import com.ragdollmod.client.RagdollKeyBindings;
import com.ragdollmod.network.RagdollInputPayload;
import com.ragdollmod.network.RagdollSyncPayload;
import com.ragdollmod.network.ToggleRagdollPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class PlayerRagdollClientMod implements ClientModInitializer {

    private static int sendTimer = 0;

    @Override
    public void onInitializeClient() {
        RagdollKeyBindings.register();

        // Receive sync from server
        ClientPlayNetworking.registerGlobalReceiver(RagdollSyncPayload.ID, (payload, ctx) ->
            ClientRagdollHandler.setLocalRagdollActive(payload.active())
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Toggle key
            while (RagdollKeyBindings.TOGGLE_RAGDOLL.wasPressed()) {
                ClientPlayNetworking.send(new ToggleRagdollPayload());
            }

            // Send input to server every 2 ticks
            if (ClientRagdollHandler.isActive()) {
                sendTimer++;
                if (sendTimer >= 2) {
                    sendTimer = 0;
                    float yaw = (float) Math.toRadians(client.player.getYaw());
                    boolean fwd   = client.options.forwardKey.isPressed();
                    boolean back  = client.options.backKey.isPressed();
                    boolean left  = client.options.leftKey.isPressed();
                    boolean right = client.options.rightKey.isPressed();
                    double moveX=0, moveZ=0;
                    if (fwd||back||left||right) {
                        double sin=Math.sin(yaw), cos=Math.cos(yaw);
                        if (fwd)  { moveX+=-sin; moveZ+=cos; }
                        if (back) { moveX+=sin;  moveZ-=cos; }
                        if (right){ moveX+=cos;  moveZ+=sin; }
                        if (left) { moveX-=cos;  moveZ-=sin; }
                        double len=Math.sqrt(moveX*moveX+moveZ*moveZ);
                        if (len>1e-6){ moveX/=len; moveZ/=len; }
                    }
                    ClientPlayNetworking.send(new RagdollInputPayload(moveX, moveZ, false, yaw));
                }
            }
        });
    }
}
