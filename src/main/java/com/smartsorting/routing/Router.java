package com.smartsorting.routing;

import com.smartsorting.tile.TileImporter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Router {

    private Router() {
    }

    public record Target(BlockPos importer, BlockPos destination, int accepted) {
    }

    public static boolean hasImporters(Level level, BlockPos exporter) {
        return !NetworkCache.importersFrom(level, exporter).isEmpty();
    }

    @Nullable
    public static Target find(Level level, BlockPos exporter, ItemStack stack, BlockPos exporterInventory,
                              boolean roundRobin, int offset) {
        for (TileImporter importer : order(level, exporter, roundRobin, offset)) {
            if (exporterInventory.equals(importer.getDestinationPos()) || !importer.accepts(stack)) {
                continue;
            }
            IItemHandler handler = importer.getDestinationHandler();
            if (handler == null) {
                continue;
            }
            int accepted = stack.getCount() - ItemHandlerHelper.insertItemStacked(handler, stack, true).getCount();
            if (accepted > 0) {
                return new Target(importer.getBlockPos(), importer.getDestinationPos(), accepted);
            }
        }
        return null;
    }

    private static List<TileImporter> order(Level level, BlockPos exporter, boolean roundRobin, int offset) {
        List<TileImporter> sorted = new ArrayList<>();
        for (BlockPos pos : NetworkCache.importersFrom(level, exporter)) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof TileImporter importer) {
                sorted.add(importer);
            }
        }

        sorted.sort(Comparator.comparingInt(TileImporter::getPriority).reversed());
        if (!roundRobin || sorted.size() < 2) {
            return sorted;
        }
        List<TileImporter> result = new ArrayList<>(sorted.size());
        int start = 0;
        while (start < sorted.size()) {
            int priority = sorted.get(start).getPriority();
            int end = start;
            while (end < sorted.size() && sorted.get(end).getPriority() == priority) {
                end++;
            }
            int size = end - start;
            for (int i = 0; i < size; i++) {
                result.add(sorted.get(start + Math.floorMod(offset + i, size)));
            }
            start = end;
        }
        return result;
    }
}
