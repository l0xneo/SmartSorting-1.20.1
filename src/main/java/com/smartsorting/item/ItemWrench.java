package com.smartsorting.item;

import com.smartsorting.block.BlockBase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class ItemWrench extends Item {

    public ItemWrench() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);
        boolean ours = state.getBlock() instanceof BlockBase;
        if (player == null || (!ours && player.isShiftKeyDown())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        if (state.getBlock() instanceof BlockBase base) {
            if (player.isShiftKeyDown()) {
                base.dismantle((ServerLevel) level, pos, state, player);
            } else {
                base.wrenchRotate(level, pos, state, context.getClickedFace());
            }
            return InteractionResult.SUCCESS;
        }
        BlockState rotated = state.rotate(level, pos, Rotation.CLOCKWISE_90);
        if (rotated == state) {
            return InteractionResult.PASS;
        }
        level.setBlock(pos, rotated, 3);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.add(getDescriptionId(), tooltip);
    }
}
