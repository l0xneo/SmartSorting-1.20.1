package com.smartsorting.block;

import com.smartsorting.tile.TileExporter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockExporter extends BlockAttachment {

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileExporter(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, tile) -> {
            if (tile instanceof TileExporter exporter) {
                exporter.tick();
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
        if (level.getBlockEntity(pos) instanceof TileExporter exporter) {
            exporter.wake();
        }
    }
}
