package com.ragdollmod.common.physics;

import net.minecraft.world.phys.Vec3;

/**
 * Represents one rigid-body segment of the ragdoll (head, torso, l-arm, r-arm, l-leg, r-leg).
 * Uses Verlet integration for stable, constraint-friendly simulation without a full physics lib.
 */
public class RagdollSegment {

    public static final double GRAVITY      = -0.08;
    public static final double DRAG         = 0.98;
    public static final double RESTITUTION  = 0.35;  // bounciness on floor hit
    public static final double FRICTION     = 0.75;

    // Current world position
    public double x, y, z;
    // Previous position (Verlet stores velocity implicitly)
    public double prevX, prevY, prevZ;
    // Half-extents of the collision box
    public final double hw, hh, hd;
    // Whether this segment is currently touching the ground
    public boolean onGround;

    public RagdollSegment(double x, double y, double z, double hw, double hh, double hd) {
        this.x = x; this.y = y; this.z = z;
        this.prevX = x; this.prevY = y; this.prevZ = z;
        this.hw = hw; this.hh = hh; this.hd = hd;
    }

    /** Apply an impulse (instantaneous velocity change). */
    public void applyImpulse(double ix, double iy, double iz) {
        prevX -= ix;
        prevY -= iy;
        prevZ -= iz;
    }

    /** Integrate one tick with gravity + drag. */
    public void integrate() {
        double vx = (x - prevX) * DRAG;
        double vy = (y - prevY) * DRAG + GRAVITY;
        double vz = (z - prevZ) * DRAG;

        prevX = x; prevY = y; prevZ = z;
        x += vx;
        y += vy;
        z += vz;
    }

    /** Resolve floor collision at groundY. Ceiling and walls handled in RagdollPhysics. */
    public void resolveFloor(double groundY) {
        if (y - hh < groundY) {
            double vy = y - prevY;
            y = groundY + hh;
            prevY = y + vy * RESTITUTION;   // bounce

            // Friction on horizontal movement when on ground
            double vx = (x - prevX) * FRICTION;
            double vz = (z - prevZ) * FRICTION;
            prevX = x - vx;
            prevZ = z - vz;
            onGround = true;
        } else {
            onGround = false;
        }
    }

    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    public Vec3 velocity() {
        return new Vec3(x - prevX, y - prevY, z - prevZ);
    }
}
