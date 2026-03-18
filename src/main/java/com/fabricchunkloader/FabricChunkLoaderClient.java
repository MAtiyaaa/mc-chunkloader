package com.fabricchunkloader;

import com.fabricchunkloader.network.ModNetworking;
import com.fabricchunkloader.registry.ModScreenHandlers;
import com.fabricchunkloader.screen.ChunkBoundaryRenderer;
import com.fabricchunkloader.screen.ChunkLoaderScreen;
import com.fabricchunkloader.screen.ManagementScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class FabricChunkLoaderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.CHUNK_LOADER_SCREEN_HANDLER, ChunkLoaderScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.LoaderListResponsePayload.ID,
                (payload, context) -> {
                    MinecraftClient client = context.client();
                    client.execute(() -> client.setScreen(new ManagementScreen(payload.entries())));
                });

        // Register chunk boundary outline rendering (visible when looking at active loaders)
        ChunkBoundaryRenderer.register();
    }
}
