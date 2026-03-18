package com.fabricchunkloader.registry;

import com.fabricchunkloader.FabricChunkLoader;
import com.fabricchunkloader.network.ChunkLoaderScreenData;
import com.fabricchunkloader.screen.ChunkLoaderScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<ChunkLoaderScreenHandler> CHUNK_LOADER_SCREEN_HANDLER =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of(FabricChunkLoader.MOD_ID, "chunk_loader"),
                    new ExtendedScreenHandlerType<>(ChunkLoaderScreenHandler::new, ChunkLoaderScreenData.PACKET_CODEC)
            );

    public static void init() {
        // Static initializer triggers registration
    }
}
