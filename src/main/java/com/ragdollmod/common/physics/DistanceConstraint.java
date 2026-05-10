package com.ragdollmod.common.physics;

/**
 * Positional distance constraint (XPBD-style) that keeps two segments
 * within [minDist, maxDist] of each other.  Iterating this multiple times
 * per tick gives stiffer, more stable joints.
 */
public class DistanceConstraint {

    public final RagdollSegment a;
    public final RagdollSegment b;
    public final double restLength;
    /** 0 = perfectly rigid, 1 = very soft spring */
    public final double compliance;

    public DistanceConstraint(RagdollSegment a, RagdollSegment b, double restLength, double compliance) {
        this.a = a;
        this.b = b;
        this.restLength = restLength;
        this.compliance = compliance;
    }

    /** Solve one iteration. Call several times per tick for rigidity. */
    public void solve() {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1e-6) return;

        double diff = (dist - restLength) / (dist * (2.0 + compliance));

        a.x += dx * diff;
        a.y += dy * diff;
        a.z += dz * diff;

        b.x -= dx * diff;
        b.y -= dy * diff;
        b.z -= dz * diff;
    }
}
