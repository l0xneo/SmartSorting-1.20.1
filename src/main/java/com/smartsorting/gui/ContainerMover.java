package com.smartsorting.gui;

import com.smartsorting.registry.ModMenus;
import com.smartsorting.tile.TileChestMover;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ContainerMover extends ContainerBase<TileChestMover> {

    public static final int PLAYER_X = 39;
    public static final int PLAYER_Y = 116;

    public ContainerMover(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, clientTile(playerInventory, data, TileChestMover.class));
    }

    public ContainerMover(int id, Inventory playerInventory, TileChestMover tile) {
        super(ModMenus.MOVER.get(), id, tile);
        addPlayerInventory(playerInventory, PLAYER_X, PLAYER_Y);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int end = slots.size();
        int hotbarStart = end - 9;
        boolean moved = index < hotbarStart
                ? moveItemStackTo(stack, hotbarStart, end, false)
                : moveItemStackTo(stack, 0, hotbarStart, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
