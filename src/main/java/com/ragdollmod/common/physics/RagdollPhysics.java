package com.ragdollmod.common.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-contained ragdoll simulation for one player.
 *
 * Segments:
 *   HEAD   (0)  – small box on top of torso
 *   TORSO  (1)  – large central mass
 *   L_ARM  (2)  – left upper arm
 *   R_ARM  (3)  – right upper arm
 *   L_FORE (4)  – left forearm
 *   R_FORE (5)  – right forearm
 *   L_THIGH(6)  – left upper leg
 *   R_THIGH(7)  – right upper leg
 *   L_SHIN (8)  – left lower leg
 *   R_SHIN (9)  – right lower leg
 *
 * All sizes are in Minecraft blocks (1 unit = 1 block).
 */
public class RagdollPhysics {

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

    private static final int CONSTRAINT_ITERATIONS = 8;

    public final RagdollSegment[] segments = new RagdollSegment[10];
    private final List<DistanceConstraint> constraints = new ArrayList<>();

    /**
     * Initialise the ragdoll at the given world position (player feet).
     */
    public RagdollPhysics(double px, double py, double pz) {
        // Build segments relative to player feet (py = bottom of player)
        // Each arg: x, y, z, half-width, half-height, half-depth
        segments[HEAD]    = new RagdollSegment(px,        py + 1.55, pz, 0.20, 0.175, 0.20);
        segments[TORSO]   = new RagdollSegment(px,        py + 1.15, pz, 0.25, 0.25,  0.125);
        segments[L_ARM]   = new RagdollSegment(px - 0.35, py + 1.25, pz, 0.075, 0.175, 0.075);
        segments[R_ARM]   = new RagdollSegment(px + 0.35, py + 1.25, pz, 0.075, 0.175, 0.075);
        segments[L_FORE]  = new RagdollSegment(px - 0.35, py + 0.95, pz, 0.06,  0.175, 0.06);
        segments[R_FORE]  = new RagdollSegment(px + 0.35, py + 0.95, pz, 0.06,  0.175, 0.06);
        segments[L_THIGH] = new RagdollSegment(px - 0.12, py + 0.60, pz, 0.09,  0.20,  0.09);
        segments[R_THIGH] = new RagdollSegment(px + 0.12, py + 0.60, pz, 0.09,  0.20,  0.09);
        segments[L_SHIN]  = new RagdollSegment(px - 0.12, py + 0.22, pz, 0.075, 0.195, 0.075);
        segments[R_SHIN]  = new RagdollSegment(px + 0.12, py + 0.22, pz, 0.075, 0.195, 0.075);

        buildConstraints();
    }

    private void buildConstraints() {
        double STIFF  = 0.0;   // near-rigid joint
        double MEDIUM = 0.05;  // slightly springy
        double LOOSE  = 0.15;  // floppy limb

        // Head ↔ Torso (neck)
        addConstraint(HEAD, TORSO, 0.40, STIFF);

        // Shoulders
        addConstraint(TORSO, L_ARM, 0.35, STIFF);
        addConstraint(TORSO, R_ARM, 0.35, STIFF);

        // Elbows
        addConstraint(L_ARM, L_FORE, 0.35, MEDIUM);
        addConstraint(R_ARM, R_FORE, 0.35, MEDIUM);

        // Hips
        addConstraint(TORSO, L_THIGH, 0.40, STIFF);
        addConstraint(TORSO, R_THIGH, 0.40, STIFF);

        // Knees
        addConstraint(L_THIGH, L_SHIN, 0.40, MEDIUM);
        addConstraint(R_THIGH, R_SHIN, 0.40, MEDIUM);

        // Cross-stability: prevent torso twisting
        addConstraint(L_ARM, R_ARM, 0.70, MEDIUM);
        addConstraint(L_THIGH, R_THIGH, 0.24, MEDIUM);
    }

    private void addConstraint(int a, int b, double rest, double compliance) {
        constraints.add(new DistanceConstraint(segments[a], segments[b], rest, compliance));
    }

