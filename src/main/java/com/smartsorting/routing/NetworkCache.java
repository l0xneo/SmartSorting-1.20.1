package com.smartsorting.routing;

import com.smartsorting.ModConfig;
import com.smartsorting.block.BlockAttachment;
import com.smartsorting.block.BlockImporter;
import com.smartsorting.block.BlockPipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class NetworkCache {

    private static final int INCOMPLETE_RETRY_TICKS = 100;

    private static final Map<Level, LevelCache> CACHES = new WeakHashMap<>();

    private NetworkCache() {
    }

    private static final class LevelCache {
        long version = -1;
        final Map<BlockPos, Entry> entries = new HashMap<>();
    }

    private record Entry(List<BlockPos> importers, Map<BlockPos, BlockPos> parents, boolean incomplete, long builtAt) {
    }

    public static List<BlockPos> importersFrom(Level level, BlockPos source) {
        return entry(level, source).importers;
    }

    public static List<BlockPos> pathTo(Level level, BlockPos source, BlockPos importer) {
        Map<BlockPos, BlockPos> parents = entry(level, source).parents;
        List<BlockPos> path = new ArrayList<>();
        for (BlockPos pos = importer; pos != null; pos = parents.get(pos)) {
            path.add(pos);
        }
        Collections.reverse(path);
        return path;
    }

    private static Entry entry(Level level, BlockPos source) {
        LevelCache cache = CACHES.computeIfAbsent(level, l -> new LevelCache());
        long version = NetworkVersion.get();
        if (cache.version != version) {
            cache.entries.clear();
            cache.version = version;
        }
        long now = level.getGameTime();
        Entry entry = cache.entries.get(source);
        if (entry == null || (entry.incomplete && now - entry.builtAt >= INCOMPLETE_RETRY_TICKS)) {
            entry = scan(level, source, now);
            cache.entries.put(source, entry);
        }
        return entry;
    }

    private static Entry scan(Level level, BlockPos source, long now) {
        BlockState self = level.getBlockState(source);
        if (!(self.getBlock() instanceof BlockAttachment)) {
            return new Entry(List.of(), Map.of(), false, now);
        }
        int maxPipes = ModConfig.MAX_NETWORK_SIZE.get();
        Set<BlockPos> importers = new LinkedHashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, BlockPos> parents = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        boolean incomplete = false;
        visited.add(source);
        queue.add(source);


        while (!queue.isEmpty()) {
            BlockPos from = queue.poll();
            BlockState fromState = level.getBlockState(from);
            for (Direction side : Direction.values()) {
                if (fromState.getBlock() instanceof BlockAttachment && !BlockAttachment.connectsOn(fromState, side)) {
                    continue;
                }
                BlockPos next = from.relative(side);
                if (visited.contains(next)) {
                    continue;
                }
                if (!level.isLoaded(next)) {
                    incomplete = true;
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof BlockPipe) {
                    if (visited.size() < maxPipes && visited.add(next)) {
                        parents.put(next, from);
                        queue.add(next);
                    }
                } else if (state.getBlock() instanceof BlockAttachment && BlockAttachment.connectsOn(state, side.getOpposite())) {
                    parents.putIfAbsent(next, from);
                    if (state.getBlock() instanceof BlockImporter) {
                        importers.add(next);
                    }
                    if (visited.size() < maxPipes && visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }

        return new Entry(List.copyOf(importers), parents, incomplete, now);
    }
}
