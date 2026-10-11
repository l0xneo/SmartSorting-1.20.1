package com.smartsorting.block;

import com.smartsorting.client.ClientHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class BlockChestLink extends BlockBase {

    public static final BooleanProperty[] SIDES = BlockPipe.CONNECTED;

    public BlockChestLink() {
        super(2.0F);
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : SIDES) {
            state = state.setValue(side, false);
        }
        registerDefaultState(state);
        buildShapes();
    }

    public static boolean isOpen(BlockState state, Direction side) {
        return state.getValue(SIDES[side.get3DDataValue()]);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.values()) {
            boolean chest = context.getLevel().getBlockEntity(context.getClickedPos().relative(side)) instanceof ChestBlockEntity;
            state = state.setValue(SIDES[side.get3DDataValue()], chest);
        }
        return state;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openChestLink(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape makeShape(BlockState state) {
        VoxelShape shape = Shapes.cube(5, 11);
        for (Direction side : Direction.values()) {
            if (isOpen(state, side)) {
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, Shapes.along(side, 11, 16, 6, 10));
            }
        }
        return shape;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation) {
        return Shapes.turnSides(state, SIDES, rotation::rotate);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return Shapes.turnSides(state, SIDES, mirror::mirror);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIDES);
    }
}
