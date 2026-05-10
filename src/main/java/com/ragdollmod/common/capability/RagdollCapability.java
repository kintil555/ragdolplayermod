package com.ragdollmod.common.capability;

import com.ragdollmod.common.physics.RagdollPhysics;

public class RagdollCapability {
    private boolean active = false;
    private RagdollPhysics physics = null;

    public double moveX=0, moveZ=0;
    public boolean jumping = false;
    public float yaw = 0;

    public boolean isActive()          { return active; }
    public RagdollPhysics getPhysics() { return physics; }

    public void enable(double px, double py, double pz) {
        active = true;
        physics = new RagdollPhysics(px, py, pz);
    }

    public void disable() {
        active = false;
        physics = null;
    }
}
