package com.smartsorting.client.gui;

import com.smartsorting.filter.ItemFilter;
import com.smartsorting.gui.ContainerImporter;
import com.smartsorting.gui.SlotGhost;
import com.smartsorting.net.MsgSetModFilter;
import com.smartsorting.net.PacketHandler;
import com.smartsorting.tile.TileImporter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class GuiImporter extends GuiBase<ContainerImporter> {

    private enum Tab { MOD, ITEMS }

    private static final int LIST_X = 10;
    private static final int LIST_Y = 42;
    private static final int LIST_WIDTH = 150;
    private static final int ROW_HEIGHT = 11;
    private static final int VISIBLE_ROWS = 6;

    private final TileImporter tile;
    private Tab tab;
    private int page;

    private EditBox search;
    private final List<ModEntry> allMods = new ArrayList<>();
    private final List<ModEntry> shownMods = new ArrayList<>();
    private int scroll;

    public GuiImporter(ContainerImporter container, Inventory inventory, Component title) {
        super(container, inventory, title, 176, 222);
        this.tile = container.getTile();
        loadMods();
        createButtons();
        showTab(tile.getFilter().getMethod() == ItemFilter.METHOD_ITEMS ? Tab.ITEMS : Tab.MOD);
    }

    private ItemFilter filter() {
        return tile.getFilter();
    }

    public boolean isItemsTabOpen() {
        return tab == Tab.ITEMS;
    }

    public TileImporter getTile() {
        return tile;
    }



    @Override
    protected void init() {
        super.init();
        String text = search == null ? "" : search.getValue();
        search = new EditBox(font, leftPos + LIST_X + 1, topPos + 27, 118, 10, Component.empty());
        search.setMaxLength(40);
        search.setValue(text);
        search.setResponder(value -> {
            scroll = 0;
            refilter();
        });
        search.visible = tab == Tab.MOD;
        addRenderableWidget(search);
        refilter();
        scrollToSelected();
    }

    private void createButtons() {
        int tabWidth = 52;
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            Tab target = tabs[i];
            addButton(new Button(6 + i * (tabWidth + 4), 5, tabWidth, 15))
                    .label(() -> tabLabel(target))
                    .color(() -> tab == target ? GuiStyle.ACCENT : 0)
                    .tooltip(() -> lines(tr("smartsorting.gui.tab." + target.name().toLowerCase(Locale.ROOT) + ".tip")))
                    .onClick(b -> showTab(target));
        }

        addButton(new Button(116, 5, 12, 15)).visible(() -> tab == Tab.ITEMS).label(() -> "<")
                .tooltip(() -> lines(tr("smartsorting.gui.prev_page")))
                .onClick(b -> showPage(page - 1));
        addButton(new Button(158, 5, 12, 15)).visible(() -> tab == Tab.ITEMS).label(() -> ">")
                .tooltip(() -> lines(tr("smartsorting.gui.next_page")))
                .onClick(b -> showPage(page + 1));

        addButton(new Button(132, 25, 36, 14)).visible(() -> tab == Tab.MOD)
                .label(() -> tr("smartsorting.gui.clear"))
                .tooltip(() -> lines(tr("smartsorting.gui.clear_mod.tip")))
                .onClick(b -> PacketHandler.CHANNEL.sendToServer(new MsgSetModFilter(tile.getBlockPos(), "")));

        addToggle(8, tr("smartsorting.gui.meta"), "smartsorting.gui.meta.tip", filter()::isMatchMeta, TileImporter.BTN_META);
        addToggle(48, tr("smartsorting.gui.nbt"), "smartsorting.gui.nbt.tip", filter()::isMatchNbt, TileImporter.BTN_NBT);
        addToggle(88, tr("smartsorting.gui.ore"), "smartsorting.gui.ore.tip", filter()::isOreDict, TileImporter.BTN_ORE);
        addButton(new Button(128, 84, 40, 14)).visible(() -> tab == Tab.ITEMS)
                .label(() -> tr("smartsorting.gui.clear"))
                .tooltip(() -> lines(tr("smartsorting.gui.clear_items.tip")))
                .onClick(b -> sendButton(TileImporter.BTN_CLEAR_GHOSTS, 0));

        addButton(new Button(8, 104, 52, 14))
                .label(() -> isActiveMethod() ? "✔ " + tr("smartsorting.gui.in_use") : tr("smartsorting.gui.use"))
                .color(() -> isActiveMethod() ? GuiStyle.GREEN : 0)
                .tooltip(() -> lines(tr("smartsorting.gui.use.tip"), tr("smartsorting.gui.empty_filter.tip")))
                .onClick(b -> sendButton(TileImporter.BTN_METHOD, tab == Tab.ITEMS ? ItemFilter.METHOD_ITEMS : ItemFilter.METHOD_MOD));
        addButton(new Button(62, 104, 60, 14))
                .label(() -> tr(filter().isInvert() ? "smartsorting.gui.blacklist" : "smartsorting.gui.whitelist"))
                .color(() -> filter().isInvert() ? GuiStyle.shade(GuiStyle.RED, 0.8F) : 0)
                .tooltip(() -> lines(tr(filter().isInvert() ? "smartsorting.gui.blacklist.tip" : "smartsorting.gui.whitelist.tip")))
                .onClick(b -> sendButton(TileImporter.BTN_INVERT, 0));

        addButton(new Button(62, 125, 34, 12))
                .label(() -> tr(tile.isEnabled() ? "smartsorting.gui.importing_on" : "smartsorting.gui.importing_off"))
                .color(() -> tile.isEnabled() ? GuiStyle.GREEN : GuiStyle.RED)
                .tooltip(() -> lines(tr(tile.isEnabled() ? "smartsorting.gui.importing_on.tip" : "smartsorting.gui.importing_off.tip")))
                .onClick(b -> sendButton(TileImporter.BTN_ENABLED, 0));
        List<String> tip = lines(tr("smartsorting.gui.importer_priority.tip"), tr("smartsorting.gui.shift_ten"));
        addButton(new Button(124, 104, 12, 14)).label(() -> "-").tooltip(() -> tip)
                .onClick(b -> sendButton(TileImporter.BTN_PRIORITY, hasShiftDown() ? -10 : -1));
        addButton(new Button(156, 104, 12, 14)).label(() -> "+").tooltip(() -> tip)
                .onClick(b -> sendButton(TileImporter.BTN_PRIORITY, hasShiftDown() ? 10 : 1));
    }

    private void addToggle(int x, String label, String tipKey, BooleanSupplier state, int buttonId) {
        addButton(new Button(x, 84, 38, 14)).visible(() -> tab == Tab.ITEMS)
                .label(() -> label)
                .color(() -> state.getAsBoolean() ? GuiStyle.ACCENT : 0)
                .tooltip(() -> lines(tr(tipKey), tr(state.getAsBoolean() ? "smartsorting.gui.on" : "smartsorting.gui.off")))
                .onClick(b -> sendButton(buttonId, 0));
    }

    private boolean isActiveMethod() {
        int method = tab == Tab.ITEMS ? ItemFilter.METHOD_ITEMS : ItemFilter.METHOD_MOD;
        return filter().getMethod() == method;
    }

    private String tabLabel(Tab target) {
        String label = tr("smartsorting.gui.tab." + target.name().toLowerCase(Locale.ROOT));
        boolean active = (target == Tab.MOD) == (filter().getMethod() == ItemFilter.METHOD_MOD);
        return active ? "● " + label : label;
    }



    private void loadMods() {
        Map<String, Integer> counts = new HashMap<>();
        for (ResourceLocation name : ForgeRegistries.ITEMS.getKeys()) {
            counts.merge(name.getNamespace(), 1, Integer::sum);
        }
        Map<String, String> names = new HashMap<>();
        for (IModInfo mod : ModList.get().getMods()) {
            names.put(mod.getModId(), mod.getDisplayName());
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            allMods.add(new ModEntry(entry.getKey(), names.getOrDefault(entry.getKey(), entry.getKey()), entry.getValue()));
        }
        allMods.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
    }

    private void refilter() {
        shownMods.clear();
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        for (ModEntry entry : allMods) {
            if (query.isEmpty() || entry.name.toLowerCase(Locale.ROOT).contains(query) || entry.id.contains(query)) {
                shownMods.add(entry);
            }
        }
        clampScroll();
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, shownMods.size() - VISIBLE_ROWS));
    }

    private void scrollToSelected() {
        for (int i = 0; i < shownMods.size(); i++) {
            if (shownMods.get(i).id.equals(filter().getModId())) {
                scroll = i - VISIBLE_ROWS / 2;
                clampScroll();
                return;
            }
        }
    }

    private int rowAt(int relX, int relY) {
        if (relX < LIST_X || relX >= LIST_X + LIST_WIDTH || relY < LIST_Y || relY >= LIST_Y + VISIBLE_ROWS * ROW_HEIGHT) {
            return -1;
        }
        int index = scroll + (relY - LIST_Y) / ROW_HEIGHT;
        return index < shownMods.size() ? index : -1;
    }



    private void showTab(Tab target) {
        tab = target;
        updateGhostSlots();
        if (search != null) {
            search.visible = tab == Tab.MOD;
            search.setFocused(false);
        }
    }

    private void showPage(int target) {
        page = Math.floorMod(target, ItemFilter.PAGES);
        updateGhostSlots();
    }

    private void updateGhostSlots() {
        for (Slot slot : menu.slots) {
            if (slot instanceof SlotGhost ghost) {
                ghost.setEnabled(tab == Tab.ITEMS && slot.getSlotIndex() / ItemFilter.PAGE_SIZE == page);
            }
        }
    }



    @Override
    protected void containerTick() {
        super.containerTick();
        search.tick();
    }

    @Override
    protected void drawBackground(GuiGraphics g, int mouseX, int mouseY) {
        GuiStyle.inset(g, leftPos + 4, topPos + 22, imageWidth - 8, 100);
        if (tab == Tab.MOD) {
            drawModList(g, mouseX - leftPos, mouseY - topPos);
        }
    }

    private void drawModList(GuiGraphics g, int relX, int relY) {
        int hovered = rowAt(relX, relY);
        int last = Math.min(shownMods.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < last; i++) {
            ModEntry entry = shownMods.get(i);
            int x = leftPos + LIST_X;
            int y = topPos + LIST_Y + (i - scroll) * ROW_HEIGHT;
            boolean selected = entry.id.equals(filter().getModId());
            if (selected) {
                g.fill(x, y, x + LIST_WIDTH, y + ROW_HEIGHT, 0x993FA9F5);
            } else if (i == hovered) {
                g.fill(x, y, x + LIST_WIDTH, y + ROW_HEIGHT, 0x33FFFFFF);
            }
            String count = String.valueOf(entry.items);
            int countWidth = font.width(count);
            String name = font.plainSubstrByWidth((selected ? "✔ " : "") + entry.name, LIST_WIDTH - countWidth - 8);
            drawText(g, name, x + 3, y + 2, selected ? 0xFFFFFFFF : GuiStyle.TEXT);
            drawText(g, count, x + LIST_WIDTH - countWidth - 3, y + 2, GuiStyle.MUTED);
        }
        if (shownMods.isEmpty()) {
            drawText(g, tr("smartsorting.gui.no_mods"), leftPos + LIST_X + 3, topPos + LIST_Y + 2, GuiStyle.MUTED);
        }

        int barX = leftPos + LIST_X + LIST_WIDTH + 2;
        int barY = topPos + LIST_Y;
        int barHeight = VISIBLE_ROWS * ROW_HEIGHT;
        g.fill(barX, barY, barX + 4, barY + barHeight, GuiStyle.SLOT);
        if (shownMods.size() > VISIBLE_ROWS) {
            int thumb = Math.max(8, barHeight * VISIBLE_ROWS / shownMods.size());
            int thumbY = barY + (barHeight - thumb) * scroll / (shownMods.size() - VISIBLE_ROWS);
            g.fill(barX, thumbY, barX + 4, thumbY + thumb, GuiStyle.MUTED);
        }
    }

    @Override
    protected void drawForeground(GuiGraphics g, int relX, int relY) {
        if (tab == Tab.MOD && search.getValue().isEmpty() && !search.isFocused()) {
            drawText(g, tr("smartsorting.gui.search"), LIST_X + 5, 28, GuiStyle.MUTED);
        }
        if (tab == Tab.ITEMS) {
            String pageText = (page + 1) + "/" + ItemFilter.PAGES;
            drawText(g, pageText, 143 - font.width(pageText) / 2, 9, GuiStyle.TEXT);
        }
        int priority = tile.getPriority();
        String value = priority > 0 ? "+" + priority : String.valueOf(priority);
        drawText(g, value, 146 - font.width(value) / 2, 107, GuiStyle.TEXT);

        drawText(g, tr("container.inventory"), 8, 129, GuiStyle.MUTED);
        drawDestination(g);
    }

    private void drawDestination(GuiGraphics g) {
        BlockPos destination = tile.getDestinationPos();
        String target;
        int color;
        if (destination != null && tile.getDestinationHandler() != null) {
            BlockState state = minecraft.level.getBlockState(destination);
            ItemStack shown;
            try {
                shown = state.getBlock().getCloneItemStack(minecraft.level, destination, state);
            } catch (RuntimeException e) {
                shown = ItemStack.EMPTY;
            }
            target = "→ " + (shown.isEmpty() ? state.getBlock().getName() : shown.getHoverName()).getString();
            color = GuiStyle.GREEN;
        } else {
            target = tr("smartsorting.gui.no_inventory");
            color = GuiStyle.RED;
        }
        target = font.plainSubstrByWidth(target, imageWidth - 8 - 98);
        drawText(g, target, imageWidth - 8 - font.width(target), 129, color);
    }

    @Override
    protected int slotColor(Slot slot) {
        return slot instanceof SlotGhost ? GuiStyle.SLOT_GHOST : GuiStyle.SLOT;
    }

    @Override
    protected void drawExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (hoveredSlot instanceof SlotGhost && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            drawTooltip(g, lines(tr("smartsorting.gui.ghost.tip.0"), tr("smartsorting.gui.ghost.tip.1")), mouseX, mouseY);
        }
        if (tab == Tab.MOD) {
            int row = rowAt(mouseX - leftPos, mouseY - topPos);
            if (row >= 0) {
                ModEntry entry = shownMods.get(row);
                drawTooltip(g, lines(entry.name, "§7" + entry.id + " · " + tr("smartsorting.gui.item_count", entry.items),
                        tr("smartsorting.gui.mod_row.tip")), mouseX, mouseY);
            }
        }
    }



    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (tab == Tab.MOD) {
            if (!search.isMouseOver(mouseX, mouseY)) {
                search.setFocused(false);
            }
            int row = rowAt((int) mouseX - leftPos, (int) mouseY - topPos);
            if (row >= 0 && mouseButton == 0) {
                playClick();
                PacketHandler.CHANNEL.sendToServer(new MsgSetModFilter(tile.getBlockPos(), shownMods.get(row).id));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (tab == Tab.MOD && search.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean onScroll(int direction, int relX, int relY) {
        if (tab == Tab.ITEMS) {
            showPage(page + direction);
            return true;
        }
        scroll += direction;
        clampScroll();
        return true;
    }

    private record ModEntry(String id, String name, int items) {
    }
}
