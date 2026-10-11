package com.smartsorting.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.UnaryOperator;

public final class Shapes {

    public static final VoxelShape PIPE_CORE = cube(6, 10);

    private static final VoxelShape[] PIPE_ARMS = new VoxelShape[6];

    static {
        for (Direction side : Direction.values()) {
            PIPE_ARMS[side.get3DDataValue()] = along(side, 10, 16, 6, 10);
        }
    }

    private Shapes() {
    }

    public static VoxelShape pipeArm(Direction side) {
        return PIPE_ARMS[side.get3DDataValue()];
    }

    public static VoxelShape cube(double from, double to) {
        return Block.box(from, from, from, to, to, to);
    }

    public static VoxelShape along(Direction side, double a0, double a1, double p0, double p1) {
        double lo = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? a0 : 16 - a1;
        double hi = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? a1 : 16 - a0;
        return switch (side.getAxis()) {
            case X -> Block.box(lo, p0, p0, hi, p1, p1);
            case Y -> Block.box(p0, lo, p0, p1, hi, p1);
            case Z -> Block.box(p0, p0, lo, p1, p1, hi);
        };
    }

    public static <T extends Comparable<T>> BlockState turnSides(BlockState state, Property<T>[] sides, UnaryOperator<Direction> turn) {
        BlockState result = state;
        for (Direction side : Direction.values()) {
            result = result.setValue(sides[turn.apply(side).get3DDataValue()], state.getValue(sides[side.get3DDataValue()]));
        }
        return result;
    }
}
