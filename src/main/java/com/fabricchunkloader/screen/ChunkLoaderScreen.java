package com.fabricchunkloader.screen;

import com.fabricchunkloader.ChunkLoaderTier;
import com.fabricchunkloader.chunk.ChunkLoadingManager;
import com.fabricchunkloader.config.ModConfig;
import com.fabricchunkloader.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

import java.util.Set;

/**
 * Client-side GUI for an individual chunk loader.
 * Shows status, tier, mode, facing, chunk preview grid,
 * and provides controls for toggling and renaming.
 */
public class ChunkLoaderScreen extends HandledScreen<ChunkLoaderScreenHandler> {
    private boolean enabled;
    private boolean centered;
    private String customName;
    private TextFieldWidget nameField;

    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int BORDER_COLOR = 0xFF4A4A6A;
    private static final int ACCENT_COLOR = 0xFF6C63FF;
    private static final int ENABLED_COLOR = 0xFF00E676;
    private static final int DISABLED_COLOR = 0xFFFF5252;
    private static final int CHUNK_LOADED_COLOR = 0x6600E676;
    private static final int CHUNK_CENTER_COLOR = 0xAAFFD600;
    private static final int CHUNK_GRID_COLOR = 0x44FFFFFF;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int LABEL_COLOR = 0xFFAAAAAA;

