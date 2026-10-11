package com.smartsorting.gui;

import com.smartsorting.tile.TileBase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

public abstract class ContainerBase<T extends TileBase> extends AbstractContainerMenu {

    protected final T tile;

    protected ContainerBase(MenuType<?> type, int id, T tile) {
        super(type, id);
        this.tile = tile;
    }

    protected static <T extends TileBase> T clientTile(Inventory inventory, FriendlyByteBuf data, Class<T> type) {
        var tile = inventory.player.level().getBlockEntity(data.readBlockPos());
        if (!type.isInstance(tile)) {
            throw new IllegalStateException("Expected " + type.getSimpleName() + " but found " + tile);
        }
        return type.cast(tile);
    }

    public T getTile() {
        return tile;
    }

    protected void addPlayerInventory(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, x + col * 18, y + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return tile.isUsableBy(player);
    }
}
