package com.smartsorting.tile;

import com.smartsorting.ModConfig;
import com.smartsorting.block.BlockChestMover;
import com.smartsorting.block.MoverSide;
import com.smartsorting.gui.ContainerMover;
import com.smartsorting.item.ItemUpgrade;
import com.smartsorting.registry.ModBlocks;
import com.smartsorting.registry.ModItems;
import com.smartsorting.transport.InventoryAccess;
import com.smartsorting.transport.TransitManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public class TileChestMover extends TileBase implements IHasContents {

    private static final int IDLE_TICKS = 20;
    public static final float CHEST_DEPTH = 0.35F;

    private final ItemStackHandler upgrades = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof ItemUpgrade;
        }

        @Override
        protected void onContentsChanged(int slot) {
            cooldown = 0;
            setChanged();
        }
    };
    private int cooldown;

    public TileChestMover(BlockPos pos, BlockState state) {
        super(ModBlocks.MOVER_TILE.get(), pos, state);
    }



    public ItemStackHandler getUpgrades() {
        return upgrades;
    }

    private ItemStack upgrade() {
        return upgrades.getStackInSlot(0);
    }

    public static float travelSpeed(ItemStack upgrade) {
        return upgrade.is(ModItems.TRANSIT_UPGRADE.get()) ? 0.2F : upgrade.is(ModItems.SPEED_UPGRADE.get()) ? 0.1F : TileExporter.TRAVEL_SPEED;
    }

    public float getTravelSpeed() {
        return travelSpeed(upgrade());
    }

    public int getItemsPerTransfer() {
        int base = ModConfig.ITEMS_PER_TRANSFER.get();
        return upgrade().is(ModItems.STACK_UPGRADE.get()) ? Math.min(64, base * 4) : base;
    }

    public int getInterval() {
        return Math.max(1, (int) Math.ceil(TransitManager.SPACING / getTravelSpeed() - 1e-4));
    }

    @Override
    public void takeContents(List<ItemStack> out) {
        if (!upgrade().isEmpty()) {
            out.add(upgrade().copy());
            upgrades.setStackInSlot(0, ItemStack.EMPTY);
        }
    }



    public void wake() {
        cooldown = 0;
    }

    public void tick() {
        if (--cooldown > 0) {
            return;
        }
        TransitManager transit = TransitManager.get((ServerLevel) level);
        if (transit.hasStuck(worldPosition)) {
            cooldown = IDLE_TICKS;
            return;
        }
        cooldown = moveOne(transit) ? getInterval() : IDLE_TICKS;
    }

    private boolean moveOne(TransitManager transit) {
        BlockState state = getBlockState();
        for (Direction from : Direction.values()) {
            if (BlockChestMover.mode(state, from) != MoverSide.TAKE) {
                continue;
            }
            BlockPos fromPos = worldPosition.relative(from);
            IItemHandler source = InventoryAccess.getHandler(level.getBlockEntity(fromPos), from.getOpposite());
            if (source == null) {
                continue;
            }
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack peek = source.extractItem(slot, getItemsPerTransfer(), true);
                if (peek.isEmpty()) {
                    continue;
                }

                for (Direction to : Direction.values()) {
                    if (BlockChestMover.mode(state, to) != MoverSide.GIVE) {
                        continue;
                    }
                    BlockPos toPos = worldPosition.relative(to);
                    IItemHandler dest = InventoryAccess.getHandler(level.getBlockEntity(toPos), to.getOpposite());
                    if (dest == null || sameChest(fromPos, toPos)) {
                        continue;
                    }
                    int fits = peek.getCount() - ItemHandlerHelper.insertItemStacked(dest, peek, true).getCount();
                    if (fits <= 0) {
                        continue;
                    }
                    List<BlockPos> path = List.of(fromPos, worldPosition, toPos);
                    if (!transit.canLaunch(path, CHEST_DEPTH)) {
                        return true;
                    }
                    ItemStack moving = source.extractItem(slot, fits, false);
                    if (!moving.isEmpty()) {
                        transit.launch((ServerLevel) level, worldPosition, moving, path, getTravelSpeed(), CHEST_DEPTH);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean sameChest(BlockPos a, BlockPos b) {
        BlockState state = level.getBlockState(a);
        return state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && a.relative(ChestBlock.getConnectedDirection(state)).equals(b);
    }



    @Override
    protected void writeSettings(CompoundTag tag) {
    }

    @Override
    protected void readSettings(CompoundTag tag) {
    }

    @Override
    protected void writeSaveData(CompoundTag tag) {
        tag.put("Upgrades", upgrades.serializeNBT());
    }

    @Override
    protected void readSaveData(CompoundTag tag) {
        if (tag.contains("Upgrades")) {
            upgrades.deserializeNBT(tag.getCompound("Upgrades"));
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerMover(id, inventory, this);
    }
}