    public ChunkLoaderScreen(ChunkLoaderScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.enabled = handler.isEnabled();
        this.centered = handler.isCentered();
        this.customName = handler.getCustomName();
        this.backgroundWidth = 256;
        this.backgroundHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        int cx = (width - backgroundWidth) / 2;
        int cy = (height - backgroundHeight) / 2;

        // Toggle enabled button
        addDrawableChild(ButtonWidget.builder(
                Text.literal(enabled ? "Disable" : "Enable"),
                btn -> {
                    enabled = !enabled;
                    btn.setMessage(Text.literal(enabled ? "Disable" : "Enable"));
                    ClientPlayNetworking.send(new ModNetworking.ToggleEnabledPayload(handler.getBlockPos()));
                }
        ).dimensions(cx + 160, cy + 45, 70, 20).build());

        // Toggle mode button
        addDrawableChild(ButtonWidget.builder(
                Text.literal(centered ? "Centered" : "Directional"),
                btn -> {
                    centered = !centered;
                    btn.setMessage(Text.literal(centered ? "Centered" : "Directional"));
                    ClientPlayNetworking.send(new ModNetworking.ToggleModePayload(handler.getBlockPos()));
                }
        ).dimensions(cx + 160, cy + 70, 70, 20).build());

        // Name field
        nameField = new TextFieldWidget(textRenderer, cx + 26, cy + 170, 140, 16, Text.literal("Name"));
        nameField.setMaxLength(64);
        nameField.setText(customName != null ? customName : "");
        addDrawableChild(nameField);

        // Rename button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Rename"),
                btn -> {
                    String newName = nameField.getText().trim();
                    customName = newName;
                    ClientPlayNetworking.send(new ModNetworking.RenameLoaderPayload(handler.getBlockPos(), newName));
                }
        ).dimensions(cx + 170, cy + 167, 55, 20).build());

        // Manage all button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Manage All"),
                btn -> ClientPlayNetworking.send(new ModNetworking.RequestLoaderListPayload(0))
        ).dimensions(cx + 26, cy + 193, 90, 20).build());

        // Close button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                btn -> close()
        ).dimensions(cx + 170, cy + 193, 55, 20).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int cx = (width - backgroundWidth) / 2;
        int cy = (height - backgroundHeight) / 2;

        // Main background
        context.fill(cx, cy, cx + backgroundWidth, cy + backgroundHeight, BG_COLOR);
        // Border
        drawBorder(context, cx, cy, backgroundWidth, backgroundHeight, BORDER_COLOR);

        // Title
        ChunkLoaderTier tier = handler.getTier();
        String displayName = (customName != null && !customName.isEmpty()) ? customName : tier.getDisplayName();
        int titleWidth = textRenderer.getWidth(displayName);
        context.drawText(textRenderer, displayName, cx + (backgroundWidth - titleWidth) / 2, cy + 8, ACCENT_COLOR, true);

        // Separator
        context.fill(cx + 10, cy + 22, cx + backgroundWidth - 10, cy + 23, BORDER_COLOR);

        // Status info
        String statusText = enabled ? "ENABLED" : "DISABLED";
        int statusColor = enabled ? ENABLED_COLOR : DISABLED_COLOR;
        context.drawText(textRenderer, "Status:", cx + 15, cy + 30, LABEL_COLOR, false);
        context.drawText(textRenderer, statusText, cx + 15, cy + 42, statusColor, true);

        context.drawText(textRenderer, "Tier:", cx + 15, cy + 56, LABEL_COLOR, false);
        context.drawText(textRenderer, tier.getDisplayName() + " (" + tier.getSize() + "x" + tier.getSize() + ")", cx + 15, cy + 68, TEXT_COLOR, false);

        context.drawText(textRenderer, "Mode:", cx + 15, cy + 82, LABEL_COLOR, false);
        context.drawText(textRenderer, centered ? "Centered" : "Directional", cx + 15, cy + 94, TEXT_COLOR, false);

        Direction facing = handler.getFacing();
        context.drawText(textRenderer, "Facing:", cx + 100, cy + 82, LABEL_COLOR, false);
        context.drawText(textRenderer, capitalize(facing.getName()), cx + 100, cy + 94, TEXT_COLOR, false);

        if (handler.getOwnerName() != null && !handler.getOwnerName().isEmpty()) {
            context.drawText(textRenderer, "Owner: " + handler.getOwnerName(), cx + 15, cy + 108, LABEL_COLOR, false);
        }

        // Separator
        context.fill(cx + 10, cy + 120, cx + backgroundWidth - 10, cy + 121, BORDER_COLOR);

        // Chunk preview grid
        context.drawText(textRenderer, "Loaded Chunks Preview:", cx + 15, cy + 125, LABEL_COLOR, false);
        drawChunkPreview(context, cx + 15, cy + 137, 226, 25);

        // Name label
        context.drawText(textRenderer, "Name:", cx + 15, cy + 173, LABEL_COLOR, false);
    }

    /**
     * Draws a minimap-style grid showing which chunks are loaded.
     * The block's chunk is highlighted distinctly.
     */
    private void drawChunkPreview(DrawContext context, int px, int py, int maxWidth, int maxHeight) {
        ChunkLoaderTier tier = handler.getTier();
        ModConfig config = ModConfig.get();
        int size = config.getEffectiveSize(tier);

        // Build a temporary entry to calculate chunks
        ChunkLoadingManager.LoaderEntry tempEntry = new ChunkLoadingManager.LoaderEntry();
        tempEntry.pos = handler.getBlockPos();
        tempEntry.tier = tier;
        tempEntry.centered = centered;
        tempEntry.facing = handler.getFacing();
        tempEntry.enabled = true;

        Set<ChunkPos> chunks = ChunkLoadingManager.calculateLoadedChunks(tempEntry);
        if (chunks.isEmpty()) return;

        int minCX = Integer.MAX_VALUE, maxCX = Integer.MIN_VALUE;
        int minCZ = Integer.MAX_VALUE, maxCZ = Integer.MIN_VALUE;
        for (ChunkPos cp : chunks) {
            minCX = Math.min(minCX, cp.x);
            maxCX = Math.max(maxCX, cp.x);
            minCZ = Math.min(minCZ, cp.z);
            maxCZ = Math.max(maxCZ, cp.z);
        }

        int gridW = maxCX - minCX + 1;
        int gridH = maxCZ - minCZ + 1;

        int cellSize = Math.min(maxWidth / gridW, maxHeight);
        cellSize = Math.max(2, Math.min(cellSize, 12));

        int totalW = gridW * cellSize;
        int totalH = gridH * cellSize;
        int offsetX = px + (maxWidth - totalW) / 2;
        int offsetY = py;

        int blockChunkX = handler.getBlockPos().getX() >> 4;
        int blockChunkZ = handler.getBlockPos().getZ() >> 4;

        for (ChunkPos cp : chunks) {
            int rx = cp.x - minCX;
            int rz = cp.z - minCZ;
            int x1 = offsetX + rx * cellSize;
            int y1 = offsetY + rz * cellSize;
            int x2 = x1 + cellSize - 1;
            int y2 = y1 + cellSize - 1;

            boolean isCenter = (cp.x == blockChunkX && cp.z == blockChunkZ);
            context.fill(x1, y1, x2, y2, isCenter ? CHUNK_CENTER_COLOR : CHUNK_LOADED_COLOR);
            // Grid lines
            context.fill(x1, y1, x2, y1 + 1, CHUNK_GRID_COLOR);
            context.fill(x1, y1, x1 + 1, y2, CHUNK_GRID_COLOR);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Don't draw the default title/inventory labels
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }
}
