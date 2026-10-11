package com.smartsorting.routing;

import com.smartsorting.block.BlockAttachment;
import com.smartsorting.block.BlockPipe;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class Connections {

    private Connections() {
    }

    public static boolean pipeConnects(BlockState neighbour, Direction side) {
        Block block = neighbour.getBlock();
        if (block instanceof BlockPipe) {
            return true;
        }
        return block instanceof BlockAttachment && BlockAttachment.connectsOn(neighbour, side.getOpposite());
    }
}
