package com.smartsorting.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class TileBase extends BlockEntity implements MenuProvider {

    protected TileBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract void writeSettings(CompoundTag tag);

    protected abstract void readSettings(CompoundTag tag);

    protected void writeSaveData(CompoundTag tag) {
    }

    protected void readSaveData(CompoundTag tag) {
    }

    public void onButton(ServerPlayer player, int id, int value) {
    }

    protected void syncSettings() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeSettings(tag);
        writeSaveData(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readSaveData(tag);
        readSettings(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeSettings(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        readSettings(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            readSettings(packet.getTag());
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public boolean isUsableBy(Player player) {
        return !isRemoved()
                && level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
