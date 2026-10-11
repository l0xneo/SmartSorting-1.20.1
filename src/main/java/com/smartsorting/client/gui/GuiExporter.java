package com.smartsorting.client.gui;

import com.smartsorting.gui.ContainerExporter;
import com.smartsorting.registry.ModItems;
import com.smartsorting.tile.ExporterStatus;
import com.smartsorting.tile.TileExporter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GuiExporter extends GuiBase<ContainerExporter> {

    private final TileExporter tile;

    public GuiExporter(ContainerExporter container, Inventory inventory, Component title) {
        super(container, inventory, title, 176, 166);
        this.tile = container.getTile();

        addButton(new Button(8, 32, 100, 16))
                .label(() -> tr(tile.getRedstoneMode().translationKey()))
                .tooltip(() -> lines(tr("smartsorting.gui.redstone.tip"), tr("smartsorting.gui.cycle.tip")))
                .onClick(b -> sendButton(TileExporter.BTN_REDSTONE, b == 1 ? 1 : 0));
        addButton(new Button(8, 52, 100, 16))
                .label(() -> tr(tile.isRoundRobin() ? "smartsorting.gui.round_robin" : "smartsorting.gui.nearest"))
                .tooltip(() -> lines(tr(tile.isRoundRobin() ? "smartsorting.gui.round_robin.tip" : "smartsorting.gui.nearest.tip"),
                        tr("smartsorting.gui.priority_note")))
                .onClick(b -> sendButton(TileExporter.BTN_ORDER, 0));
    }

    @Override
    protected void drawForeground(GuiGraphics g, int relX, int relY) {
        g.drawString(font, title, 8, 6, GuiStyle.TEXT, false);
        ExporterStatus status = menu.getClientStatus();
        g.fill(8, 19, 13, 24, status.color);
        drawText(g, tr(status.translationKey()), 17, 18, GuiStyle.MUTED);

        drawText(g, tr("smartsorting.gui.upgrades"), ContainerExporter.SPEED_X - 1, 28, GuiStyle.MUTED);
        float sendsPerSecond = 20.0F / tile.getInterval();
        drawText(g, tr("smartsorting.gui.rate", String.format("%.1f", sendsPerSecond * tile.getItemsPerTransfer())),
                ContainerExporter.SPEED_X - 1, 58, GuiStyle.TEXT);
        drawText(g, tr("smartsorting.gui.travel", String.format("%.2f", tile.getTravelSpeed() * 20).replaceAll("\\.?0+$", "")),
                ContainerExporter.SPEED_X - 1, 68, GuiStyle.MUTED);
        drawText(g, tr("container.inventory"), 8, ContainerExporter.PLAYER_Y - 11, GuiStyle.MUTED);

        drawGhostIcon(g, 0, ModItems.SPEED_UPGRADE.get());
        drawGhostIcon(g, 1, ModItems.STACK_UPGRADE.get());
        drawGhostIcon(g, 2, ModItems.TRANSIT_UPGRADE.get());
    }

    private void drawGhostIcon(GuiGraphics g, int slotIndex, Item item) {
        Slot slot = menu.getSlot(slotIndex);
        if (slot.hasItem()) {
            return;
        }
        g.renderItem(new ItemStack(item), slot.x, slot.y);
        g.fill(RenderType.guiOverlay(), slot.x, slot.y, slot.x + 16, slot.y + 16, 0xB014171B);
    }

    @Override
    protected void drawExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
        Slot slot = hoveredSlot;
        if (slot != null && slot.index < TileExporter.UPGRADE_SLOTS && !slot.hasItem()) {
            String key = "item.smartsorting." + new String[] {"speed_upgrade", "stack_upgrade", "transit_upgrade"}[slot.index];
            drawTooltip(g, lines(tr(key), tr(key + ".tooltip.0"), tr("smartsorting.gui.max_upgrades", TileExporter.MAX_UPGRADES)),
                    mouseX, mouseY);
        }
    }
}
