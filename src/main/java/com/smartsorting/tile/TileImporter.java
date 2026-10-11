package com.smartsorting.tile;

import com.smartsorting.block.BlockAttachment;
import com.smartsorting.block.BlockImporter;
import com.smartsorting.filter.ItemFilter;
import com.smartsorting.gui.ContainerImporter;
import com.smartsorting.registry.ModBlocks;
import com.smartsorting.transport.InventoryAccess;
import com.smartsorting.transport.TransitManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public class TileImporter extends TileBase {

    public static final int BTN_METHOD = 0;
    public static final int BTN_INVERT = 1;
    public static final int BTN_META = 2;
    public static final int BTN_NBT = 3;
    public static final int BTN_ORE = 4;
    public static final int BTN_PRIORITY = 5;
    public static final int BTN_CLEAR_GHOSTS = 6;
    public static final int BTN_ENABLED = 7;

    public static final int MAX_PRIORITY = 99;

    private final ItemFilter filter = new ItemFilter(this::syncSettings);
    private int priority;
    private boolean enabled = true;

    @Nullable
    private BlockEntity destinationTile;

    public TileImporter(BlockPos pos, BlockState state) {
        super(ModBlocks.IMPORTER_TILE.get(), pos, state);
    }

    public ItemFilter getFilter() {
        return filter;
    }

    public boolean accepts(ItemStack stack) {
        return enabled && filter.allows(stack);
    }

    public int getPriority() {
        return priority;
    }

    @Nullable
    private Direction getNetworkSide() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof BlockImporter ? BlockAttachment.getFacing(state) : null;
    }

    @Nullable
    public BlockPos getDestinationPos() {
        Direction side = getNetworkSide();
        return side == null ? null : worldPosition.relative(side.getOpposite());
    }

    @Nullable
    public IItemHandler getDestinationHandler() {
        Direction side = getNetworkSide();
        if (side == null) {
            return null;
        }
        BlockPos destination = worldPosition.relative(side.getOpposite());
        if (destinationTile == null || destinationTile.isRemoved() || !destinationTile.getBlockPos().equals(destination)) {
            destinationTile = level.isLoaded(destination) ? level.getBlockEntity(destination) : null;
        }
        return InventoryAccess.getHandler(destinationTile, side);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerImporter(id, inventory, this);
    }

    @Override
    protected void syncSettings() {
        super.syncSettings();
        if (level instanceof ServerLevel server) {
            TransitManager.get(server).onImporterChanged(server, this);
        }
    }

    @Override
    public void onButton(ServerPlayer player, int id, int value) {
        switch (id) {
            case BTN_METHOD -> filter.setMethod(value);
            case BTN_INVERT -> filter.toggleInvert();
            case BTN_META -> filter.toggleMatchMeta();
            case BTN_NBT -> filter.toggleMatchNbt();
            case BTN_ORE -> filter.toggleOreDict();
            case BTN_CLEAR_GHOSTS -> filter.clearGhosts();
            case BTN_ENABLED -> {
                enabled = !enabled;
                syncSettings();
            }
            case BTN_PRIORITY -> {
                priority = Mth.clamp(priority + value, -MAX_PRIORITY, MAX_PRIORITY);
                syncSettings();
            }
            default -> {
            }
        }
    }

    @Override
    protected void writeSettings(CompoundTag tag) {
        filter.write(tag);
        tag.putInt("Priority", priority);
        tag.putBoolean("Disabled", !enabled);
    }

    @Override
    protected void readSettings(CompoundTag tag) {
        filter.read(tag);
        priority = Mth.clamp(tag.getInt("Priority"), -MAX_PRIORITY, MAX_PRIORITY);
        enabled = !tag.getBoolean("Disabled");
    }

    public boolean isEnabled() {
        return enabled;
    }
}
