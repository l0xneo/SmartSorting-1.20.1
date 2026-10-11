package com.smartsorting.net;

import com.smartsorting.tile.TileImporter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record MsgSetModFilter(BlockPos pos, String modId) {

    private static final int MAX_LENGTH = 64;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(modId, MAX_LENGTH);
    }

    public static MsgSetModFilter decode(FriendlyByteBuf buf) {
        return new MsgSetModFilter(buf.readBlockPos(), buf.readUtf(MAX_LENGTH));
    }

    public void handle(ServerPlayer player) {
        TileImporter tile = PacketHandler.openTile(player, pos, TileImporter.class);
        if (tile != null) {
            tile.getFilter().setModId(modId);
        }
    }
}
