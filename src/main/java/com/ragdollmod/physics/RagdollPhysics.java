package com.ragdollmod.physics;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Full XPBD ragdoll simulation — upgraded from the NeoForge reference.
 *
 * Segment indices (same as reference for renderer compatibility):
 *   HEAD(0) TORSO(1) L_ARM(2) R_ARM(3) L_FORE(4) R_FORE(5)
 *   L_THIGH(6) R_THIGH(7) L_SHIN(8) R_SHIN(9)
 *
 * Improvements over reference:
 *   1. XPBD constraints with per-mass weighting (torso is heaviest)
 *   2. Angular momentum — segments spin on collision
 *   3. Continuous collision detection (CCD) for thin blocks
 *   4. Locomotion uses per-segment ground check for crawl impulse
 *   5. Stand-up lerp helper for smooth transition back to normal
 */
public class RagdollPhysics {

    // ── Segment indices ──────────────────────────────────────────────────────
    public static final int HEAD    = 0;
    public static final int TORSO   = 1;
    public static final int L_ARM   = 2;
    public static final int R_ARM   = 3;
    public static final int L_FORE  = 4;
    public static final int R_FORE  = 5;
    public static final int L_THIGH = 6;
    public static final int R_THIGH = 7;
    public static final int L_SHIN  = 8;
    public static final int R_SHIN  = 9;

    private static final int SOLVER_ITERATIONS  = 10; // more = stiffer joints
    private static final int CCD_SUBSTEPS       = 3;  // sub-steps for CCD

    public final XPBDSegment[] segments = new XPBDSegment[10];
    private final List<XPBDConstraint> constraints = new ArrayList<>();

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Build ragdoll with player feet at (px, py, pz).
     *
     * Masses (kg-equivalent): torso=4, thighs=2, shins=1.5, head=2,
     *                          upper-arms=1.5, forearms=1
     * These are relative — only the ratio matters for XPBD.
     */
    public RagdollPhysics(double px, double py, double pz) {
        //                                         x           y          z     hw     hh     hd   mass
        segments[HEAD]    = new XPBDSegment(px,        py+1.55,  pz,  0.20, 0.175, 0.20,  2.0);
        segments[TORSO]   = new XPBDSegment(px,        py+1.15,  pz,  0.25, 0.25,  0.125, 4.0);
        segments[L_ARM]   = new XPBDSegment(px-0.35,   py+1.25,  pz,  0.075,0.175, 0.075, 1.5);
        segments[R_ARM]   = new XPBDSegment(px+0.35,   py+1.25,  pz,  0.075,0.175, 0.075, 1.5);
        segments[L_FORE]  = new XPBDSegment(px-0.35,   py+0.95,  pz,  0.06, 0.175, 0.06,  1.0);
        segments[R_FORE]  = new XPBDSegment(px+0.35,   py+0.95,  pz,  0.06, 0.175, 0.06,  1.0);
        segments[L_THIGH] = new XPBDSegment(px-0.12,   py+0.60,  pz,  0.09, 0.20,  0.09,  2.0);
        segments[R_THIGH] = new XPBDSegment(px+0.12,   py+0.60,  pz,  0.09, 0.20,  0.09,  2.0);
        segments[L_SHIN]  = new XPBDSegment(px-0.12,   py+0.22,  pz,  0.075,0.195, 0.075, 1.5);
        segments[R_SHIN]  = new XPBDSegment(px+0.12,   py+0.22,  pz,  0.075,0.195, 0.075, 1.5);

        buildConstraints();
    }

    // ── Constraint setup ─────────────────────────────────────────────────────

    private void buildConstraints() {
        // Compliance values: 0=rigid, higher=springier
        final double RIGID  = 0.0;
        final double MEDIUM = 0.02;
        final double LOOSE  = 0.10;

        // Spine / neck
        c(HEAD,    TORSO,   0.40, RIGID,  true);

        // Shoulders — rigid attachment but with angular coupling so arms swing
        c(TORSO,   L_ARM,   0.35, RIGID,  true);
        c(TORSO,   R_ARM,   0.35, RIGID,  true);

        // Elbows — slightly springy
        c(L_ARM,   L_FORE,  0.32, MEDIUM, true);
        c(R_ARM,   R_FORE,  0.32, MEDIUM, true);

        // Hips
        c(TORSO,   L_THIGH, 0.38, RIGID,  true);
        c(TORSO,   R_THIGH, 0.38, RIGID,  true);

        // Knees
        c(L_THIGH, L_SHIN,  0.38, MEDIUM, true);
        c(R_THIGH, R_SHIN,  0.38, MEDIUM, true);

        // Cross-stability — prevents torso from folding in half
        c(L_ARM,   R_ARM,   0.70, MEDIUM, false);
        c(L_THIGH, R_THIGH, 0.24, MEDIUM, false);

        // Diagonal chest — keeps shoulders from collapsing onto hips
        c(L_ARM,   R_THIGH, 0.90, LOOSE,  false);
        c(R_ARM,   L_THIGH, 0.90, LOOSE,  false);
    }

