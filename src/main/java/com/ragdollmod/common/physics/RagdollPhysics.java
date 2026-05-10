package com.ragdollmod.common.physics;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class RagdollPhysics {
    public static final int HEAD=0,TORSO=1,L_ARM=2,R_ARM=3,L_FORE=4,R_FORE=5,L_THIGH=6,R_THIGH=7,L_SHIN=8,R_SHIN=9;
    private static final int CONSTRAINT_ITERS = 8;

    public final RagdollSegment[] segments = new RagdollSegment[10];
    private final List<DistanceConstraint> constraints = new ArrayList<>();

    public RagdollPhysics(double px, double py, double pz) {
        segments[HEAD]    = new RagdollSegment(px,        py+1.55, pz, 0.20, 0.175, 0.20);
        segments[TORSO]   = new RagdollSegment(px,        py+1.15, pz, 0.25, 0.25,  0.125);
        segments[L_ARM]   = new RagdollSegment(px-0.35,   py+1.25, pz, 0.075,0.175, 0.075);
        segments[R_ARM]   = new RagdollSegment(px+0.35,   py+1.25, pz, 0.075,0.175, 0.075);
        segments[L_FORE]  = new RagdollSegment(px-0.35,   py+0.95, pz, 0.06, 0.175, 0.06);
        segments[R_FORE]  = new RagdollSegment(px+0.35,   py+0.95, pz, 0.06, 0.175, 0.06);
        segments[L_THIGH] = new RagdollSegment(px-0.12,   py+0.60, pz, 0.09, 0.20,  0.09);
        segments[R_THIGH] = new RagdollSegment(px+0.12,   py+0.60, pz, 0.09, 0.20,  0.09);
        segments[L_SHIN]  = new RagdollSegment(px-0.12,   py+0.22, pz, 0.075,0.195, 0.075);
        segments[R_SHIN]  = new RagdollSegment(px+0.12,   py+0.22, pz, 0.075,0.195, 0.075);
        buildConstraints();
    }

    private void buildConstraints() {
        addC(HEAD,TORSO,0.40,0.0);
        addC(TORSO,L_ARM,0.35,0.0);  addC(TORSO,R_ARM,0.35,0.0);
        addC(L_ARM,L_FORE,0.35,0.05);addC(R_ARM,R_FORE,0.35,0.05);
        addC(TORSO,L_THIGH,0.40,0.0);addC(TORSO,R_THIGH,0.40,0.0);
        addC(L_THIGH,L_SHIN,0.40,0.05);addC(R_THIGH,R_SHIN,0.40,0.05);
        addC(L_ARM,R_ARM,0.70,0.05);
        addC(L_THIGH,R_THIGH,0.24,0.05);
    }

    private void addC(int a, int b, double rest, double comp) {
        constraints.add(new DistanceConstraint(segments[a], segments[b], rest, comp));
    }

    public void tick(World world) {
        for (RagdollSegment seg : segments) seg.integrate();
        for (int i = 0; i < CONSTRAINT_ITERS; i++)
            for (DistanceConstraint c : constraints) c.solve();
        for (RagdollSegment seg : segments) resolveBlockCollisions(world, seg);
    }

    private void resolveBlockCollisions(World world, RagdollSegment seg) {
        int x0=(int)Math.floor(seg.x-seg.hw-0.5), x1=(int)Math.floor(seg.x+seg.hw+0.5);
        int y0=(int)Math.floor(seg.y-seg.hh-0.5), y1=(int)Math.floor(seg.y+seg.hh+0.5);
        int z0=(int)Math.floor(seg.z-seg.hd-0.5), z1=(int)Math.floor(seg.z+seg.hd+0.5);
        seg.onGround = false;
        for (int bx=x0;bx<=x1;bx++) for (int by=y0;by<=y1;by++) for (int bz=z0;bz<=z1;bz++) {
            BlockPos pos = new BlockPos(bx,by,bz);
            if (!world.isChunkLoaded(pos)) continue;
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) continue;
            VoxelShape shape = state.getCollisionShape(world, pos);
            if (shape.isEmpty()) continue;
            Box blockBox = shape.getBoundingBox().offset(bx,by,bz);
            resolveOverlap(seg, blockBox);
        }
    }

    private void resolveOverlap(RagdollSegment seg, Box block) {
        double sMinX=seg.x-seg.hw,sMaxX=seg.x+seg.hw;
        double sMinY=seg.y-seg.hh,sMaxY=seg.y+seg.hh;
        double sMinZ=seg.z-seg.hd,sMaxZ=seg.z+seg.hd;
        if (sMaxX<=block.minX||sMinX>=block.maxX) return;
        if (sMaxY<=block.minY||sMinY>=block.maxY) return;
        if (sMaxZ<=block.minZ||sMinZ>=block.maxZ) return;

        double ox=Math.min(sMaxX-block.minX,block.maxX-sMinX);
        double oy=Math.min(sMaxY-block.minY,block.maxY-sMinY);
        double oz=Math.min(sMaxZ-block.minZ,block.maxZ-sMinZ);

        double vx=seg.x-seg.prevX,vy=seg.y-seg.prevY,vz=seg.z-seg.prevZ;

        if (oy<=ox&&oy<=oz) {
            if (vy<0&&seg.y<block.minY+(block.maxY-block.minY)*0.5) {
                seg.y=block.minY-seg.hh;
                seg.prevY=seg.y+vy*RagdollSegment.RESTITUTION;
                seg.prevX=seg.x-vx*RagdollSegment.FRICTION;
                seg.prevZ=seg.z-vz*RagdollSegment.FRICTION;
                seg.onGround=true;
            } else {
                seg.y=block.maxY+seg.hh;
                seg.prevY=seg.y+vy*RagdollSegment.RESTITUTION;
                seg.onGround=true;
            }
        } else if (ox<=oz) {
            seg.x=(seg.x<block.minX+(block.maxX-block.minX)*0.5)?block.minX-seg.hw:block.maxX+seg.hw;
            seg.prevX=seg.x+vx*RagdollSegment.RESTITUTION;
        } else {
            seg.z=(seg.z<block.minZ+(block.maxZ-block.minZ)*0.5)?block.minZ-seg.hd:block.maxZ+seg.hd;
            seg.prevZ=seg.z+vz*RagdollSegment.RESTITUTION;
        }
    }

    public void applyLocomotionImpulse(double dirX, double dirZ, boolean jumping, float yaw) {
        boolean hasDir = dirX!=0||dirZ!=0;
        if (jumping&&hasDir) {
            for (RagdollSegment s:segments) s.applyImpulse(dirX*0.28,0.42,dirZ*0.28);
        } else if (jumping) {
            for (RagdollSegment s:segments) s.applyImpulse(0,0.18,0);
        } else if (hasDir) {
            segments[TORSO].applyImpulse(dirX*0.04,0,dirZ*0.04);
            segments[HEAD].applyImpulse(dirX*0.02,0,dirZ*0.02);
        }
    }

    public void teleportTo(double px, double py, double pz) {
        RagdollSegment torso=segments[TORSO];
        double dx=px-torso.x,dy=(py+1.15)-torso.y,dz=pz-torso.z;
        for (RagdollSegment s:segments){ s.x+=dx;s.prevX+=dx;s.y+=dy;s.prevY+=dy;s.z+=dz;s.prevZ+=dz; }
    }

    public Vec3d rootPosition() { return segments[TORSO].position(); }

    public double feetY() {
        double min=Double.MAX_VALUE;
        for (RagdollSegment s:segments) min=Math.min(min,s.y-s.hh);
        return min;
    }
}
