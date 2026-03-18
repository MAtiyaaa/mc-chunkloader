package com.fabricchunkloader.registry;

import com.fabricchunkloader.FabricChunkLoader;
import com.fabricchunkloader.block.ChunkLoaderBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<ChunkLoaderBlockEntity> CHUNK_LOADER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(FabricChunkLoader.MOD_ID, "chunk_loader_block_entity"),
                    BlockEntityType.Builder.create(
                            ChunkLoaderBlockEntity::new,
                            ModBlocks.BASIC_CHUNK_LOADER,
                            ModBlocks.COMPACT_CHUNK_LOADER,
                            ModBlocks.ADVANCED_CHUNK_LOADER,
                            ModBlocks.ELITE_CHUNK_LOADER,
                            ModBlocks.ULTIMATE_CHUNK_LOADER
                    ).build()
            );

    public static void init() {
        // Static initializer triggers registration
    }
}
