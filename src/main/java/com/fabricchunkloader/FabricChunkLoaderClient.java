package com.fabricchunkloader;

import com.fabricchunkloader.network.ModNetworking;
import com.fabricchunkloader.registry.ModScreenHandlers;
import com.fabricchunkloader.screen.ChunkLoaderScreen;
import com.fabricchunkloader.screen.ManagementScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class FabricChunkLoaderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register screen for the chunk loader screen handler
        HandledScreens.register(ModScreenHandlers.CHUNK_LOADER_SCREEN_HANDLER, ChunkLoaderScreen::new);

        // Register S2C receiver for loader list (opens management screen)
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.LoaderListResponsePayload.ID,
                (payload, context) -> {
                    MinecraftClient client = context.client();
                    client.execute(() -> client.setScreen(new ManagementScreen(payload.entries())));
                });
    }
}
