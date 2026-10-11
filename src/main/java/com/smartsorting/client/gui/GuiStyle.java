package com.smartsorting.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public final class GuiStyle {

    public static final int OUTLINE = 0xFF0E1013;
    public static final int PANEL = 0xFF262A31;
    public static final int PANEL_LIGHT = 0xFF353B44;
    public static final int INSET = 0xFF1B1E23;
    public static final int SLOT = 0xFF14171B;
    public static final int SLOT_GHOST = 0xFF1A2733;
    public static final int BUTTON = 0xFF3A404A;
    public static final int BUTTON_HOVER = 0xFF4A515D;
    public static final int TEXT = 0xFFE6E6E6;
    public static final int MUTED = 0xFF9AA3AD;
    public static final int ACCENT = 0xFF3FA9F5;
    public static final int GREEN = 0xFF3FAE4F;
    public static final int RED = 0xFFD2443C;
    public static final int YELLOW = 0xFFE0B13A;

    private GuiStyle() {
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, PANEL_LIGHT);
    }

    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, INSET);
    }

    public static void slot(GuiGraphics g, int x, int y, int fill) {
        g.fill(x - 1, y - 1, x + 17, y + 17, OUTLINE);
        g.fill(x, y, x + 16, y + 16, fill);
    }

    public static int shade(int argb, float factor) {
        int r = Math.min(255, (int) (((argb >> 16) & 255) * factor));
        int gr = Math.min(255, (int) (((argb >> 8) & 255) * factor));
        int b = Math.min(255, (int) ((argb & 255) * factor));
        return (argb & 0xFF000000) | (r << 16) | (gr << 8) | b;
    }
}
