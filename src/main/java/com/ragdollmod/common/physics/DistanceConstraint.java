package com.ragdollmod.common.physics;

public class DistanceConstraint {
    public final RagdollSegment a, b;
    public final double restLength, compliance;

    public DistanceConstraint(RagdollSegment a, RagdollSegment b, double restLength, double compliance) {
        this.a = a; this.b = b;
        this.restLength = restLength; this.compliance = compliance;
    }

    public void solve() {
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (dist < 1e-6) return;
        double diff = (dist - restLength) / (dist * (2.0 + compliance));
        a.x += dx*diff; a.y += dy*diff; a.z += dz*diff;
        b.x -= dx*diff; b.y -= dy*diff; b.z -= dz*diff;
    }
}
