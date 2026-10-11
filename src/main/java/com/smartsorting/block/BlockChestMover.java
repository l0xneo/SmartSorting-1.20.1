package com.smartsorting.block;

import com.smartsorting.tile.TileChestMover;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockChestMover extends BlockBase implements EntityBlock {

    @SuppressWarnings("unchecked")
    public static final EnumProperty<MoverSide>[] SIDES = new EnumProperty[6];

    static {
        for (Direction side : Direction.values()) {
            SIDES[side.get3DDataValue()] = EnumProperty.create(side.getName(), MoverSide.class);
        }
    }

    public BlockChestMover() {
        super(2.0F);
        BlockState state = stateDefinition.any();
        for (EnumProperty<MoverSide> side : SIDES) {
            state = state.setValue(side, MoverSide.OFF);
        }
        registerDefaultState(state);
        buildShapes();
    }

    public static MoverSide mode(BlockState state, Direction side) {
        return state.getValue(SIDES[side.get3DDataValue()]);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileChestMover(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, tile) -> {
            if (tile instanceof TileChestMover mover) {
                mover.tick();
            }
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        wake(level, pos);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        wake(level, pos);
    }

    private static void wake(LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TileChestMover mover) {
            mover.wake();
        }
    }

    @Override
    protected VoxelShape makeShape(BlockState state) {
        VoxelShape shape = Shapes.cube(5, 11);
        for (Direction side : Direction.values()) {
            if (mode(state, side) != MoverSide.OFF) {
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, Shapes.along(side, 11, 16, 6, 10));
            }
        }
        return shape;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation) {
        return Shapes.turnSides(state, SIDES, rotation::rotate);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return Shapes.turnSides(state, SIDES, mirror::mirror);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIDES);
    }
}
