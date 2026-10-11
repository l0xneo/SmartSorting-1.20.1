package com.smartsorting.filter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ItemFilter {

    public static final int METHOD_MOD = 1;
    public static final int METHOD_ITEMS = 2;
    public static final int PAGE_SIZE = 27;
    public static final int PAGES = 10;
    public static final int SLOTS = PAGE_SIZE * PAGES;

    private static final Pattern MOD_ID = Pattern.compile("[a-z0-9_.\\-]{1,64}");

    private static final String TAG_METHOD = "SelectedTab";
    private static final String TAG_MOD_ID = "ConfiguredModId";
    private static final String TAG_GHOSTS = "Tab2Inventory";
    private static final String TAG_INVERT = "Invert";
    private static final String TAG_META = "MatchMeta";
    private static final String TAG_NBT = "MatchNbt";
    private static final String TAG_ORE = "OreDict";

    private final Runnable onChange;

    private int method = METHOD_MOD;
    private String modId = "";
    private boolean invert;
    private boolean matchMeta;
    private boolean matchNbt;
    private boolean oreDict;

    private final ItemStackHandler ghosts = new ItemStackHandler(SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            onChange.run();
        }
    };

    public ItemFilter(Runnable onChange) {
        this.onChange = onChange;
    }



    public boolean allows(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return matches(stack) != invert;
    }

    private boolean matches(ItemStack stack) {
        if (method == METHOD_MOD) {
            ResourceLocation name = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return !modId.isEmpty() && name != null && modId.equals(name.getNamespace());
        }
        Set<TagKey<Item>> stackTags = oreDict ? commonTags(stack) : Set.of();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack ghost = ghosts.getStackInSlot(i);
            if (!ghost.isEmpty() && matchesGhost(ghost, stack, stackTags)) {
                return true;
            }
        }
        return false;
    }

    private static Set<TagKey<Item>> commonTags(ItemStack stack) {
        return stack.getTags().filter(tag -> tag.location().getNamespace().equals("forge") && tag.location().getPath().contains("/")).collect(Collectors.toSet());
    }

    private boolean matchesGhost(ItemStack ghost, ItemStack stack, Set<TagKey<Item>> stackTags) {
        if (!stackTags.isEmpty() && ghost.getTags().anyMatch(stackTags::contains)) {
            return true;
        }
        return ghost.getItem() == stack.getItem()
                && (!matchMeta || ghost.isDamaged() == stack.isDamaged())
                && (!matchNbt || Objects.equals(withoutDamage(ghost), withoutDamage(stack)));
    }

    @Nullable
    private static CompoundTag withoutDamage(ItemStack stack) {
        if (stack.getTag() == null) {
            return null;
        }
        CompoundTag tag = stack.getTag().copy();
        tag.remove(ItemStack.TAG_DAMAGE);
        return tag.isEmpty() ? null : tag;
    }



    public int getMethod() {
        return method;
    }

    public void setMethod(int newMethod) {
        int value = newMethod == METHOD_ITEMS ? METHOD_ITEMS : METHOD_MOD;
        if (value != method) {
            method = value;
            onChange.run();
        }
    }

    public String getModId() {
        return modId;
    }

    public void setModId(String newModId) {
        String value = newModId == null ? "" : newModId.trim();
        if ((value.isEmpty() || MOD_ID.matcher(value).matches()) && !value.equals(modId)) {
            modId = value;
            onChange.run();
        }
    }

    public boolean isInvert() {
        return invert;
    }

    public boolean isMatchMeta() {
        return matchMeta;
    }

    public boolean isMatchNbt() {
        return matchNbt;
    }

    public boolean isOreDict() {
        return oreDict;
    }

    public void toggleInvert() {
        invert = !invert;
        onChange.run();
    }

    public void toggleMatchMeta() {
        matchMeta = !matchMeta;
        onChange.run();
    }

    public void toggleMatchNbt() {
        matchNbt = !matchNbt;
        onChange.run();
    }

    public void toggleOreDict() {
        oreDict = !oreDict;
        onChange.run();
    }

    public ItemStackHandler getGhosts() {
        return ghosts;
    }

    public void setGhost(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SLOTS) {
            ghosts.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(stack, 1));
        }
    }

    public void addGhost(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        int free = -1;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack ghost = ghosts.getStackInSlot(i);
            if (ghost.isEmpty()) {
                free = free < 0 ? i : free;
            } else if (ItemHandlerHelper.canItemStacksStack(ghost, ItemHandlerHelper.copyStackWithSize(stack, 1))) {
                return;
            }
        }
        setGhost(free, stack);
    }

    public void clearGhosts() {

        ghosts.deserializeNBT(new ItemStackHandler(SLOTS).serializeNBT());
        onChange.run();
    }



    public void write(CompoundTag tag) {
        tag.putInt(TAG_METHOD, method);
        if (!modId.isEmpty()) {
            tag.putString(TAG_MOD_ID, modId);
        }
        tag.put(TAG_GHOSTS, ghosts.serializeNBT());
        tag.putBoolean(TAG_INVERT, invert);
        tag.putBoolean(TAG_META, matchMeta);
        tag.putBoolean(TAG_NBT, matchNbt);
        tag.putBoolean(TAG_ORE, oreDict);
    }

    public void read(CompoundTag tag) {
        method = tag.getInt(TAG_METHOD) == METHOD_ITEMS ? METHOD_ITEMS : METHOD_MOD;
        modId = tag.getString(TAG_MOD_ID);
        invert = tag.getBoolean(TAG_INVERT);
        matchMeta = tag.getBoolean(TAG_META);
        matchNbt = tag.getBoolean(TAG_NBT);
        oreDict = tag.getBoolean(TAG_ORE);

        ItemStackHandler loaded = new ItemStackHandler(SLOTS);
        loaded.deserializeNBT(tag.getCompound(TAG_GHOSTS));
        ItemStackHandler result = new ItemStackHandler(SLOTS);
        for (int i = 0; i < Math.min(SLOTS, loaded.getSlots()); i++) {
            ItemStack stack = loaded.getStackInSlot(i);
            if (!stack.isEmpty()) {
                result.setStackInSlot(i, ItemHandlerHelper.copyStackWithSize(stack, 1));
            }
        }
        ghosts.deserializeNBT(result.serializeNBT());
    }
}
