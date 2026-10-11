package com.smartsorting.registry;

import com.smartsorting.SmartSorting;
import com.smartsorting.client.gui.GuiChestMover;
import com.smartsorting.client.gui.GuiExporter;
import com.smartsorting.client.gui.GuiImporter;
import com.smartsorting.client.gui.GuiStorage;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SmartSorting.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistry {

    private ClientRegistry() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.EXPORTER.get(), GuiExporter::new);
            MenuScreens.register(ModMenus.IMPORTER.get(), GuiImporter::new);
            MenuScreens.register(ModMenus.STORAGE.get(), GuiStorage::new);
            MenuScreens.register(ModMenus.MOVER.get(), GuiChestMover::new);
        });
    }
}
