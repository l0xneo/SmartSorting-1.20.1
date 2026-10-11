package com.smartsorting.net;

import com.smartsorting.block.BlockChestLink;
import com.smartsorting.block.BlockChestMover;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public record MsgLinkSide(BlockPos pos, int side) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(side);
    }

    public static MsgLinkSide decode(FriendlyByteBuf buf) {
        return new MsgLinkSide(buf.readBlockPos(), buf.readByte());
    }

    public void handle(ServerPlayer player) {
        if (side < 0 || side >= 6 || player.distanceToSqr(pos.getCenter()) > 64 || !player.mayBuild()
                || !player.level().isLoaded(pos)) {
            return;
        }
        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof BlockChestLink) {
            player.level().setBlock(pos, state.cycle(BlockChestLink.SIDES[side]), 3);
        } else if (state.getBlock() instanceof BlockChestMover) {
            player.level().setBlock(pos, state.cycle(BlockChestMover.SIDES[side]), 3);
        }
    }
}
