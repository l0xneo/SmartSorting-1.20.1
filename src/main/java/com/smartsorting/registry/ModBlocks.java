package com.smartsorting.registry;

import com.smartsorting.SmartSorting;
import com.smartsorting.block.BlockChestLink;
import com.smartsorting.block.BlockChestMover;
import com.smartsorting.block.BlockExporter;
import com.smartsorting.block.BlockImporter;
import com.smartsorting.block.BlockPipe;
import com.smartsorting.tile.TileChestMover;
import com.smartsorting.tile.TileExporter;
import com.smartsorting.tile.TileImporter;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SmartSorting.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TILES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SmartSorting.MOD_ID);

    public static final RegistryObject<Block> PIPE = BLOCKS.register("pipe", BlockPipe::new);
    public static final RegistryObject<Block> CHEST_EXPORTER = BLOCKS.register("chest_exporter", BlockExporter::new);
    public static final RegistryObject<Block> CHEST_IMPORTER = BLOCKS.register("chest_importer", BlockImporter::new);
    public static final RegistryObject<Block> CHEST_LINK = BLOCKS.register("chest_link", BlockChestLink::new);
    public static final RegistryObject<Block> CHEST_MOVER = BLOCKS.register("chest_mover", BlockChestMover::new);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<TileExporter>> EXPORTER_TILE = TILES.register("chest_exporter",
            () -> BlockEntityType.Builder.of(TileExporter::new, CHEST_EXPORTER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<TileImporter>> IMPORTER_TILE = TILES.register("chest_importer",
            () -> BlockEntityType.Builder.of(TileImporter::new, CHEST_IMPORTER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<TileChestMover>> MOVER_TILE = TILES.register("chest_mover",
            () -> BlockEntityType.Builder.of(TileChestMover::new, CHEST_MOVER.get()).build(null));

    private ModBlocks() {
    }
}
