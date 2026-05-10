package com.ragdollmod.physics;

/**
 * One rigid segment in the ragdoll skeleton.
 *
 * Upgrade over the NeoForge reference:
 *  - Stores angular velocity (wx, wy, wz) so segments can spin when they hit things
 *  - DRAG and ANGULAR_DRAG are tuned separately
 *  - applyImpulseAtOffset() lets constraints apply torque, not just linear force
 */
public class XPBDSegment {

    // ── Physics constants ────────────────────────────────────────────────────
    public static final double GRAVITY         = -0.055; // blocks/tick² (slightly lighter than NeoForge ref)
    public static final double LINEAR_DRAG     = 0.985;
    public static final double ANGULAR_DRAG    = 0.88;   // spins damp faster than translation
    public static final double RESTITUTION     = 0.25;   // bounciness on floor hit
    public static final double GROUND_FRICTION = 0.72;   // horizontal friction when grounded
    public static final double AIR_FRICTION    = 0.995;  // almost no air resistance

    // ── Kinematics ───────────────────────────────────────────────────────────
    /** Current world position (centre of AABB). */
    public double x, y, z;
    /** Previous position — Verlet stores velocity implicitly as (pos - prevPos). */
    public double prevX, prevY, prevZ;
    /** Angular velocity (radians/tick) around each world axis. */
    public double wx, wy, wz;
    /** Current orientation Euler angles (radians) for rendering. */
    public double pitch, yaw, roll;

    // ── Collision geometry ───────────────────────────────────────────────────
    /** Half-extents of the collision AABB. */
    public final double hw, hh, hd;
    /** 1/mass — heavier segments (torso) move less. */
    public final double invMass;

    // ── State ────────────────────────────────────────────────────────────────
    public boolean onGround;

    // ── Constructor ──────────────────────────────────────────────────────────
    public XPBDSegment(double x, double y, double z,
                       double hw, double hh, double hd,
                       double mass) {
        this.x = x; this.y = y; this.z = z;
        this.prevX = x; this.prevY = y; this.prevZ = z;
        this.hw = hw; this.hh = hh; this.hd = hd;
        this.invMass = (mass > 0) ? 1.0 / mass : 0.0;
    }

    // ── Verlet integration ───────────────────────────────────────────────────

    /**
     * Advance one tick.
     * Uses Verlet integration for position and simple Euler for angular velocity.
     */
    public void integrate() {
        // Linear velocity = current - previous (Verlet)
        double vx = (x - prevX) * LINEAR_DRAG;
        double vy = (y - prevY) * LINEAR_DRAG + GRAVITY;
        double vz = (z - prevZ) * LINEAR_DRAG;

        prevX = x; prevY = y; prevZ = z;
        x += vx;
        y += vy;
        z += vz;

        // Angular integration — damp and accumulate into orientation
        wx *= ANGULAR_DRAG;
        wy *= ANGULAR_DRAG;
        wz *= ANGULAR_DRAG;

        pitch += wx;
        yaw   += wy;
        roll  += wz;
    }

    // ── Impulse helpers ──────────────────────────────────────────────────────

    /**
     * Apply a linear impulse (changes velocity this tick).
     * In Verlet: delta-v = move previous position backwards by impulse amount.
     */
    public void applyImpulse(double ix, double iy, double iz) {
        if (invMass == 0) return;
        prevX -= ix * invMass;
        prevY -= iy * invMass;
        prevZ -= iz * invMass;
    }

    /**
     * Apply an impulse at a world-space offset from centre.
     * Generates both linear and angular response — used by collision resolver
     * so segments spin when a corner hits a block.
     *
     * @param ix,iy,iz  impulse vector
     * @param rx,ry,rz  offset from segment centre to point of application
     */
    public void applyImpulseAtOffset(double ix, double iy, double iz,
                                     double rx, double ry, double rz) {
        applyImpulse(ix, iy, iz);
        // Torque = r × F  (cross product)
        wx += (ry * iz - rz * iy) * invMass * 0.6;
        wy += (rz * ix - rx * iz) * invMass * 0.6;
        wz += (rx * iy - ry * ix) * invMass * 0.6;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public double velX() { return x - prevX; }
    public double velY() { return y - prevY; }
    public double velZ() { return z - prevZ; }

    public double speed2() {
        double vx = velX(), vy = velY(), vz = velZ();
        return vx * vx + vy * vy + vz * vz;
    }
}
