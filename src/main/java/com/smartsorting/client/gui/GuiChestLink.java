package com.smartsorting.client.gui;

import com.smartsorting.block.BlockChestLink;
import com.smartsorting.net.MsgLinkSide;
import com.smartsorting.net.PacketHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

public class GuiChestLink extends Screen {

    private static final int WIDTH = 240;
    private static final int CELL_W = 50;
    private static final int CELL_H = 22;
    private static final int GAP = 4;
    private static final int BOX_Y = 22;
    private static final int BOX_H = 3 * CELL_H + 2 * GAP + 16;
    private static final int HEIGHT = BOX_Y + BOX_H + 8;
    private static final int COMPASS_X = 8;
    private static final int COMPASS_W = 3 * CELL_W + 2 * GAP + 10;
    private static final int GRID_X = COMPASS_X + 5;
    private static final int GRID_Y = BOX_Y + 8;
    private static final int COLUMN_BOX_X = COMPASS_X + COMPASS_W + 6;
    private static final int COLUMN_BOX_W = WIDTH - 8 - COLUMN_BOX_X;
    private static final int COLUMN_W = 44;
    private static final int COLUMN_X = COLUMN_BOX_X + (COLUMN_BOX_W - COLUMN_W) / 2;

    private final BlockPos pos;
    private final Map<Direction, Button> buttons = new EnumMap<>(Direction.class);
    private int left;
    private int top;

    public GuiChestLink(BlockPos pos) {
        super(Component.translatable("block.smartsorting.chest_link"));
        this.pos = pos;
    }

    private void addSide(Direction side, int x, int y) {
        String name = I18n.get("smartsorting.side." + side.getName());
        buttons.put(side, new Button(x, y, side.getAxis().isVertical() ? COLUMN_W : CELL_W, CELL_H)
                .label(() -> name)
                .color(() -> open(side) ? GuiStyle.GREEN : 0)
                .onClick(b -> PacketHandler.CHANNEL.sendToServer(new MsgLinkSide(pos, side.get3DDataValue()))));
    }

    private boolean open(Direction side) {
        BlockState state = minecraft.level.getBlockState(pos);
        return state.getBlock() instanceof BlockChestLink && BlockChestLink.isOpen(state, side);
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;

        Direction facing = minecraft.player.getDirection();
        buttons.clear();
        addSide(facing, GRID_X + CELL_W + GAP, GRID_Y);
        addSide(facing.getCounterClockWise(), GRID_X, GRID_Y + CELL_H + GAP);
        addSide(facing.getClockWise(), GRID_X + 2 * (CELL_W + GAP), GRID_Y + CELL_H + GAP);
        addSide(facing.getOpposite(), GRID_X + CELL_W + GAP, GRID_Y + 2 * (CELL_H + GAP));

        addSide(Direction.UP, COLUMN_X, GRID_Y);
        addSide(Direction.DOWN, COLUMN_X, GRID_Y + 2 * (CELL_H + GAP));
    }

    @Override
    public void tick() {

        if (!(minecraft.level.getBlockState(pos).getBlock() instanceof BlockChestLink)
                || minecraft.player.distanceToSqr(pos.getCenter()) > 64) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        GuiStyle.panel(g, left, top, WIDTH, HEIGHT);
        g.drawString(font, title, left + 10, top + 9, GuiStyle.TEXT, false);
        GuiStyle.inset(g, left + COMPASS_X, top + BOX_Y, COMPASS_W, BOX_H);
        GuiStyle.inset(g, left + COLUMN_BOX_X, top + BOX_Y, COLUMN_BOX_W, BOX_H);
        String level = I18n.get("smartsorting.gui.link_level");
        g.drawString(font, level, left + COLUMN_BOX_X + (COLUMN_BOX_W - font.width(level)) / 2,
                top + GRID_Y + CELL_H + GAP + 7, GuiStyle.MUTED, false);

        int cx = left + GRID_X + CELL_W + GAP;
        int cy = top + GRID_Y + CELL_H + GAP;
        GuiStyle.slot(g, cx + 16, cy + 2, GuiStyle.SLOT_GHOST);
        g.drawString(font, "↑", cx + 25 - font.width("↑") / 2, cy + 7, GuiStyle.YELLOW, false);

        for (Button button : buttons.values()) {
            button.draw(g, font, left, top, button.contains(mouseX - left, mouseY - top));
        }
        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (Button button : buttons.values()) {
            if (button.contains(mouseX - left, mouseY - top)) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                button.click(mouseButton);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
