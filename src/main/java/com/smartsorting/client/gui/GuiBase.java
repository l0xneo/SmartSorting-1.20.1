package com.smartsorting.client.gui;

import com.smartsorting.gui.ContainerBase;
import com.smartsorting.net.MsgButton;
import com.smartsorting.net.PacketHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class GuiBase<C extends ContainerBase<?>> extends AbstractContainerScreen<C> {

    protected final List<Button> buttons = new ArrayList<>();

    protected GuiBase(C container, Inventory inventory, Component title, int width, int height) {
        super(container, inventory, title);
        this.imageWidth = width;
        this.imageHeight = height;
    }

    protected Button addButton(Button button) {
        buttons.add(button);
        return button;
    }

    protected void sendButton(int id, int value) {
        PacketHandler.CHANNEL.sendToServer(new MsgButton(menu.getTile().getBlockPos(), id, value));
    }

    protected void playClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    protected static String tr(String key, Object... args) {
        return I18n.get(key, args);
    }

    protected static List<String> lines(String... lines) {
        return Arrays.asList(lines);
    }

    protected void drawTooltip(GuiGraphics g, List<String> lines, int mouseX, int mouseY) {
        g.renderComponentTooltip(font, lines.stream().<Component>map(Component::literal).toList(), mouseX, mouseY);
    }



    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);
        for (Button button : buttons) {
            if (button.isVisible() && button.contains(mouseX - leftPos, mouseY - topPos) && !button.getTooltip().isEmpty()) {
                drawTooltip(g, button.getTooltip(), mouseX, mouseY);
            }
        }
        drawExtraTooltips(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        GuiStyle.panel(g, leftPos, topPos, imageWidth, imageHeight);
        drawBackground(g, mouseX, mouseY);
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                GuiStyle.slot(g, leftPos + slot.x, topPos + slot.y, slotColor(slot));
            }
        }
        for (Button button : buttons) {
            if (button.isVisible()) {
                button.draw(g, font, leftPos, topPos, button.contains(mouseX - leftPos, mouseY - topPos));
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        drawForeground(g, mouseX - leftPos, mouseY - topPos);
    }

    protected void drawBackground(GuiGraphics g, int mouseX, int mouseY) {
    }

    protected void drawForeground(GuiGraphics g, int relX, int relY) {
    }

    protected void drawExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
    }

    protected int slotColor(Slot slot) {
        return GuiStyle.SLOT;
    }

    protected void drawText(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }



    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (Button button : buttons) {
            if (button.isVisible() && button.contains(mouseX - leftPos, mouseY - topPos)) {
                playClick();
                button.click(mouseButton);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0 && onScroll(delta > 0 ? -1 : 1, (int) mouseX - leftPos, (int) mouseY - topPos)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    protected boolean onScroll(int direction, int relX, int relY) {
        return false;
    }
}
