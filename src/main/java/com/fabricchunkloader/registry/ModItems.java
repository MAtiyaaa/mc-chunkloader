package com.fabricchunkloader.registry;

import com.fabricchunkloader.FabricChunkLoader;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item BASIC_CHUNK_LOADER = register("basic_chunk_loader",
            new BlockItem(ModBlocks.BASIC_CHUNK_LOADER, new Item.Settings()));

    public static final Item COMPACT_CHUNK_LOADER = register("compact_chunk_loader",
            new BlockItem(ModBlocks.COMPACT_CHUNK_LOADER, new Item.Settings()));

    public static final Item ADVANCED_CHUNK_LOADER = register("advanced_chunk_loader",
            new BlockItem(ModBlocks.ADVANCED_CHUNK_LOADER, new Item.Settings()));

    public static final Item ELITE_CHUNK_LOADER = register("elite_chunk_loader",
            new BlockItem(ModBlocks.ELITE_CHUNK_LOADER, new Item.Settings()));

    public static final Item ULTIMATE_CHUNK_LOADER = register("ultimate_chunk_loader",
            new BlockItem(ModBlocks.ULTIMATE_CHUNK_LOADER, new Item.Settings()));

    public static final RegistryKey<ItemGroup> CHUNK_LOADER_GROUP = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of(FabricChunkLoader.MOD_ID, "chunk_loaders")
    );

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(FabricChunkLoader.MOD_ID, name), item);
    }

    public static void init() {
        Registry.register(Registries.ITEM_GROUP, CHUNK_LOADER_GROUP,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(ADVANCED_CHUNK_LOADER))
                        .displayName(Text.translatable("itemGroup.chunkloader.chunk_loaders"))
                        .entries((context, entries) -> {
                            entries.add(BASIC_CHUNK_LOADER);
                            entries.add(COMPACT_CHUNK_LOADER);
                            entries.add(ADVANCED_CHUNK_LOADER);
                            entries.add(ELITE_CHUNK_LOADER);
                            entries.add(ULTIMATE_CHUNK_LOADER);
                        })
                        .build()
        );
    }
}
