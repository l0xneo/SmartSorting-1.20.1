package com.smartsorting.registry;

import com.smartsorting.SmartSorting;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SmartSorting.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register(SmartSorting.MOD_ID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + SmartSorting.MOD_ID))
            .icon(() -> new ItemStack(ModItems.CHEST_EXPORTER.get()))
            .displayItems((params, output) -> ModItems.ALL.forEach(item -> output.accept(item.get())))
            .build());

    private ModCreativeTab() {
    }
}
