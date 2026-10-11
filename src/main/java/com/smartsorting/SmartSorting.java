package com.smartsorting;

import com.smartsorting.net.PacketHandler;
import com.smartsorting.registry.ModBlocks;
import com.smartsorting.registry.ModCreativeTab;
import com.smartsorting.registry.ModItems;
import com.smartsorting.registry.ModMenus;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SmartSorting.MOD_ID)
public class SmartSorting {

    public static final String MOD_ID = "smartsorting";
    public static final Logger LOGGER = LoggerFactory.getLogger("Smart Sorting");

    public SmartSorting(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.TILES.register(bus);
        ModItems.ITEMS.register(bus);
        ModMenus.MENUS.register(bus);
        ModCreativeTab.TABS.register(bus);
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, ModConfig.SPEC);
        PacketHandler.init();
    }
}
