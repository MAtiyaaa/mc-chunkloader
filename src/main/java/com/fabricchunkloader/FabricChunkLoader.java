package com.fabricchunkloader;

import com.fabricchunkloader.chunk.ChunkLoadingManager;
import com.fabricchunkloader.command.ModCommands;
import com.fabricchunkloader.config.ModConfig;
import com.fabricchunkloader.network.ModNetworking;
import com.fabricchunkloader.registry.ModBlockEntities;
import com.fabricchunkloader.registry.ModBlocks;
import com.fabricchunkloader.registry.ModItems;
import com.fabricchunkloader.registry.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricChunkLoader implements ModInitializer {
    public static final String MOD_ID = "chunkloader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Fabric Chunk Loader");

        ModConfig.load();

        // Register blocks, items, block entities, screen handlers
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModScreenHandlers.init();

        // Register networking payloads
        ModNetworking.registerC2SPayloads();
        ModNetworking.registerS2CPayloads();
        ModNetworking.registerServerReceivers();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ModCommands.register(dispatcher));

        // Re-enable chunk loading when the server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Re-enabling chunk loaders...");
            for (ServerWorld world : server.getWorlds()) {
                ChunkLoadingManager manager = ChunkLoadingManager.get(world);
                manager.onServerStart();
                LOGGER.info("  {} - {} loaders active",
                        world.getRegistryKey().getValue(),
                        manager.getLoaderCount());
            }
        });

        LOGGER.info("Fabric Chunk Loader initialized successfully");
    }
}
