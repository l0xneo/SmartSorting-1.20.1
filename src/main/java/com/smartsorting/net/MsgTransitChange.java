package com.smartsorting.net;

import com.smartsorting.client.TransitRenderer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record MsgTransitChange(List<Change> changes, List<Integer> removed) {

    public record Change(int id, float progress, float speed, float end) {
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeCollection(changes, (b, c) -> {
            b.writeVarInt(c.id);
            b.writeFloat(c.progress);
            b.writeFloat(c.speed);
            b.writeFloat(c.end);
        });
        buf.writeCollection(removed, FriendlyByteBuf::writeVarInt);
    }

    public static MsgTransitChange decode(FriendlyByteBuf buf) {
        List<Change> changes = buf.readCollection(ArrayList::new, b -> new Change(b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat()));
        return new MsgTransitChange(changes, buf.readCollection(ArrayList::new, FriendlyByteBuf::readVarInt));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TransitRenderer.change(this)));
        context.get().setPacketHandled(true);
    }
}
