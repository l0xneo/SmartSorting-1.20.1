package com.smartsorting.compat.jei;

import com.smartsorting.SmartSorting;
import com.smartsorting.client.gui.GuiImporter;
import com.smartsorting.gui.SlotGhost;
import com.smartsorting.net.MsgSetGhost;
import com.smartsorting.net.PacketHandler;
import com.smartsorting.registry.ModItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(SmartSorting.MOD_ID, "main");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(GuiImporter.class, new GhostHandler());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        addInfo(registration, ModItems.PIPE, "pipe");
        addInfo(registration, ModItems.CHEST_EXPORTER, "chest_exporter");
        addInfo(registration, ModItems.CHEST_IMPORTER, "chest_importer");
        addInfo(registration, ModItems.CHEST_LINK, "chest_link");
        addInfo(registration, ModItems.WRENCH, "wrench");
        addInfo(registration, ModItems.SPEED_UPGRADE, "upgrades");
        addInfo(registration, ModItems.STACK_UPGRADE, "upgrades");
        addInfo(registration, ModItems.TRANSIT_UPGRADE, "upgrades");
    }

    private static void addInfo(IRecipeRegistration registration, RegistryObject<Item> item, String page) {
        registration.addItemStackInfo(new ItemStack(item.get()), Component.translatable("smartsorting.jei." + page));
    }

    private static final class GhostHandler implements IGhostIngredientHandler<GuiImporter> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(GuiImporter gui, ITypedIngredient<I> ingredient, boolean doStart) {
            List<Target<I>> targets = new ArrayList<>();
            if (!(ingredient.getIngredient() instanceof ItemStack) || !gui.isItemsTabOpen()) {
                return targets;
            }
            for (Slot slot : gui.getMenu().slots) {
                if (slot instanceof SlotGhost && slot.isActive()) {
                    Rect2i area = new Rect2i(gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y, 16, 16);
                    int index = slot.getSlotIndex();
                    targets.add(new Target<I>() {
                        @Override
                        public Rect2i getArea() {
                            return area;
                        }

                        @Override
                        public void accept(I item) {
                            PacketHandler.CHANNEL.sendToServer(new MsgSetGhost(gui.getTile().getBlockPos(), index, (ItemStack) item));
                        }
                    });
                }
            }
            return targets;
        }

        @Override
        public void onComplete() {
        }
    }
}
