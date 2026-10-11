package com.smartsorting.net;

import com.smartsorting.gui.ContainerStorage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record MsgStorageView(int offset, String search) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(offset);
        buf.writeUtf(search, ContainerStorage.MAX_SEARCH);
    }

    public static MsgStorageView decode(FriendlyByteBuf buf) {
        return new MsgStorageView(buf.readVarInt(), buf.readUtf(ContainerStorage.MAX_SEARCH));
    }

    public void handle(ServerPlayer player) {
        if (player.containerMenu instanceof ContainerStorage menu) {
            menu.setView(offset, search);
        }
    }
}