    /**
     * Step the full simulation one Minecraft tick.
     * @param level  used for block collision queries
     */
    public void tick(Level level) {
        // 1. Integrate all segments
        for (RagdollSegment seg : segments) {
            seg.integrate();
        }

        // 2. Constraint solve (multiple iterations for stability)
        for (int iter = 0; iter < CONSTRAINT_ITERATIONS; iter++) {
            for (DistanceConstraint c : constraints) {
                c.solve();
            }
        }

        // 3. Block collision per segment
        for (RagdollSegment seg : segments) {
            resolveBlockCollisions(level, seg);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Block collision resolution
    // ──────────────────────────────────────────────────────────────

    private void resolveBlockCollisions(Level level, RagdollSegment seg) {
        AABB box = new AABB(
            seg.x - seg.hw, seg.y - seg.hh, seg.z - seg.hd,
            seg.x + seg.hw, seg.y + seg.hh, seg.z + seg.hd
        );

        int x0 = (int) Math.floor(seg.x - seg.hw - 0.5);
        int x1 = (int) Math.floor(seg.x + seg.hw + 0.5);
        int y0 = (int) Math.floor(seg.y - seg.hh - 0.5);
        int y1 = (int) Math.floor(seg.y + seg.hh + 0.5);
        int z0 = (int) Math.floor(seg.z - seg.hd - 0.5);
        int z1 = (int) Math.floor(seg.z + seg.hd + 0.5);

        seg.onGround = false;

        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    if (!level.isLoaded(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getCollisionShape(level, pos);
                    if (shape.isEmpty()) continue;

                    AABB blockBox = shape.bounds().move(bx, by, bz);
                    resolveOverlap(seg, blockBox);
                }
            }
        }
    }

    /**
     * Push the segment out of a solid block box along the axis of least penetration.
     */
    private void resolveOverlap(RagdollSegment seg, AABB block) {
        double segMinX = seg.x - seg.hw, segMaxX = seg.x + seg.hw;
        double segMinY = seg.y - seg.hh, segMaxY = seg.y + seg.hh;
        double segMinZ = seg.z - seg.hd, segMaxZ = seg.z + seg.hd;

        // No overlap?
        if (segMaxX <= block.minX || segMinX >= block.maxX) return;
        if (segMaxY <= block.minY || segMinY >= block.maxY) return;
        if (segMaxZ <= block.minZ || segMinZ >= block.maxZ) return;

        double overlapX = Math.min(segMaxX - block.minX, block.maxX - segMinX);
        double overlapY = Math.min(segMaxY - block.minY, block.maxY - segMinY);
        double overlapZ = Math.min(segMaxZ - block.minZ, block.maxZ - segMinZ);

        double vx = seg.x - seg.prevX;
        double vy = seg.y - seg.prevY;
        double vz = seg.z - seg.prevZ;

        if (overlapY <= overlapX && overlapY <= overlapZ) {
            // Resolve Y
            if (vy < 0 && seg.y < block.minY + (block.maxY - block.minY) * 0.5) {
                seg.y = block.minY - seg.hh;
                seg.prevY = seg.y + vy * RagdollSegment.RESTITUTION;
                // Friction
                seg.prevX = seg.x - vx * RagdollSegment.FRICTION;
                seg.prevZ = seg.z - vz * RagdollSegment.FRICTION;
                seg.onGround = true;
            } else {
                seg.y = block.maxY + seg.hh;
                seg.prevY = seg.y + vy * RagdollSegment.RESTITUTION;
                seg.onGround = true;
            }
        } else if (overlapX <= overlapZ) {
            // Resolve X
            if (seg.x < block.minX + (block.maxX - block.minX) * 0.5) {
                seg.x = block.minX - seg.hw;
            } else {
                seg.x = block.maxX + seg.hw;
            }
            seg.prevX = seg.x + vx * RagdollSegment.RESTITUTION;
        } else {
            // Resolve Z
            if (seg.z < block.minZ + (block.maxZ - block.minZ) * 0.5) {
                seg.z = block.minZ - seg.hd;
            } else {
                seg.z = block.maxZ + seg.hd;
            }
            seg.prevZ = seg.z + vz * RagdollSegment.RESTITUTION;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Locomotion impulses
    // ──────────────────────────────────────────────────────────────

    /**
     * Apply the ragdoll locomotion: pressing W/A/S/D alone drags the body weakly;
     * W+SPACE launches the player in the facing direction.
     *
     * @param dirX    move direction X (normalised)
     * @param dirZ    move direction Z (normalised)
     * @param jumping true if Space is held
     * @param yaw     player yaw in radians, used to compute facing for drag crawl
     */
    public void applyLocomotionImpulse(double dirX, double dirZ, boolean jumping, float yaw) {
        RagdollSegment torso = segments[TORSO];
        RagdollSegment head  = segments[HEAD];

        boolean hasDirection = dirX != 0 || dirZ != 0;

        if (jumping && hasDirection) {
            // LAUNCH: big impulse in movement direction + upward
            double launchH = 0.28;
            double launchV = 0.42;
            double ix = dirX * launchH;
            double iz = dirZ * launchH;
            // Apply to torso + head so whole body flies
            for (RagdollSegment seg : segments) {
                seg.applyImpulse(ix, launchV, iz);
            }
        } else if (jumping) {
            // Space alone = weak upward push (flail in place)
            for (RagdollSegment seg : segments) {
                seg.applyImpulse(0, 0.18, 0);
            }
        } else if (hasDirection) {
            // WASD only = slow dragging crawl — apply tiny impulse to torso only
            // The constraints drag the rest of the body along like a floppy noodle
            double drag = 0.04;
            torso.applyImpulse(dirX * drag, 0, dirZ * drag);
            head.applyImpulse(dirX * drag * 0.5, 0, dirZ * drag * 0.5);
        }
    }

    /**
     * Teleport the entire ragdoll to a new root position (used on respawn/enable).
     */
    public void teleportTo(double px, double py, double pz) {
        // Compute offset from current torso center
        RagdollSegment torso = segments[TORSO];
        double dx = px - torso.x;
        double dy = (py + 1.15) - torso.y;
        double dz = pz - torso.z;
        for (RagdollSegment seg : segments) {
            seg.x += dx; seg.prevX += dx;
            seg.y += dy; seg.prevY += dy;
            seg.z += dz; seg.prevZ += dz;
        }
    }

    /**
     * The "root" position of the ragdoll (center of torso) used to keep the
     * player entity's bounding box in roughly the right place.
     */
    public Vec3 rootPosition() {
        return segments[TORSO].position();
    }

    /**
     * Returns approximate feet-level Y (bottom of lowest leg segment).
     */
    public double feetY() {
        double minY = Double.MAX_VALUE;
        for (RagdollSegment seg : segments) {
            minY = Math.min(minY, seg.y - seg.hh);
        }
        return minY;
    }
}
