package com.smartsorting.tile;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IHasContents {

    void takeContents(List<ItemStack> out);
}
