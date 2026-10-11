package com.smartsorting.block;

import com.smartsorting.tile.TileImporter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockImporter extends BlockAttachment {

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileImporter(pos, state);
    }
}
