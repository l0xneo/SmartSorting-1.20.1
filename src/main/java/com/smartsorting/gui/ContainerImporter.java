package com.smartsorting.gui;

import com.smartsorting.filter.ItemFilter;
import com.smartsorting.registry.ModMenus;
import com.smartsorting.tile.TileImporter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public class ContainerImporter extends ContainerBase<TileImporter> {

    public static final int GHOST_X = 8;
    public static final int GHOST_Y = 26;
    public static final int PLAYER_X = 8;
    public static final int PLAYER_Y = 140;

    public ContainerImporter(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, clientTile(playerInventory, data, TileImporter.class));
    }

    public ContainerImporter(int id, Inventory playerInventory, TileImporter tile) {
        super(ModMenus.IMPORTER.get(), id, tile);
        for (int i = 0; i < ItemFilter.SLOTS; i++) {
            int onPage = i % ItemFilter.PAGE_SIZE;
            addSlot(new SlotGhost(tile.getFilter().getGhosts(), i, GHOST_X + (onPage % 9) * 18, GHOST_Y + (onPage / 9) * 18));
        }
        addPlayerInventory(playerInventory, PLAYER_X, PLAYER_Y);
    }

    private static boolean isGhost(int slotId) {
        return slotId >= 0 && slotId < ItemFilter.SLOTS;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!isGhost(slotId)) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        ItemStack held = getCarried();
        Slot slot = slots.get(slotId);
        if (clickType == ClickType.PICKUP) {
            slot.set(held.isEmpty() ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(held, 1));
        } else if (clickType == ClickType.QUICK_MOVE) {
            slot.set(ItemStack.EMPTY);
        }

    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!isGhost(index) && index < slots.size()) {
            Slot slot = slots.get(index);
            if (slot.hasItem()) {
                tile.getFilter().addGhost(slot.getItem());
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot instanceof SlotGhost);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof SlotGhost);
    }
}
