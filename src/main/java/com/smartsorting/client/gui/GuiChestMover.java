package com.smartsorting.client.gui;

import com.smartsorting.block.BlockChestMover;
import com.smartsorting.block.MoverSide;
import com.smartsorting.gui.ContainerMover;
import com.smartsorting.net.MsgLinkSide;
import com.smartsorting.net.PacketHandler;
import com.smartsorting.tile.TileChestMover;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;

public class GuiChestMover extends GuiBase<ContainerMover> {

    private static final int CELL_W = 50;
    private static final int CELL_H = 22;
    private static final int GAP = 4;
    private static final int BOX_Y = 18;
    private static final int BOX_H = 3 * CELL_H + 2 * GAP + 16;
    private static final int COMPASS_X = 8;
    private static final int COMPASS_W = 3 * CELL_W + 2 * GAP + 10;
    private static final int GRID_X = COMPASS_X + 5;
    private static final int GRID_Y = BOX_Y + 8;
    private static final int COLUMN_BOX_X = COMPASS_X + COMPASS_W + 6;
    private static final int COLUMN_BOX_W = 50;
    private static final int COLUMN_W = 44;
    private static final int COLUMN_X = COLUMN_BOX_X + (COLUMN_BOX_W - COLUMN_W) / 2;

    private final BlockPos pos;
    private final Direction facing;

    public GuiChestMover(ContainerMover menu, Inventory inventory, Component title) {
        super(menu, inventory, title, COLUMN_BOX_X + COLUMN_BOX_W + 8, ContainerMover.PLAYER_Y + 58 + 24);
        this.pos = menu.getTile().getBlockPos();

        this.facing = Minecraft.getInstance().player.getDirection();
        addSide(facing, GRID_X + CELL_W + GAP, GRID_Y);
        addSide(facing.getCounterClockWise(), GRID_X, GRID_Y + CELL_H + GAP);
        addSide(facing.getClockWise(), GRID_X + 2 * (CELL_W + GAP), GRID_Y + CELL_H + GAP);
        addSide(facing.getOpposite(), GRID_X + CELL_W + GAP, GRID_Y + 2 * (CELL_H + GAP));
        addSide(Direction.UP, COLUMN_X, GRID_Y);
        addSide(Direction.DOWN, COLUMN_X, GRID_Y + 2 * (CELL_H + GAP));
    }

    private void addSide(Direction side, int x, int y) {
        String name = tr("smartsorting.side." + side.getName());
        addButton(new Button(x, y, side.getAxis().isVertical() ? COLUMN_W : CELL_W, CELL_H))
                .label(() -> name)
                .color(() -> switch (mode(side)) {
                    case TAKE -> GuiStyle.RED;
                    case GIVE -> GuiStyle.GREEN;
                    default -> 0;
                })
                .onClick(b -> PacketHandler.CHANNEL.sendToServer(new MsgLinkSide(pos, side.get3DDataValue())));
    }

    private MoverSide mode(Direction side) {
        BlockState state = minecraft.level.getBlockState(pos);
        return state.getBlock() instanceof BlockChestMover ? BlockChestMover.mode(state, side) : MoverSide.OFF;
    }

    @Override
    protected void drawBackground(GuiGraphics g, int mouseX, int mouseY) {
        GuiStyle.inset(g, leftPos + COMPASS_X, topPos + BOX_Y, COMPASS_W, BOX_H);
        GuiStyle.inset(g, leftPos + COLUMN_BOX_X, topPos + BOX_Y, COLUMN_BOX_W, BOX_H);

        int cx = leftPos + GRID_X + CELL_W + GAP;
        int cy = topPos + GRID_Y + CELL_H + GAP;
        GuiStyle.slot(g, cx + 16, cy + 2, GuiStyle.SLOT_GHOST);
    }

    @Override
    protected void drawForeground(GuiGraphics g, int relX, int relY) {
        drawText(g, title.getString(), 8, 6, GuiStyle.TEXT);
        int cx = GRID_X + CELL_W + GAP;
        int cy = GRID_Y + CELL_H + GAP;
        drawText(g, "↑", cx + 25 - font.width("↑") / 2, cy + 7, GuiStyle.YELLOW);
        String level = tr("smartsorting.gui.link_level");
        drawText(g, level, COLUMN_BOX_X + (COLUMN_BOX_W - font.width(level)) / 2, GRID_Y + CELL_H + GAP + 7, GuiStyle.MUTED);
        float speed = TileChestMover.travelSpeed(menu.getSlot(0).getItem()) * 20;
        drawText(g, tr("smartsorting.gui.travel", String.format("%.2f", speed).replaceAll("\\.?0+$", "")),
                ContainerMover.UPGRADE_X + 22, ContainerMover.UPGRADE_Y + 5, GuiStyle.MUTED);
    }
}
