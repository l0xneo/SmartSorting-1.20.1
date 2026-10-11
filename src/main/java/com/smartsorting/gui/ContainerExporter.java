package com.smartsorting.gui;

import com.smartsorting.registry.ModMenus;
import com.smartsorting.tile.ExporterStatus;
import com.smartsorting.tile.TileExporter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerExporter extends ContainerBase<TileExporter> {

    public static final int SPEED_X = 116;
    public static final int STACK_X = 134;
    public static final int TRANSIT_X = 152;
    public static final int UPGRADE_Y = 40;
    public static final int PLAYER_Y = 84;

    private static final int UPGRADE_SLOTS = TileExporter.UPGRADE_SLOTS;

    private ExporterStatus clientStatus = ExporterStatus.SOURCE_EMPTY;

    public ContainerExporter(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, clientTile(playerInventory, data, TileExporter.class));
    }

    public ContainerExporter(int id, Inventory playerInventory, TileExporter tile) {
        super(ModMenus.EXPORTER.get(), id, tile);
        addSlot(new SlotItemHandler(tile.getUpgrades(), TileExporter.SLOT_SPEED, SPEED_X, UPGRADE_Y));
        addSlot(new SlotItemHandler(tile.getUpgrades(), TileExporter.SLOT_STACK, STACK_X, UPGRADE_Y));
        addSlot(new SlotItemHandler(tile.getUpgrades(), TileExporter.SLOT_TRANSIT, TRANSIT_X, UPGRADE_Y));
        addPlayerInventory(playerInventory, 8, PLAYER_Y);
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return tile.getStatus().ordinal();
            }

            @Override
            public void set(int value) {
                clientStatus = ExporterStatus.fromOrdinal(value);
            }
        });
    }

    public ExporterStatus getClientStatus() {
        return clientStatus;
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
        if (index < UPGRADE_SLOTS) {
            if (!moveItemStackTo(stack, UPGRADE_SLOTS, end, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, UPGRADE_SLOTS, false)) {
            int hotbarStart = end - 9;
            boolean moved = index < hotbarStart
                    ? moveItemStackTo(stack, hotbarStart, end, false)
                    : moveItemStackTo(stack, UPGRADE_SLOTS, hotbarStart, false);
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
