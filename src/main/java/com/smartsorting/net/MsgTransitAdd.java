package com.smartsorting.net;

import com.smartsorting.client.TransitRenderer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record MsgTransitAdd(boolean reset, List<Data> items) {

    private static final int MAX_PATH = 256;

    public record Data(int id, ItemStack stack, List<BlockPos> path, float progress, float speed, float end) {
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(reset);
        List<Data> shown = items.stream().filter(d -> d.path.size() <= MAX_PATH).toList();
        buf.writeCollection(shown, (b, d) -> {
            b.writeVarInt(d.id);
            b.writeItem(d.stack.copyWithCount(1));
            b.writeCollection(d.path, FriendlyByteBuf::writeBlockPos);
            b.writeFloat(d.progress);
            b.writeFloat(d.speed);
            b.writeFloat(d.end);
        });
    }

    public static MsgTransitAdd decode(FriendlyByteBuf buf) {
        boolean reset = buf.readBoolean();
        List<Data> items = buf.readCollection(ArrayList::new, b -> new Data(b.readVarInt(), b.readItem(),
                b.readCollection(ArrayList::new, FriendlyByteBuf::readBlockPos), b.readFloat(), b.readFloat(), b.readFloat()));
        items.removeIf(d -> d.path.size() < 2 || d.path.size() > MAX_PATH);
        return new MsgTransitAdd(reset, items);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TransitRenderer.add(this)));
        context.get().setPacketHandled(true);
    }
}
