package com.smartsorting.transport;

import com.smartsorting.routing.NetworkCache;
import com.smartsorting.routing.Router;
import com.smartsorting.tile.ExporterStatus;
import com.smartsorting.tile.TileExporter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public final class ItemMover {

    private ItemMover() {
    }

    public static ExporterStatus transfer(TileExporter exporter) {
        if (!(exporter.getLevel() instanceof ServerLevel level)) {
            return ExporterStatus.NO_DESTINATION;
        }
        BlockPos exporterPos = exporter.getBlockPos();
        TransitManager transit = TransitManager.get(level);
        if (!Router.hasImporters(level, exporterPos)) {
            return ExporterStatus.NO_DESTINATION;
        }
        IItemHandler inventory = exporter.getSourceHandler();
        if (inventory == null) {
            return ExporterStatus.NO_SOURCE;
        }
        BlockPos inventoryPos = exporter.getSourcePos();
        int perTransfer = exporter.getItemsPerTransfer();
        boolean hadItems = false;

        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack offered = inventory.extractItem(slot, perTransfer, true);
            if (offered.isEmpty()) {
                continue;
            }
            hadItems = true;
            Router.Target target = Router.find(level, exporterPos, offered, inventoryPos,
                    exporter.isRoundRobin(), exporter.getRoundRobinOffset());
            if (target == null) {
                continue;
            }
            List<BlockPos> path = new ArrayList<>(NetworkCache.pathTo(level, exporterPos, target.importer()));
            path.add(target.destination());
            if (path.size() < 3) {
                continue;
            }
            if (!transit.canLaunch(path)) {
                return ExporterStatus.PIPE_FULL;
            }
            ItemStack moving = inventory.extractItem(slot, target.accepted(), false);
            if (moving.isEmpty()) {
                continue;
            }
            exporter.advanceRoundRobin();
            transit.launch(level, exporterPos, moving, path, exporter.getTravelSpeed());
            return ExporterStatus.WORKING;
        }
        return hadItems ? ExporterStatus.BLOCKED : ExporterStatus.SOURCE_EMPTY;
    }
}
