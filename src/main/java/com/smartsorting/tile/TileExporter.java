package com.smartsorting.tile;

import com.smartsorting.ModConfig;
import com.smartsorting.block.BlockAttachment;
import com.smartsorting.block.BlockExporter;
import com.smartsorting.gui.ContainerExporter;
import com.smartsorting.registry.ModBlocks;
import com.smartsorting.registry.ModItems;
import com.smartsorting.transport.InventoryAccess;
import com.smartsorting.transport.ItemMover;
import com.smartsorting.transport.TransitManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

public class TileExporter extends TileBase implements IHasContents {

    public static final int BTN_REDSTONE = 0;
    public static final int BTN_ORDER = 1;

    public static final int SLOT_SPEED = 0;
    public static final int SLOT_STACK = 1;
    public static final int SLOT_TRANSIT = 2;
    public static final int UPGRADE_SLOTS = 3;

    public static final float TRAVEL_SPEED = 0.0375F;
    public static final int MAX_UPGRADES = 1;

    private RedstoneMode redstone = RedstoneMode.ALWAYS;
    private boolean roundRobin;

    private final ItemStackHandler upgrades = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return MAX_UPGRADES;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(switch (slot) {
                case SLOT_SPEED -> ModItems.SPEED_UPGRADE.get();
                case SLOT_STACK -> ModItems.STACK_UPGRADE.get();
                default -> ModItems.TRANSIT_UPGRADE.get();
            });
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int cooldown;
    private int idleDelay;
    private ExporterStatus status = ExporterStatus.SOURCE_EMPTY;
    private int roundRobinOffset;

    @Nullable
    private BlockEntity sourceTile;

    public TileExporter(BlockPos pos, BlockState state) {
        super(ModBlocks.EXPORTER_TILE.get(), pos, state);
    }



    public void tick() {
        if (TransitManager.get((ServerLevel) level).hasStuck(worldPosition)) {
            status = ExporterStatus.STUCK;
            return;
        }
        if (--cooldown > 0) {
            return;
        }
        int backoffMax = ModConfig.IDLE_BACKOFF_MAX.get();
        if (!redstone.allows(level.hasNeighborSignal(worldPosition))) {
            status = ExporterStatus.DISABLED;
            cooldown = backoffMax;
            return;
        }
        status = ItemMover.transfer(this);
        if (status == ExporterStatus.WORKING || status == ExporterStatus.PIPE_FULL) {
            idleDelay = 0;
            cooldown = getInterval();
        } else {
            idleDelay = Math.min(Math.max(idleDelay * 2, getInterval()), Math.max(backoffMax, getInterval()));
            cooldown = idleDelay;
        }
    }

    public void wake() {
        cooldown = 0;
        idleDelay = 0;
        sourceTile = null;
    }

    public ExporterStatus getStatus() {
        return status;
    }



    public ItemStackHandler getUpgrades() {
        return upgrades;
    }

    public int getInterval() {
        return Math.max(1, (int) Math.ceil(TransitManager.SPACING / getTravelSpeed() - 1e-4));
    }

    public float getTravelSpeed() {
        boolean speed = !upgrades.getStackInSlot(SLOT_SPEED).isEmpty();
        boolean transit = !upgrades.getStackInSlot(SLOT_TRANSIT).isEmpty();

        return speed && transit ? 0.4F : transit ? 0.2F : speed ? 0.1F : TRAVEL_SPEED;
    }

    public int getItemsPerTransfer() {
        return Math.min(64, ModConfig.ITEMS_PER_TRANSFER.get() << (2 * upgrades.getStackInSlot(SLOT_STACK).getCount()));
    }

    @Override
    public void takeContents(List<ItemStack> out) {
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (!stack.isEmpty()) {
                out.add(stack.copy());
                upgrades.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }



    public RedstoneMode getRedstoneMode() {
        return redstone;
    }

    public boolean isRoundRobin() {
        return roundRobin;
    }

    public int getRoundRobinOffset() {
        return roundRobinOffset;
    }

    public void advanceRoundRobin() {
        if (roundRobin) {
            roundRobinOffset++;
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerExporter(id, inventory, this);
    }

    @Override
    public void onButton(ServerPlayer player, int id, int value) {
        if (id == BTN_REDSTONE) {
            redstone = redstone.cycle(value != 0);
        } else if (id == BTN_ORDER) {
            roundRobin = !roundRobin;
        } else {
            return;
        }
        wake();
        syncSettings();
    }



    @Nullable
    private Direction getNetworkSide() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof BlockExporter ? BlockAttachment.getFacing(state) : null;
    }

    public BlockPos getSourcePos() {
        Direction side = getNetworkSide();
        return side == null ? worldPosition : worldPosition.relative(side.getOpposite());
    }

    @Nullable
    public IItemHandler getSourceHandler() {
        Direction side = getNetworkSide();
        if (side == null) {
            return null;
        }
        BlockPos source = worldPosition.relative(side.getOpposite());
        if (sourceTile == null || sourceTile.isRemoved() || !sourceTile.getBlockPos().equals(source)) {
            sourceTile = level.isLoaded(source) ? level.getBlockEntity(source) : null;
        }

        return sourceTile instanceof TileBase ? null : InventoryAccess.getHandler(sourceTile, side);
    }



    @Override
    protected void writeSettings(CompoundTag tag) {
        tag.putInt("Redstone", redstone.ordinal());
        tag.putBoolean("RoundRobin", roundRobin);
    }

    @Override
    protected void readSettings(CompoundTag tag) {
        redstone = RedstoneMode.fromOrdinal(tag.getInt("Redstone"));
        roundRobin = tag.getBoolean("RoundRobin");
    }

    @Override
    protected void writeSaveData(CompoundTag tag) {
        tag.put("Upgrades", upgrades.serializeNBT());
    }

    @Override
    protected void readSaveData(CompoundTag tag) {
        if (tag.contains("Upgrades")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(tag.getCompound("Upgrades"));
            for (int i = 0; i < upgrades.getSlots(); i++) {
                upgrades.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
    }
}
