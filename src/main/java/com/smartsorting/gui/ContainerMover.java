package com.smartsorting.gui;

import com.smartsorting.registry.ModMenus;
import com.smartsorting.tile.TileChestMover;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerMover extends ContainerBase<TileChestMover> {

    public static final int UPGRADE_X = 9;
    public static final int UPGRADE_Y = 115;
    public static final int PLAYER_X = 39;
    public static final int PLAYER_Y = 142;

    public ContainerMover(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, clientTile(playerInventory, data, TileChestMover.class));
    }

    public ContainerMover(int id, Inventory playerInventory, TileChestMover tile) {
        super(ModMenus.MOVER.get(), id, tile);
        addSlot(new SlotItemHandler(tile.getUpgrades(), 0, UPGRADE_X, UPGRADE_Y));
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
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, end, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            int hotbarStart = end - 9;
            boolean moved = index < hotbarStart
                    ? moveItemStackTo(stack, hotbarStart, end, false)
                    : moveItemStackTo(stack, 1, hotbarStart, false);
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }
}
