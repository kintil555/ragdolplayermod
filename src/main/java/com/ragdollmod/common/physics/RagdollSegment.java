package com.ragdollmod.common.physics;

import net.minecraft.util.math.Vec3d;

public class RagdollSegment {
    public static final double GRAVITY     = -0.08;
    public static final double DRAG        = 0.98;
    public static final double RESTITUTION = 0.35;
    public static final double FRICTION    = 0.75;

    public double x, y, z;
    public double prevX, prevY, prevZ;
    public final double hw, hh, hd;
    public boolean onGround;

    public RagdollSegment(double x, double y, double z, double hw, double hh, double hd) {
        this.x = x; this.y = y; this.z = z;
        this.prevX = x; this.prevY = y; this.prevZ = z;
        this.hw = hw; this.hh = hh; this.hd = hd;
    }

    public void applyImpulse(double ix, double iy, double iz) {
        prevX -= ix; prevY -= iy; prevZ -= iz;
    }

    public void integrate() {
        double vx = (x - prevX) * DRAG;
        double vy = (y - prevY) * DRAG + GRAVITY;
        double vz = (z - prevZ) * DRAG;
        prevX = x; prevY = y; prevZ = z;
        x += vx; y += vy; z += vz;
    }

    public Vec3d position()  { return new Vec3d(x, y, z); }
    public Vec3d velocity()  { return new Vec3d(x - prevX, y - prevY, z - prevZ); }
}
