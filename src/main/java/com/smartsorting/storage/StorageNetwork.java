package com.smartsorting.storage;

import com.smartsorting.block.BlockChestLink;
import com.smartsorting.gui.ContainerStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StorageNetwork {

    private static final int MAX_NODES = 4096;

    private StorageNetwork() {
    }

    public static boolean isLink(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BlockChestLink;
    }

    private static boolean linkOpen(Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BlockChestLink && BlockChestLink.isOpen(state, side);
    }

    public static boolean isLinkedChest(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity)) {
            return false;
        }
        for (BlockPos half : halves(level, pos)) {
            for (Direction side : Direction.values()) {
                if (linkOpen(level, half.relative(side), side.getOpposite())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<ChestBlockEntity> chests(Level level, BlockPos origin) {
        List<ChestBlockEntity> chests = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> links = new ArrayDeque<>();
        if (isLink(level, origin)) {
            seen.add(origin);
            links.add(origin);
        } else {
            addChest(level, origin, chests, seen, links);
        }
        while (!links.isEmpty() && seen.size() < MAX_NODES) {
            BlockPos link = links.poll();
            for (Direction side : Direction.values()) {
                BlockPos next = link.relative(side);
                if (!linkOpen(level, link, side) || seen.contains(next) || !level.isLoaded(next)) {
                    continue;
                }
                if (isLink(level, next)) {
                    if (!linkOpen(level, next, side.getOpposite())) {
                        continue;
                    }
                    seen.add(next);
                    links.add(next);
                } else {
                    addChest(level, next, chests, seen, links);
                }
            }
        }
        return chests;
    }

    private static void addChest(Level level, BlockPos pos, List<ChestBlockEntity> chests, Set<BlockPos> seen, ArrayDeque<BlockPos> links) {
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity)) {
            return;
        }
        for (BlockPos half : halves(level, pos)) {
            if (!seen.add(half) || !(level.getBlockEntity(half) instanceof ChestBlockEntity chest)) {
                continue;
            }
            chests.add(chest);
            for (Direction side : Direction.values()) {
                BlockPos next = half.relative(side);
                if (!seen.contains(next) && linkOpen(level, next, side.getOpposite())) {
                    seen.add(next);
                    links.add(next);
                }
            }
        }
    }

    private static List<BlockPos> halves(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return List.of(pos, pos.relative(ChestBlock.getConnectedDirection(state)));
        }
        return List.of(pos);
    }

    public static void open(ServerPlayer player, BlockPos origin) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new ContainerStorage(id, inventory, origin),
                Component.translatable("container.smartsorting.chest_link")), buf -> buf.writeBlockPos(origin));
    }
}
