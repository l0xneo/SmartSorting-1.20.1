package com.smartsorting.transport;

import com.smartsorting.SmartSorting;
import com.smartsorting.net.MsgTransitAdd;
import com.smartsorting.net.MsgTransitChange;
import com.smartsorting.net.PacketHandler;
import com.smartsorting.tile.TileChestMover;
import com.smartsorting.tile.TileExporter;
import com.smartsorting.tile.TileImporter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransitManager extends SavedData {

    private static final String NAME = SmartSorting.MOD_ID + "_transit";
    public static final float SPACING = 0.25F;
    private static final int RETRY_TICKS = 20;

    static final class Item {
        final int id;
        ItemStack stack;
        final List<BlockPos> path;
        final BlockPos source;
        final float speed;
        float progress;
        float end;
        @Nullable
        BlockPos dropAt;
        boolean stuck;
        long retryAt;
        float sentSpeed;

        Vec3 pos;
        Vec3 dir;

        Item(int id, ItemStack stack, List<BlockPos> path, BlockPos source, float speed) {
            this.id = id;
            this.stack = stack;
            this.path = path;
            this.source = source;
            this.speed = speed;
            this.end = path.size() - 1.35F;
            this.sentSpeed = speed;
        }

        void snapshot() {
            pos = PathMath.point(path, progress);
            dir = PathMath.direction(path, progress);
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final Set<BlockPos> stuckSources = new HashSet<>();
    private int nextId;

    private final List<Item> added = new ArrayList<>();
    private final Set<Item> changed = new HashSet<>();
    private final List<Integer> removed = new ArrayList<>();

    public static TransitManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TransitManager::load, TransitManager::new, NAME);
    }



    public boolean hasStuck(BlockPos source) {
        return stuckSources.contains(source);
    }

    public boolean canLaunch(List<BlockPos> path) {
        return canLaunch(path, 0);
    }

    public boolean canLaunch(List<BlockPos> path, float startProgress) {
        Vec3 start = PathMath.point(path, startProgress);
        Vec3 dir = PathMath.direction(path, startProgress);
        List<Item> near = new ArrayList<>();
        for (Item item : items) {
            item.snapshot();
            if (item.pos.distanceToSqr(start) < 1) {
                near.add(item);
            }
        }
        return limit(null, start, dir, startProgress, near) >= startProgress;
    }

    public void launch(ServerLevel level, BlockPos source, ItemStack stack, List<BlockPos> path, float speed) {
        launch(level, source, stack, path, speed, 0);
    }

    public void launch(ServerLevel level, BlockPos source, ItemStack stack, List<BlockPos> path, float speed, float startProgress) {
        Item item = new Item(nextId++, stack, List.copyOf(path), source.immutable(), speed);
        item.progress = startProgress;
        items.add(item);
        added.add(item);
        setDirty();
    }



    public void tick(ServerLevel level) {
        if (items.isEmpty() && removed.isEmpty() && added.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Map<BlockPos, List<Item>> byBlock = new HashMap<>();
        for (Item item : items) {
            item.snapshot();
            byBlock.computeIfAbsent(BlockPos.containing(item.pos), p -> new ArrayList<>()).add(item);
        }
        stuckSources.clear();

        for (Item item : new ArrayList<>(items)) {
            if (item.stuck) {
                if (now >= item.retryAt) {
                    deliver(level, item, now);
                }
                continue;
            }
            float target = Math.min(item.progress + item.speed, item.end);
            BlockPos entering = item.path.get(Math.min(item.path.size() - 1, (int) Math.ceil(target)));
            if (!level.isLoaded(entering)) {
                target = item.progress;
            } else {
                int here = Math.round(item.progress);
                List<Item> near = new ArrayList<>();
                for (int i = here; i <= Math.min(here + 1, item.path.size() - 1); i++) {
                    near.addAll(byBlock.getOrDefault(item.path.get(i), List.of()));
                }
                target = Math.max(item.progress, Math.min(target, limit(item, item.pos, item.dir, item.progress, near)));
            }
            float moved = target - item.progress;
            item.progress = target;
            if (Math.abs(moved - item.sentSpeed) > 1e-4F) {
                item.sentSpeed = moved;
                changed.add(item);
            }
            if (item.progress >= item.end) {
                arrive(level, item, now);
            }
        }
        for (Item item : items) {
            if (item.stuck) {
                stuckSources.add(item.source);
            }
        }
        if (!items.isEmpty()) {
            setDirty();
        }
        flush(level);
    }

    private static float limit(@Nullable Item self, Vec3 pos, Vec3 dir, float progress, List<Item> near) {
        float limit = Float.MAX_VALUE;
        for (Item other : near) {
            if (other == self || dir.dot(other.dir) < 0.5) {
                continue;
            }
            Vec3 offset = other.pos.subtract(pos);
            double ahead = offset.dot(dir);

            boolean leads = ahead > 1e-4 || (Math.abs(ahead) <= 1e-4 && (self == null || other.id < self.id));
            if (!leads || ahead > 1.5 || offset.subtract(dir.scale(ahead)).lengthSqr() > 0.04) {
                continue;
            }
            limit = Math.min(limit, progress + (float) ahead - SPACING);
        }
        return limit;
    }

    private void arrive(ServerLevel level, Item item, long now) {
        if (item.dropAt != null) {
            drop(level, item, PathMath.point(item.path, item.progress));
        } else {
            deliver(level, item, now);
        }
    }

    private void deliver(ServerLevel level, Item item, long now) {
        ItemStack left = item.stack;
        BlockPos last = item.path.get(item.path.size() - 1);
        BlockPos beforeLast = item.path.get(item.path.size() - 2);
        IItemHandler destination = null;
        if (level.isLoaded(beforeLast) && level.getBlockEntity(beforeLast) instanceof TileImporter importer) {
            destination = importer.getDestinationHandler();
        } else if (level.isLoaded(beforeLast) && level.getBlockEntity(beforeLast) instanceof TileChestMover) {
            destination = chestNextTo(level, last, beforeLast);
        }
        if (destination != null) {
            left = ItemHandlerHelper.insertItemStacked(destination, left, false);
        }
        IItemHandler source = null;
        if (!left.isEmpty() && level.isLoaded(item.source) && level.getBlockEntity(item.source) instanceof TileExporter exporter) {
            source = exporter.getSourceHandler();
        } else if (!left.isEmpty() && level.isLoaded(item.source) && level.getBlockEntity(item.source) instanceof TileChestMover) {
            source = chestNextTo(level, item.path.get(0), item.source);
        }
        if (source != null) {
            left = ItemHandlerHelper.insertItemStacked(source, left, false);
        }
        if (left.isEmpty()) {
            remove(item);
            return;
        }
        item.stack = left;
        item.stuck = true;
        item.retryAt = now + RETRY_TICKS;
        setDirty();
    }

    @Nullable
    private static IItemHandler chestNextTo(ServerLevel level, BlockPos chest, BlockPos from) {
        if (!level.isLoaded(chest)) {
            return null;
        }
        BlockPos d = from.subtract(chest);
        return InventoryAccess.getHandler(level.getBlockEntity(chest), Direction.getNearest(d.getX(), d.getY(), d.getZ()));
    }



    public void onImporterChanged(ServerLevel level, TileImporter importer) {
        for (Item item : new ArrayList<>(items)) {
            if (item.dropAt != null || !item.path.get(item.path.size() - 2).equals(importer.getBlockPos()) || importer.accepts(item.stack)
                    || !(level.getBlockEntity(item.source) instanceof TileExporter exporter)) {
                continue;
            }

            int here = Math.min((int) Math.ceil(item.progress), item.path.size() - 1);
            List<BlockPos> back = new ArrayList<>(item.path.subList(0, here + 1));
            Collections.reverse(back);
            back.add(exporter.getSourcePos());
            Item returning = new Item(nextId++, item.stack, List.copyOf(back), item.source, item.speed);
            returning.progress = Math.min(here - item.progress, returning.end);
            remove(item);
            items.add(returning);
            added.add(returning);
        }
        setDirty();
        flush(level);
    }



    public void onBroken(ServerLevel level, BlockPos pos) {
        for (Item item : new ArrayList<>(items)) {
            if (BlockPos.containing(PathMath.point(item.path, item.progress)).equals(pos)) {
                drop(level, item, Vec3.atCenterOf(pos));
                continue;
            }
            int index = item.path.indexOf(pos);
            if (index > item.progress && index - 0.5F < item.end) {
                item.end = index - 0.5F;
                item.dropAt = pos;
                item.stuck = false;
                changed.add(item);
            }
        }
        setDirty();
        flush(level);
    }

    private void drop(ServerLevel level, Item item, Vec3 at) {
        Containers.dropItemStack(level, at.x, at.y, at.z, item.stack);
        remove(item);
    }

    private void remove(Item item) {
        items.remove(item);
        changed.remove(item);
        if (!added.remove(item)) {
            removed.add(item.id);
        }
        setDirty();
    }



    private void flush(ServerLevel level) {
        PacketDistributor.PacketTarget everyone = PacketDistributor.DIMENSION.with(level::dimension);
        if (!added.isEmpty()) {
            PacketHandler.CHANNEL.send(everyone, new MsgTransitAdd(false, added.stream().map(TransitManager::data).toList()));
            added.clear();
        }
        if (!changed.isEmpty() || !removed.isEmpty()) {
            List<MsgTransitChange.Change> changes = changed.stream()
                    .map(i -> new MsgTransitChange.Change(i.id, i.progress, i.sentSpeed, i.end)).toList();
            PacketHandler.CHANNEL.send(everyone, new MsgTransitChange(changes, List.copyOf(removed)));
            changed.clear();
            removed.clear();
        }
    }

    public void syncTo(ServerPlayer player) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new MsgTransitAdd(true, items.stream().map(TransitManager::data).toList()));
    }

    private static MsgTransitAdd.Data data(Item item) {
        return new MsgTransitAdd.Data(item.id, item.stack, item.path, item.progress, item.sentSpeed, item.end);
    }



    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Item item : items) {
            CompoundTag entry = new CompoundTag();
            entry.put("Item", item.stack.save(new CompoundTag()));
            entry.putLongArray("Path", item.path.stream().mapToLong(BlockPos::asLong).toArray());
            entry.putLong("Source", item.source.asLong());
            entry.putFloat("Speed", item.speed);
            entry.putFloat("Progress", item.progress);
            entry.putFloat("End", item.end);
            if (item.dropAt != null) {
                entry.putLong("DropAt", item.dropAt.asLong());
            }
            entry.putBoolean("Stuck", item.stuck);
            list.add(entry);
        }
        tag.put("Items", list);
        return tag;
    }

    private static TransitManager load(CompoundTag tag) {
        TransitManager manager = new TransitManager();
        for (Tag raw : tag.getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ItemStack stack = ItemStack.of(entry.getCompound("Item"));
            List<BlockPos> path = Arrays.stream(entry.getLongArray("Path")).mapToObj(BlockPos::of).toList();
            if (stack.isEmpty() || path.size() < 2) {
                continue;
            }
            Item item = new Item(manager.nextId++, stack, path, BlockPos.of(entry.getLong("Source")), entry.getFloat("Speed"));
            item.progress = entry.getFloat("Progress");
            item.end = entry.getFloat("End");
            item.dropAt = entry.contains("DropAt") ? BlockPos.of(entry.getLong("DropAt")) : null;
            item.stuck = entry.getBoolean("Stuck");
            item.sentSpeed = item.stuck ? 0 : item.speed;
            if (item.stuck) {
                manager.stuckSources.add(item.source);
            }
            manager.items.add(item);
        }
        return manager;
    }
}
