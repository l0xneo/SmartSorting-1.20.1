package com.smartsorting.net;

import com.smartsorting.tile.TileBase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record MsgButton(BlockPos pos, int id, int value) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(id);
        buf.writeInt(value);
    }

    public static MsgButton decode(FriendlyByteBuf buf) {
        return new MsgButton(buf.readBlockPos(), buf.readByte(), buf.readInt());
    }

    public void handle(ServerPlayer player) {
        TileBase tile = PacketHandler.openTile(player, pos, TileBase.class);
        if (tile != null) {
            tile.onButton(player, id, value);
        }
    }
}
