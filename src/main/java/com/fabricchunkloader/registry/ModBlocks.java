package com.fabricchunkloader.registry;

import com.fabricchunkloader.ChunkLoaderTier;
import com.fabricchunkloader.block.ChunkLoaderBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block BASIC_CHUNK_LOADER = register("basic_chunk_loader",
            new ChunkLoaderBlock(baseSettings().mapColor(MapColor.IRON_GRAY), ChunkLoaderTier.BASIC));

    public static final Block COMPACT_CHUNK_LOADER = register("compact_chunk_loader",
            new ChunkLoaderBlock(baseSettings().mapColor(MapColor.GOLD), ChunkLoaderTier.COMPACT));

    public static final Block ADVANCED_CHUNK_LOADER = register("advanced_chunk_loader",
            new ChunkLoaderBlock(baseSettings().mapColor(MapColor.DIAMOND_BLUE).luminance(state ->
                    state.get(ChunkLoaderBlock.ACTIVE) ? 10 : 0), ChunkLoaderTier.ADVANCED));

    public static final Block ELITE_CHUNK_LOADER = register("elite_chunk_loader",
            new ChunkLoaderBlock(baseSettings().mapColor(MapColor.EMERALD_GREEN).luminance(state ->
                    state.get(ChunkLoaderBlock.ACTIVE) ? 12 : 0), ChunkLoaderTier.ELITE));

    public static final Block ULTIMATE_CHUNK_LOADER = register("ultimate_chunk_loader",
            new ChunkLoaderBlock(baseSettings().mapColor(MapColor.PURPLE).luminance(state ->
                    state.get(ChunkLoaderBlock.ACTIVE) ? 15 : 0), ChunkLoaderTier.ULTIMATE));

    private static AbstractBlock.Settings baseSettings() {
        return AbstractBlock.Settings.create()
                .strength(5.0f, 1200.0f)
                .requiresTool()
                .sounds(BlockSoundGroup.METAL);
    }

    private static Block register(String name, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of("chunkloader", name), block);
    }

    public static void init() {
        // Static initializer triggers field registration
    }
}
