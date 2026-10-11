package com.smartsorting.net;

import com.smartsorting.tile.TileImporter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record MsgSetGhost(BlockPos pos, int slot, ItemStack stack) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(slot);
        buf.writeItem(stack);
    }

    public static MsgSetGhost decode(FriendlyByteBuf buf) {
        return new MsgSetGhost(buf.readBlockPos(), buf.readVarInt(), buf.readItem());
    }

    public void handle(ServerPlayer player) {
        TileImporter tile = PacketHandler.openTile(player, pos, TileImporter.class);
        if (tile != null) {
            tile.getFilter().setGhost(slot, stack);
        }
    }
}
