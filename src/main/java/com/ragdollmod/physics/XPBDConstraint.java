package com.ragdollmod.physics;

/**
 * XPBD (Extended Position-Based Dynamics) distance constraint.
 *
 * Improvement over the NeoForge reference DistanceConstraint:
 *  - Per-joint compliance properly accounts for invMass weighting (heavier segments move less)
 *  - Supports asymmetric mass — torso barely moves while arms flail around it
 *  - Optional angular coupling: joint can generate torque on both segments
 */
public class XPBDConstraint {

    public final XPBDSegment a;
    public final XPBDSegment b;
    public final double restLength;

    /**
     * XPBD compliance α.
     * 0.0  = perfectly rigid (hard joint — neck, hips)
     * 0.02 = slightly springy (shoulder, knee)
     * 0.12 = floppy (wrist, ankle)
     */
    public final double compliance;

    /** If true, constraint also applies a small counter-torque so limbs curl naturally. */
    public final boolean angularCoupling;

    public XPBDConstraint(XPBDSegment a, XPBDSegment b,
                           double restLength, double compliance,
                           boolean angularCoupling) {
        this.a = a;
        this.b = b;
        this.restLength = restLength;
        this.compliance = compliance;
        this.angularCoupling = angularCoupling;
    }

    public XPBDConstraint(XPBDSegment a, XPBDSegment b, double restLength, double compliance) {
        this(a, b, restLength, compliance, false);
    }

    /**
     * Solve one XPBD iteration.
     *
     * Unlike the reference code which divides by (2 + compliance), the proper XPBD
     * formulation weights each side by invMass so heavier segments barely move:
     *
     *   λ = -C / (w_a + w_b + α)
     *   Δx_a = +w_a * λ * n
     *   Δx_b = -w_b * λ * n
     *
     * where n = unit vector a→b, C = dist - restLength.
     */
    public void solve() {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1e-8) return;

        double C = dist - restLength;
        double wSum = a.invMass + b.invMass + compliance;
        if (wSum < 1e-10) return;

        double lambda = -C / wSum;

        double nx = dx / dist;
        double ny = dy / dist;
        double nz = dz / dist;

        // Positional correction
        a.x -= nx * lambda * a.invMass;
        a.y -= ny * lambda * a.invMass;
        a.z -= nz * lambda * a.invMass;

        b.x += nx * lambda * b.invMass;
        b.y += ny * lambda * b.invMass;
        b.z += nz * lambda * b.invMass;

        // Angular coupling — generate a small restoring torque so limbs don't hyper-extend
        if (angularCoupling && Math.abs(C) > 0.01) {
            double torqueScale = lambda * 0.15;
            // Torque direction = n × up, makes the joint want to rotate back
            double tx = ny * torqueScale;
            double tz = -nx * torqueScale;
            a.wx -= tx * a.invMass;
            a.wz -= tz * a.invMass;
            b.wx += tx * b.invMass;
            b.wz += tz * b.invMass;
        }
    }
}
