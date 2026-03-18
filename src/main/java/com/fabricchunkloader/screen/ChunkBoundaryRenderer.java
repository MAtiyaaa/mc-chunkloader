package com.fabricchunkloader.screen;

import com.fabricchunkloader.ChunkLoaderTier;
import com.fabricchunkloader.block.ChunkLoaderBlock;
import com.fabricchunkloader.block.ChunkLoaderBlockEntity;
import com.fabricchunkloader.chunk.ChunkLoadingManager;
import com.fabricchunkloader.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

/**
 * Renders chunk boundary outlines when the player is looking at an active chunk loader.
 * Uses the Fabric rendering API to draw lines in world space.
 */
public class ChunkBoundaryRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ChunkBoundaryRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit)) return;

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChunkLoaderBlock clb)) return;
        if (!state.get(ChunkLoaderBlock.ACTIVE)) return;

        ChunkLoaderTier tier = clb.getTier();
        Direction facing = state.get(ChunkLoaderBlock.FACING);

        // Build a temp entry for chunk calculation
        ChunkLoadingManager.LoaderEntry tempEntry = new ChunkLoadingManager.LoaderEntry();
        tempEntry.pos = pos;
        tempEntry.tier = tier;
        tempEntry.centered = true; // Default assumption; we don't know server state on client
        tempEntry.facing = facing;
        tempEntry.enabled = true;

        Set<ChunkPos> chunks = ChunkLoadingManager.calculateLoadedChunks(tempEntry);
        if (chunks.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d camera = context.camera().getPos();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        // Draw outlines for the loaded chunk area
        int minCX = Integer.MAX_VALUE, maxCX = Integer.MIN_VALUE;
        int minCZ = Integer.MAX_VALUE, maxCZ = Integer.MIN_VALUE;
        for (ChunkPos cp : chunks) {
            minCX = Math.min(minCX, cp.x);
            maxCX = Math.max(maxCX, cp.x);
            minCZ = Math.min(minCZ, cp.z);
            maxCZ = Math.max(maxCZ, cp.z);
        }

        float y1 = pos.getY() - 1;
        float y2 = pos.getY() + 3;
        float r = 0.2f, g = 0.9f, b = 0.4f, a = 0.8f;

        // Outer boundary of loaded area
        float x1 = minCX * 16;
        float z1 = minCZ * 16;
        float x2 = (maxCX + 1) * 16;
        float z2 = (maxCZ + 1) * 16;

        // Draw outer rectangle at block Y level
        drawLine(matrices, lines, x1, y1, z1, x2, y1, z1, r, g, b, a);
        drawLine(matrices, lines, x2, y1, z1, x2, y1, z2, r, g, b, a);
        drawLine(matrices, lines, x2, y1, z2, x1, y1, z2, r, g, b, a);
        drawLine(matrices, lines, x1, y1, z2, x1, y1, z1, r, g, b, a);

        drawLine(matrices, lines, x1, y2, z1, x2, y2, z1, r, g, b, a);
        drawLine(matrices, lines, x2, y2, z1, x2, y2, z2, r, g, b, a);
        drawLine(matrices, lines, x2, y2, z2, x1, y2, z2, r, g, b, a);
        drawLine(matrices, lines, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // Vertical edges
        drawLine(matrices, lines, x1, y1, z1, x1, y2, z1, r, g, b, a);
        drawLine(matrices, lines, x2, y1, z1, x2, y2, z1, r, g, b, a);
        drawLine(matrices, lines, x2, y1, z2, x2, y2, z2, r, g, b, a);
        drawLine(matrices, lines, x1, y1, z2, x1, y2, z2, r, g, b, a);

        matrices.pop();
    }

    private static void drawLine(MatrixStack matrices, VertexConsumer consumer,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;
        dx /= len; dy /= len; dz /= len;

        consumer.vertex(entry, x1, y1, z1).color(r, g, b, a).normal(entry, dx, dy, dz);
        consumer.vertex(entry, x2, y2, z2).color(r, g, b, a).normal(entry, dx, dy, dz);
    }
}
