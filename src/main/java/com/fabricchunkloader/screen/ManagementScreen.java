package com.fabricchunkloader.screen;

import com.fabricchunkloader.ChunkLoaderTier;
import com.fabricchunkloader.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * Management screen showing all the player's chunk loaders across dimensions.
 * Opened via /chunkloader manage or via the "Manage All" button in a loader's GUI.
 */
public class ManagementScreen extends Screen {
    private final List<ModNetworking.LoaderListEntry> entries;
    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 42;
    private static final int VISIBLE_ENTRIES = 4;

    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int BORDER_COLOR = 0xFF4A4A6A;
    private static final int ACCENT_COLOR = 0xFF6C63FF;
    private static final int ENTRY_BG = 0xAA252540;
    private static final int ENTRY_BORDER = 0xFF3A3A5A;
    private static final int ENABLED_COLOR = 0xFF00E676;
    private static final int DISABLED_COLOR = 0xFFFF5252;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int LABEL_COLOR = 0xFFAAAAAA;

    public ManagementScreen(List<ModNetworking.LoaderListEntry> entries) {
        super(Text.literal("Chunk Loader Manager"));
        this.entries = entries;
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearChildren();

        int panelWidth = 300;
        int panelHeight = 30 + VISIBLE_ENTRIES * ENTRY_HEIGHT + 30;
        int cx = (width - panelWidth) / 2;
        int cy = (height - panelHeight) / 2;

        // Scroll buttons
        if (entries.size() > VISIBLE_ENTRIES) {
            addDrawableChild(ButtonWidget.builder(Text.literal("\u25B2"), btn -> {
                if (scrollOffset > 0) { scrollOffset--; rebuildButtons(); }
            }).dimensions(cx + panelWidth - 25, cy + 28, 20, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("\u25BC"), btn -> {
                if (scrollOffset < entries.size() - VISIBLE_ENTRIES) { scrollOffset++; rebuildButtons(); }
            }).dimensions(cx + panelWidth - 25, cy + panelHeight - 48, 20, 20).build());
        }

        // Toggle buttons for each visible entry
        int end = Math.min(scrollOffset + VISIBLE_ENTRIES, entries.size());
        for (int i = scrollOffset; i < end; i++) {
            final int index = i;
            int entryY = cy + 30 + (i - scrollOffset) * ENTRY_HEIGHT;
            ModNetworking.LoaderListEntry entry = entries.get(i);

            addDrawableChild(ButtonWidget.builder(
                    Text.literal(entry.enabled() ? "Disable" : "Enable"),
                    btn -> {
                        ClientPlayNetworking.send(new ModNetworking.ToggleEnabledPayload(entry.pos()));
                        // Optimistically update local state
                        ModNetworking.LoaderListEntry updated = new ModNetworking.LoaderListEntry(
                                entry.pos(), entry.dimension(), entry.tierOrdinal(),
                                !entry.enabled(), entry.centered(), entry.facingHorizontal(),
                                entry.customName(), entry.ownerName()
                        );
                        entries.set(index, updated);
                        rebuildButtons();
                    }
            ).dimensions(cx + panelWidth - 80, entryY + 18, 50, 18).build());
        }

        // Close button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                btn -> close()
        ).dimensions(cx + panelWidth / 2 - 30, cy + panelHeight - 25, 60, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = 300;
        int panelHeight = 30 + VISIBLE_ENTRIES * ENTRY_HEIGHT + 30;
        int cx = (width - panelWidth) / 2;
        int cy = (height - panelHeight) / 2;

        // Background
        context.fill(cx, cy, cx + panelWidth, cy + panelHeight, BG_COLOR);
        drawBorder(context, cx, cy, panelWidth, panelHeight, BORDER_COLOR);

        // Title
        String title = "Chunk Loader Manager (" + entries.size() + " loaders)";
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title, cx + (panelWidth - titleWidth) / 2, cy + 8, ACCENT_COLOR, true);

        // Separator
        context.fill(cx + 5, cy + 22, cx + panelWidth - 5, cy + 23, BORDER_COLOR);

        // Entries
        int end = Math.min(scrollOffset + VISIBLE_ENTRIES, entries.size());
        for (int i = scrollOffset; i < end; i++) {
            int entryY = cy + 30 + (i - scrollOffset) * ENTRY_HEIGHT;
            drawEntry(context, cx + 5, entryY, panelWidth - 40, entries.get(i));
        }

        if (entries.isEmpty()) {
            String empty = "No chunk loaders found.";
            int ew = textRenderer.getWidth(empty);
            context.drawText(textRenderer, empty, cx + (panelWidth - ew) / 2, cy + 60, LABEL_COLOR, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawEntry(DrawContext context, int x, int y, int w, ModNetworking.LoaderListEntry entry) {
        // Entry background
        context.fill(x, y, x + w, y + ENTRY_HEIGHT - 2, ENTRY_BG);
        drawBorder(context, x, y, w, ENTRY_HEIGHT - 2, ENTRY_BORDER);

        ChunkLoaderTier tier = ChunkLoaderTier.fromOrdinal(entry.tierOrdinal());
        String name = (entry.customName() != null && !entry.customName().isEmpty())
                ? entry.customName() : tier.getDisplayName();

        // Name and status
        String status = entry.enabled() ? "\u2714" : "\u2718";
        int statusColor = entry.enabled() ? ENABLED_COLOR : DISABLED_COLOR;
        context.drawText(textRenderer, status, x + 4, y + 4, statusColor, true);
        context.drawText(textRenderer, name, x + 16, y + 4, TEXT_COLOR, false);

        // Details line 1
        String mode = entry.centered() ? "Centered" : "Dir:" + Direction.fromHorizontal(entry.facingHorizontal()).getName();
        String details = tier.getSize() + "x" + tier.getSize() + " | " + mode;
        context.drawText(textRenderer, details, x + 4, y + 16, LABEL_COLOR, false);

        // Details line 2
        String pos = "@ " + entry.pos().getX() + ", " + entry.pos().getY() + ", " + entry.pos().getZ();
        String dim = formatDimension(entry.dimension());
        context.drawText(textRenderer, pos + " | " + dim, x + 4, y + 28, 0xFF777777, false);
    }

    private String formatDimension(String dim) {
        if (dim.contains(":")) {
            dim = dim.substring(dim.indexOf(':') + 1);
        }
        return dim.replace("_", " ");
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0 && scrollOffset > 0) {
            scrollOffset--;
            rebuildButtons();
        } else if (verticalAmount < 0 && scrollOffset < entries.size() - VISIBLE_ENTRIES) {
            scrollOffset++;
            rebuildButtons();
        }
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
