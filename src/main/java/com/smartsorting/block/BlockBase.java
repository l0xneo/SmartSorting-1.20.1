package com.smartsorting.block;

import com.smartsorting.routing.NetworkVersion;
import com.smartsorting.tile.IHasContents;
import com.smartsorting.transport.TransitManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BlockBase extends Block {

    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    protected BlockBase(float hardness) {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(hardness, 6.0F)
                .noOcclusion().pushReaction(PushReaction.BLOCK));
    }

    protected void buildShapes() {
        for (BlockState state : getStateDefinition().getPossibleStates()) {
            shapes.put(state, makeShape(state));
        }
    }

    protected abstract VoxelShape makeShape(BlockState state);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state);
    }



    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof MenuProvider menu)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, menu, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public boolean wrenchRotate(Level level, BlockPos pos, BlockState state, Direction clickedSide) {
        return false;
    }

    public void dismantle(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        List<ItemStack> items = new ArrayList<>();
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof IHasContents contents) {
            contents.takeContents(items);
        }
        items.addAll(getDrops(state, level, pos, tile, player, player.getMainHandItem()));
        level.removeBlock(pos, false);
        for (ItemStack stack : items) {
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }
    }



    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        NetworkVersion.bump();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            TransitManager.get(serverLevel).onBroken(serverLevel, pos);
        }
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof IHasContents contents) {
            List<ItemStack> items = new ArrayList<>();
            contents.takeContents(items);
            for (ItemStack stack : items) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        NetworkVersion.bump();
        super.onRemove(state, level, pos, newState, moving);
    }
}