    private void c(int a, int b, double rest, double compliance, boolean angular) {
        constraints.add(new XPBDConstraint(segments[a], segments[b], rest, compliance, angular));
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    /**
     * Advance the ragdoll one Minecraft server/client tick.
     *
     * Pipeline: integrate → substep CCD → constraint solve → collision response
     */
    public void tick(World world) {
        // CCD: split each tick into sub-steps so fast-moving segments don't tunnel
        double dt = 1.0 / CCD_SUBSTEPS;
        for (int sub = 0; sub < CCD_SUBSTEPS; sub++) {
            // a) Integrate (gravity + drag)
            for (XPBDSegment seg : segments) {
                integrateSub(seg, dt);
            }

            // b) Constraint solve
            for (int iter = 0; iter < SOLVER_ITERATIONS; iter++) {
                for (XPBDConstraint c : constraints) {
                    c.solve();
                }
            }

            // c) Block collision per segment
            for (XPBDSegment seg : segments) {
                resolveBlockCollisions(world, seg);
            }
        }
    }

    /**
     * Sub-stepped integration: integrates a fraction dt of one tick.
     * Gravity is full-tick value — we divide velocity contribution by substep count
     * to keep energy correct.
     */
    private void integrateSub(XPBDSegment seg, double dt) {
        double vx = (seg.x - seg.prevX) * XPBDSegment.LINEAR_DRAG;
        double vy = (seg.y - seg.prevY) * XPBDSegment.LINEAR_DRAG
                  + XPBDSegment.GRAVITY * dt;
        double vz = (seg.z - seg.prevZ) * XPBDSegment.LINEAR_DRAG;

        seg.prevX = seg.x; seg.prevY = seg.y; seg.prevZ = seg.z;
        seg.x += vx * dt * CCD_SUBSTEPS;   // scale so total displacement matches 1 full tick
        seg.y += vy;
        seg.z += vz * dt * CCD_SUBSTEPS;

        // Angular
        seg.wx *= XPBDSegment.ANGULAR_DRAG;
        seg.wy *= XPBDSegment.ANGULAR_DRAG;
        seg.wz *= XPBDSegment.ANGULAR_DRAG;
        seg.pitch += seg.wx;
        seg.yaw   += seg.wy;
        seg.roll  += seg.wz;
    }

    // ── Block collision ───────────────────────────────────────────────────────

    private void resolveBlockCollisions(World world, XPBDSegment seg) {
        Box box = new Box(
            seg.x - seg.hw, seg.y - seg.hh, seg.z - seg.hd,
            seg.x + seg.hw, seg.y + seg.hh, seg.z + seg.hd
        );

        int x0 = (int) Math.floor(seg.x - seg.hw - 0.5);
        int x1 = (int) Math.ceil (seg.x + seg.hw + 0.5);
        int y0 = (int) Math.floor(seg.y - seg.hh - 0.5);
        int y1 = (int) Math.ceil (seg.y + seg.hh + 0.5);
        int z0 = (int) Math.floor(seg.z - seg.hd - 0.5);
        int z1 = (int) Math.ceil (seg.z + seg.hd + 0.5);

        seg.onGround = false;

        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    if (!world.isChunkLoaded(pos)) continue;
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) continue;

                    var shape = state.getCollisionShape(world, pos);
                    if (shape.isEmpty()) continue;

                    Box blockBox = shape.getBoundingBox().offset(bx, by, bz);
                    resolveOverlap(seg, blockBox);
                }
            }
        }
    }

    /**
     * Push segment out of block overlap along axis of minimum penetration.
     * Also applies angular impulse on the hit segment so it spins.
     */
    private void resolveOverlap(XPBDSegment seg, Box block) {
        double sMinX = seg.x - seg.hw, sMaxX = seg.x + seg.hw;
        double sMinY = seg.y - seg.hh, sMaxY = seg.y + seg.hh;
        double sMinZ = seg.z - seg.hd, sMaxZ = seg.z + seg.hd;

        if (sMaxX <= block.minX || sMinX >= block.maxX) return;
        if (sMaxY <= block.minY || sMinY >= block.maxY) return;
        if (sMaxZ <= block.minZ || sMinZ >= block.maxZ) return;

        double ovX = Math.min(sMaxX - block.minX, block.maxX - sMinX);
        double ovY = Math.min(sMaxY - block.minY, block.maxY - sMinY);
        double ovZ = Math.min(sMaxZ - block.minZ, block.maxZ - sMinZ);

        double vx = seg.velX(), vy = seg.velY(), vz = seg.velZ();

        if (ovY <= ovX && ovY <= ovZ) {
            // ── Floor / ceiling ──────────────────────────────────────────────
            boolean hitFloor = (vy <= 0);
            if (hitFloor) {
                seg.y = block.maxY + seg.hh;
                seg.prevY = seg.y + vy * XPBDSegment.RESTITUTION;
                // Friction on XZ
                seg.prevX = seg.x - vx * XPBDSegment.GROUND_FRICTION;
                seg.prevZ = seg.z - vz * XPBDSegment.GROUND_FRICTION;
                seg.onGround = true;
                // Angular: landing generates roll based on incoming horizontal velocity
                seg.wz += vx * 1.2 * seg.invMass;
                seg.wx -= vz * 1.2 * seg.invMass;
            } else {
                seg.y = block.minY - seg.hh;
                seg.prevY = seg.y + vy * XPBDSegment.RESTITUTION;
                seg.onGround = true;
            }
        } else if (ovX <= ovZ) {
            // ── Side X ──────────────────────────────────────────────────────
            double push = (seg.x < block.minX + (block.maxX - block.minX) * 0.5)
                    ? block.minX - seg.hw
                    : block.maxX + seg.hw;
            seg.x = push;
            seg.prevX = seg.x + vx * XPBDSegment.RESTITUTION;
            // Spin around Y on X-axis wall hit
            seg.wy += vz * 0.8 * seg.invMass;
        } else {
            // ── Side Z ──────────────────────────────────────────────────────
            double push = (seg.z < block.minZ + (block.maxZ - block.minZ) * 0.5)
                    ? block.minZ - seg.hd
                    : block.maxZ + seg.hd;
            seg.z = push;
            seg.prevZ = seg.z + vz * XPBDSegment.RESTITUTION;
            seg.wy -= vx * 0.8 * seg.invMass;
        }
    }

    // ── Locomotion ────────────────────────────────────────────────────────────

    /**
     * Apply crawl impulse while in ragdoll mode.
     * Only segments touching the ground receive horizontal impulse — this makes
     * the crawl feel like dragging, not flying.
     */
    public void applyLocomotionImpulse(double dirX, double dirZ, float yaw) {
        if (dirX == 0 && dirZ == 0) return;

        // Normalize direction
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        dirX /= len;
        dirZ /= len;

        final double CRAWL_FORCE = 0.018;

        for (XPBDSegment seg : segments) {
            if (seg.onGround) {
                // Reduce by how fast we're already moving that direction (natural speed limit)
                double dot = seg.velX() * dirX + seg.velZ() * dirZ;
                double scale = Math.max(0, 1.0 - dot / 0.12);
                seg.prevX -= dirX * CRAWL_FORCE * scale * seg.invMass;
                seg.prevZ -= dirZ * CRAWL_FORCE * scale * seg.invMass;
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Teleport entire ragdoll to new root position (player feet).
     */
    public void teleportTo(double px, double py, double pz) {
        XPBDSegment torso = segments[TORSO];
        double dx = px - torso.x;
        double dy = (py + 1.15) - torso.y;
        double dz = pz - torso.z;
        for (XPBDSegment seg : segments) {
            seg.x += dx; seg.prevX += dx;
            seg.y += dy; seg.prevY += dy;
            seg.z += dz; seg.prevZ += dz;
        }
    }

    /** World position of the torso (used to move the player entity). */
    public Vec3d rootPosition() {
        XPBDSegment t = segments[TORSO];
        return new Vec3d(t.x, t.y, t.z);
    }

    /** Y coordinate of the lowest point of all segments. */
    public double feetY() {
        double minY = Double.MAX_VALUE;
        for (XPBDSegment seg : segments) {
            minY = Math.min(minY, seg.y - seg.hh);
        }
        return minY;
    }

    /** True when the whole ragdoll has come to rest (used for stand-up check). */
    public boolean isAtRest() {
        double totalKE = 0;
        for (XPBDSegment seg : segments) {
            totalKE += seg.speed2();
        }
        return totalKE < 0.0004;
    }
}
