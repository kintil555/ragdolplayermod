package com.ragdollmod.physics;

/**
 * Mutable ragdoll state stored per-player on the server.
 * Attached to ServerPlayerEntity via Fabric's attachment API alternative —
 * here we just use a static WeakHashMap (safe: ServerPlayerEntity is GC'd on disconnect).
 */
public class RagdollState {

    private boolean active = false;
    private RagdollPhysics physics = null;

    // Latest input from client
    public double moveX  = 0;
    public double moveZ  = 0;
    public float  yaw    = 0;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    public void enable(double px, double py, double pz) {
        if (active) return;
        active = true;
        physics = new RagdollPhysics(px, py, pz);
        // Initial fall impulse — tumble effect on activation
        for (XPBDSegment seg : physics.segments) {
            double rx = (Math.random() - 0.5) * 0.12;
            double rz = (Math.random() - 0.5) * 0.12;
            seg.applyImpulse(rx, -0.35, rz);
            seg.wx = (Math.random() - 0.5) * 0.8;
            seg.wz = (Math.random() - 0.5) * 0.8;
        }
    }

    public void disable() {
        active  = false;
        physics = null;
        moveX   = 0;
        moveZ   = 0;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public boolean  isActive()  { return active;  }
    public RagdollPhysics getPhysics() { return physics; }
}
