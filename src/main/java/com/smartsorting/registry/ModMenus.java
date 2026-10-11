package com.smartsorting.registry;

import com.smartsorting.SmartSorting;
import com.smartsorting.gui.ContainerExporter;
import com.smartsorting.gui.ContainerImporter;
import com.smartsorting.gui.ContainerMover;
import com.smartsorting.gui.ContainerStorage;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SmartSorting.MOD_ID);

    public static final RegistryObject<MenuType<ContainerExporter>> EXPORTER =
            MENUS.register("chest_exporter", () -> IForgeMenuType.create(ContainerExporter::new));
    public static final RegistryObject<MenuType<ContainerImporter>> IMPORTER =
            MENUS.register("chest_importer", () -> IForgeMenuType.create(ContainerImporter::new));
    public static final RegistryObject<MenuType<ContainerStorage>> STORAGE =
            MENUS.register("chest_link", () -> IForgeMenuType.create(ContainerStorage::new));
    public static final RegistryObject<MenuType<ContainerMover>> MOVER =
            MENUS.register("chest_mover", () -> IForgeMenuType.create(ContainerMover::new));

    private ModMenus() {
    }
}
