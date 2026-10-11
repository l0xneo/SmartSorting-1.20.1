package com.smartsorting.client.gui;

import com.smartsorting.gui.ContainerStorage;
import com.smartsorting.net.MsgStorageView;
import com.smartsorting.net.PacketHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import org.lwjgl.glfw.GLFW;

public class GuiStorage extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<ContainerStorage> {

    private static final int BAR_X = 174;
    private static final int BAR_Y = ContainerStorage.GRID_Y;
    private static final int BAR_WIDTH = 12;
    private static final int BAR_HEIGHT = ContainerStorage.ROWS * 18;

    private EditBox search;
    private int offset;
    private boolean dragging;

    public GuiStorage(ContainerStorage menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 194;
        imageHeight = 238;
    }

    @Override
    protected void init() {
        super.init();
        String text = search == null ? "" : search.getValue();
        search = new EditBox(font, leftPos + 9, topPos + 17, 160, 11, Component.empty());
        search.setMaxLength(ContainerStorage.MAX_SEARCH);
        search.setValue(text);
        search.setResponder(value -> {
            offset = 0;
            send();
        });
        addRenderableWidget(search);
    }

    private int maxOffset() {
        return Math.max(0, menu.getRows() - ContainerStorage.ROWS);
    }

    private void scrollTo(int target) {
        int clamped = Math.max(0, Math.min(target, maxOffset()));
        if (clamped != offset) {
            offset = clamped;
            send();
        }
    }

    private void send() {
        PacketHandler.CHANNEL.sendToServer(new MsgStorageView(offset, search.getValue()));
    }

    private int thumbHeight() {
        return Math.max(8, BAR_HEIGHT * ContainerStorage.ROWS / Math.max(ContainerStorage.ROWS, menu.getRows()));
    }



    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        offset = Math.min(offset, maxOffset());
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        GuiStyle.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            GuiStyle.slot(g, leftPos + slot.x, topPos + slot.y, GuiStyle.SLOT);
        }
        int x = leftPos + BAR_X;
        int y = topPos + BAR_Y;
        GuiStyle.inset(g, x, y - 1, BAR_WIDTH, BAR_HEIGHT + 2);
        int thumb = thumbHeight();
        int thumbY = y + (maxOffset() == 0 ? 0 : (BAR_HEIGHT - thumb) * offset / maxOffset());
        g.fill(x + 1, thumbY, x + BAR_WIDTH - 1, thumbY + thumb, maxOffset() == 0 ? GuiStyle.BUTTON : GuiStyle.ACCENT);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, GuiStyle.TEXT, false);
        if (search.getValue().isEmpty() && !search.isFocused()) {
            g.drawString(font, I18n.get("smartsorting.gui.search_items"), 13, 19, GuiStyle.MUTED, false);
        }
        g.drawString(font, playerInventoryTitle, 8, ContainerStorage.PLAYER_Y - 11, GuiStyle.MUTED, false);
    }



    private boolean overBar(double mouseX, double mouseY) {
        double x = mouseX - leftPos - BAR_X;
        double y = mouseY - topPos - BAR_Y;
        return x >= 0 && x < BAR_WIDTH && y >= 0 && y < BAR_HEIGHT;
    }

    private void dragTo(double mouseY) {
        int thumb = thumbHeight();
        double fraction = (mouseY - topPos - BAR_Y - thumb / 2.0) / Math.max(1, BAR_HEIGHT - thumb);
        scrollTo((int) Math.round(fraction * maxOffset()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!search.isMouseOver(mouseX, mouseY)) {
            search.setFocused(false);
        }
        if (button == 0 && overBar(mouseX, mouseY)) {
            dragging = true;
            dragTo(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            dragTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            scrollTo(offset - (int) Math.signum(delta));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        search.tick();
    }
}
