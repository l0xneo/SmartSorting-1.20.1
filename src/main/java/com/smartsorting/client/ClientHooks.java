package com.smartsorting.client;

import com.smartsorting.client.gui.GuiChestLink;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openChestLink(BlockPos pos) {
        Minecraft.getInstance().setScreen(new GuiChestLink(pos));
    }
}
