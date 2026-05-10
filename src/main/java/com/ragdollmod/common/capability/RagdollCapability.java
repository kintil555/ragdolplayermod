package com.ragdollmod.common.capability;

import com.ragdollmod.common.physics.RagdollPhysics;
import net.minecraft.nbt.CompoundTag;

/**
 * Per-player data: whether ragdoll is active and the physics simulation object.
 */
public class RagdollCapability {

    private boolean active = false;
    private RagdollPhysics physics = null;

    // Input state replicated from client → server each tick
    public double moveX = 0;
    public double moveZ = 0;
    public boolean jumping = false;
    public float yaw = 0;

    public boolean isActive() { return active; }

    public RagdollPhysics getPhysics() { return physics; }

    public void enable(double px, double py, double pz) {
        active = true;
        physics = new RagdollPhysics(px, py, pz);
    }

    public void disable() {
        active = false;
        physics = null;
    }

    // ── NBT persistence (survives logout) ─────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", active);
        // Physics state is ephemeral; we only persist the toggle
        return tag;
    }

    public void load(CompoundTag tag) {
        active = tag.getBoolean("active");
        // physics will be re-created when the player next logs in if active
    }
}
