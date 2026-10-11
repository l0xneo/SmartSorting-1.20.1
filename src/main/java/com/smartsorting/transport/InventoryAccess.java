package com.smartsorting.transport;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;

public final class InventoryAccess {

    private InventoryAccess() {
    }

    @Nullable
    public static IItemHandler getHandler(@Nullable BlockEntity tile, Direction side) {
        if (tile == null || tile.isRemoved()) {
            return null;
        }
        IItemHandler handler = tile.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
        if (handler != null) {
            return handler;
        }
        if (tile instanceof WorldlyContainer sided) {
            return new SidedInvWrapper(sided, side);
        }
        if (tile instanceof Container container) {
            return new InvWrapper(container);
        }
        return null;
    }
}
