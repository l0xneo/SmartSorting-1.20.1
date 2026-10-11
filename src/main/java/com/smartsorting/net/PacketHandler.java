package com.smartsorting.net;

import com.smartsorting.SmartSorting;
import com.smartsorting.gui.ContainerBase;
import com.smartsorting.tile.TileBase;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class PacketHandler {

    private static final String VERSION = "6";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SmartSorting.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private PacketHandler() {
    }

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, MsgButton.class, MsgButton::encode, MsgButton::decode,
                onServer(MsgButton::handle), Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, MsgSetModFilter.class, MsgSetModFilter::encode, MsgSetModFilter::decode,
                onServer(MsgSetModFilter::handle), Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, MsgSetGhost.class, MsgSetGhost::encode, MsgSetGhost::decode,
                onServer(MsgSetGhost::handle), Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, MsgStorageView.class, MsgStorageView::encode, MsgStorageView::decode,
                onServer(MsgStorageView::handle), Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, MsgLinkSide.class, MsgLinkSide::encode, MsgLinkSide::decode,
                onServer(MsgLinkSide::handle), Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, MsgTransitAdd.class, MsgTransitAdd::encode, MsgTransitAdd::decode,
                MsgTransitAdd::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, MsgTransitChange.class, MsgTransitChange::encode, MsgTransitChange::decode,
                MsgTransitChange::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static <M> BiConsumer<M, Supplier<NetworkEvent.Context>> onServer(BiConsumer<M, ServerPlayer> handler) {
        return (message, context) -> {
            NetworkEvent.Context ctx = context.get();
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ctx.enqueueWork(() -> handler.accept(message, player));
            }
            ctx.setPacketHandled(true);
        };
    }

    @Nullable
    static <T extends TileBase> T openTile(ServerPlayer player, BlockPos pos, Class<T> type) {
        if (!(player.containerMenu instanceof ContainerBase<?> menu)) {
            return null;
        }
        TileBase tile = menu.getTile();
        if (!tile.getBlockPos().equals(pos) || !tile.isUsableBy(player) || !type.isInstance(tile)) {
            return null;
        }
        return type.cast(tile);
    }
}
