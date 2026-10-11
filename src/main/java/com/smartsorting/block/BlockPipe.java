package com.smartsorting.block;

import com.smartsorting.routing.Connections;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockPipe extends BlockBase {

    public static final BooleanProperty[] CONNECTED = new BooleanProperty[6];

    static {
        for (Direction side : Direction.values()) {
            CONNECTED[side.get3DDataValue()] = BooleanProperty.create(side.getName());
        }
    }

    public BlockPipe() {
        super(1.0F);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTED) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
        buildShapes();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.values()) {
            BlockState neighbour = context.getLevel().getBlockState(context.getClickedPos().relative(side));
            state = state.setValue(CONNECTED[side.get3DDataValue()], Connections.pipeConnects(neighbour, side));
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbour, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbourPos) {
        return state.setValue(CONNECTED[side.get3DDataValue()], Connections.pipeConnects(neighbour, side));
    }

    @Override
    protected VoxelShape makeShape(BlockState state) {
        VoxelShape shape = Shapes.PIPE_CORE;
        for (Direction side : Direction.values()) {
            if (state.getValue(CONNECTED[side.get3DDataValue()])) {
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, Shapes.pipeArm(side));
            }
        }
        return shape;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation) {
        return Shapes.turnSides(state, CONNECTED, rotation::rotate);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return Shapes.turnSides(state, CONNECTED, mirror::mirror);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED);
    }
}
