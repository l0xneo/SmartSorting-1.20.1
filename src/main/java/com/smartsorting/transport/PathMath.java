package com.smartsorting.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class PathMath {

    private PathMath() {
    }

    public static Vec3 point(List<BlockPos> path, float progress) {
        int segment = segment(path, progress);
        Vec3 from = Vec3.atCenterOf(path.get(segment));
        return from.lerp(Vec3.atCenterOf(path.get(segment + 1)), Math.min(1, progress - segment));
    }

    public static Vec3 direction(List<BlockPos> path, float progress) {
        int segment = segment(path, progress);
        return Vec3.atLowerCornerOf(path.get(segment + 1).subtract(path.get(segment)));
    }

    private static int segment(List<BlockPos> path, float progress) {
        return Math.max(0, Math.min((int) Math.floor(progress), path.size() - 2));
    }
}
