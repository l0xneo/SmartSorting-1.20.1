package com.smartsorting.gui;

import com.smartsorting.registry.ModMenus;
import com.smartsorting.storage.StorageNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public class ContainerStorage extends AbstractContainerMenu {

    public static final int COLS = 9;
    public static final int ROWS = 6;
    public static final int WINDOW = COLS * ROWS;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 32;
    public static final int PLAYER_Y = 156;
    public static final int MAX_SEARCH = 50;

    private final BlockPos origin;
    private final Level level;
    @Nullable
    private final View view;
    private int rows;
    private int offset;
    private String search = "";

    public ContainerStorage(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    public ContainerStorage(int id, Inventory inventory, BlockPos origin) {
        super(ModMenus.STORAGE.get(), id);
        this.origin = origin;
        this.level = inventory.player.level();
        this.view = level.isClientSide ? null : new View();
        IItemHandlerModifiable window = view != null ? view : new ItemStackHandler(WINDOW);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new SlotItemHandler(window, col + row * COLS, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, GRID_X + col * 18, PLAYER_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, GRID_X + col * 18, PLAYER_Y + 58));
        }
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return rows;
            }

            @Override
            public void set(int value) {
                rows = value;
            }
        });
        if (view != null) {
            view.rebuild();
        }
    }

    public int getRows() {
        return rows;
    }

    public void setView(int offset, String search) {
        if (view != null) {
            String text = search.length() > MAX_SEARCH ? search.substring(0, MAX_SEARCH) : search;
            this.offset = offset;
            if (!text.equals(this.search)) {
                this.search = text;
                view.rebuild();
            } else {
                view.clampOffset();
            }
        }
    }

    public List<ChestBlockEntity> getChests() {
        return StorageNetwork.chests(level, origin);
    }

    public void refresh() {
        if (view != null) {
            view.rebuild();
        }
    }

    @Override
    public void broadcastChanges() {

        if (view != null && view.chests.stream().anyMatch(BlockEntity::isRemoved)) {
            view.rebuild();
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5) <= 64
                && (StorageNetwork.isLink(level, origin) || StorageNetwork.isLinkedChest(level, origin));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < WINDOW) {
            if (!moveItemStackTo(stack, WINDOW, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (view == null) {
                return ItemStack.EMPTY;
            }
            ItemStack left = ItemHandlerHelper.insertItemStacked(view.all, stack.copy(), false);
            if (left.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            stack.setCount(left.getCount());
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private final class View implements IItemHandlerModifiable {

        List<ChestBlockEntity> chests = new ArrayList<>();
        IItemHandlerModifiable all = new CombinedInvWrapper();
        int[] shown = new int[0];

        void rebuild() {
            chests = new ArrayList<>(StorageNetwork.chests(level, origin));

            chests.sort(Comparator.comparingInt((ChestBlockEntity chest) -> -itemCount(chest)));
            all = new CombinedInvWrapper(chests.stream().map(InvWrapper::new).toArray(IItemHandlerModifiable[]::new));
            String query = search.trim().toLowerCase(Locale.ROOT);
            shown = IntStream.range(0, all.getSlots())
                    .filter(i -> query.isEmpty() || matches(all.getStackInSlot(i), query)).toArray();
            rows = (shown.length + COLS - 1) / COLS;
            clampOffset();
        }

        void clampOffset() {
            offset = Math.max(0, Math.min(offset, rows - ROWS));
        }

        private static int itemCount(ChestBlockEntity chest) {
            int count = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                count += chest.getItem(i).getCount();
            }
            return count;
        }

        private static boolean matches(ItemStack stack, String query) {
            if (stack.isEmpty()) {
                return false;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                    || (id != null && id.toString().contains(query));
        }

        private int index(int slot) {
            int i = offset * COLS + slot;
            return i < shown.length ? shown[i] : -1;
        }

        @Override
        public int getSlots() {
            return WINDOW;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            int i = index(slot);
            return i < 0 ? ItemStack.EMPTY : all.getStackInSlot(i);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int i = index(slot);
            return i < 0 ? stack : all.insertItem(i, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int i = index(slot);
            return i < 0 ? ItemStack.EMPTY : all.extractItem(i, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            int i = index(slot);
            return i < 0 ? 0 : all.getSlotLimit(i);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            int i = index(slot);
            return i >= 0 && all.isItemValid(i, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            int i = index(slot);
            if (i >= 0) {
                all.setStackInSlot(i, stack);
            }
        }
    }
}
