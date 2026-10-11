package com.smartsorting.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class Button {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    private Supplier<String> label = () -> "";
    private Supplier<List<String>> tooltip = Collections::emptyList;
    private IntSupplier color = () -> 0;
    private BooleanSupplier visible = () -> true;
    private IntConsumer onClick = button -> {
    };

    public Button(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Button label(Supplier<String> label) {
        this.label = label;
        return this;
    }

    public Button tooltip(Supplier<List<String>> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public Button color(IntSupplier color) {
        this.color = color;
        return this;
    }

    public Button visible(BooleanSupplier visible) {
        this.visible = visible;
        return this;
    }

    public Button onClick(IntConsumer onClick) {
        this.onClick = onClick;
        return this;
    }

    public boolean isVisible() {
        return visible.getAsBoolean();
    }

    public boolean contains(double relX, double relY) {
        return relX >= x && relX < x + width && relY >= y && relY < y + height;
    }

    public List<String> getTooltip() {
        return tooltip.get();
    }

    public void click(int mouseButton) {
        onClick.accept(mouseButton);
    }

    public void draw(GuiGraphics g, Font font, int left, int top, boolean hovered) {
        int x0 = left + x;
        int y0 = top + y;
        int fill = color.getAsInt();
        if (fill == 0) {
            fill = hovered ? GuiStyle.BUTTON_HOVER : GuiStyle.BUTTON;
        } else if (hovered) {
            fill = GuiStyle.shade(fill, 1.2F);
        }
        g.fill(x0, y0, x0 + width, y0 + height, GuiStyle.OUTLINE);
        g.fill(x0 + 1, y0 + 1, x0 + width - 1, y0 + height - 1, fill);
        g.fill(x0 + 1, y0 + 1, x0 + width - 1, y0 + 2, GuiStyle.shade(fill, 1.25F));
        String text = font.plainSubstrByWidth(label.get(), width - 4);
        g.drawString(font, text, x0 + (width - font.width(text)) / 2, y0 + (height - 8) / 2 + 1, GuiStyle.TEXT, true);
    }
}
