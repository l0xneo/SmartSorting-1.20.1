package com.smartsorting.registry;

import com.smartsorting.SmartSorting;
import com.smartsorting.item.ItemBlockInfo;
import com.smartsorting.item.ItemUpgrade;
import com.smartsorting.item.ItemWrench;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SmartSorting.MOD_ID);

    public static final RegistryObject<Item> PIPE = block(ModBlocks.PIPE);
    public static final RegistryObject<Item> CHEST_EXPORTER = block(ModBlocks.CHEST_EXPORTER);
    public static final RegistryObject<Item> CHEST_IMPORTER = block(ModBlocks.CHEST_IMPORTER);
    public static final RegistryObject<Item> CHEST_LINK = block(ModBlocks.CHEST_LINK);
    public static final RegistryObject<Item> CHEST_MOVER = block(ModBlocks.CHEST_MOVER);
    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", ItemWrench::new);
    public static final RegistryObject<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade", ItemUpgrade::new);
    public static final RegistryObject<Item> STACK_UPGRADE = ITEMS.register("stack_upgrade", ItemUpgrade::new);
    public static final RegistryObject<Item> TRANSIT_UPGRADE = ITEMS.register("transit_upgrade", ItemUpgrade::new);

    public static final List<RegistryObject<Item>> ALL =
            List.of(PIPE, CHEST_EXPORTER, CHEST_IMPORTER, CHEST_LINK, CHEST_MOVER, WRENCH, SPEED_UPGRADE, STACK_UPGRADE, TRANSIT_UPGRADE);

    private ModItems() {
    }

    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new ItemBlockInfo(block.get()));
    }
}
