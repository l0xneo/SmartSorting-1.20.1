package com.smartsorting.block;

import com.smartsorting.routing.Connections;
import com.smartsorting.routing.NetworkVersion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockAttachment extends BlockBase implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final BooleanProperty[] CONNECTED = BlockPipe.CONNECTED;

    protected BlockAttachment() {
        super(2.0F);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.UP);
        for (BooleanProperty property : CONNECTED) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
        buildShapes();
    }

    public static Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }

    public static boolean connectsOn(BlockState state, Direction side) {
        return side != getFacing(state).getOpposite();
    }

    private static boolean connects(BlockState self, Direction side, BlockState neighbour) {
        return connectsOn(self, side) && Connections.pipeConnects(neighbour, side);
    }

    private static BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            state = state.setValue(CONNECTED[side.get3DDataValue()], connects(state, side, level.getBlockState(pos.relative(side))));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState().setValue(FACING, context.getClickedFace()), context.getLevel(), context.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbour, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbourPos) {
        return state.setValue(CONNECTED[side.get3DDataValue()], connects(state, side, neighbour));
    }

    @Override
    public boolean wrenchRotate(Level level, BlockPos pos, BlockState state, Direction clickedSide) {
        Direction next = Direction.from3DDataValue(state.getValue(FACING).get3DDataValue() + 1);
        level.setBlock(pos, withConnections(state.setValue(FACING, next), level, pos), 3);
        NetworkVersion.bump();
        return true;
    }

    @Override
    protected VoxelShape makeShape(BlockState state) {
        Direction facing = state.getValue(FACING);
        VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.or(
                Shapes.along(facing, 0, 2, 4, 12),
                Shapes.along(facing, 2, 10, 6, 10));
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
        return Shapes.turnSides(state, CONNECTED, rotation::rotate).setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return Shapes.turnSides(state, CONNECTED, mirror::mirror).setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(CONNECTED);
    }
}
