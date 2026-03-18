package com.fabricchunkloader.screen;

import com.fabricchunkloader.ChunkLoaderTier;
import com.fabricchunkloader.block.ChunkLoaderBlockEntity;
import com.fabricchunkloader.chunk.ChunkLoadingManager;
import com.fabricchunkloader.network.ChunkLoaderScreenData;
import com.fabricchunkloader.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Server-side screen handler for the chunk loader GUI.
 * The client constructor receives opening data; the server constructor
 * receives the block entity directly.
 */
public class ChunkLoaderScreenHandler extends ScreenHandler {
    private final BlockPos blockPos;
    private final ChunkLoaderTier tier;
    private boolean enabled;
    private boolean centered;
    private Direction facing;
    private String customName;
    private String ownerName;

    // Client constructor – called by ExtendedScreenHandlerType
    public ChunkLoaderScreenHandler(int syncId, PlayerInventory inv, ChunkLoaderScreenData data) {
        super(ModScreenHandlers.CHUNK_LOADER_SCREEN_HANDLER, syncId);
        this.blockPos = data.pos();
        this.tier = ChunkLoaderTier.fromOrdinal(data.tierOrdinal());
        this.enabled = data.enabled() != 0;
        this.centered = data.centered() != 0;
        this.facing = Direction.fromHorizontal(data.facingHorizontal());
        this.customName = data.customName();
        this.ownerName = data.ownerName();
    }

    // Server constructor – called from ChunkLoaderBlockEntity.createMenu
    public ChunkLoaderScreenHandler(int syncId, PlayerInventory inv, ChunkLoaderBlockEntity entity) {
        super(ModScreenHandlers.CHUNK_LOADER_SCREEN_HANDLER, syncId);
        this.blockPos = entity.getPos();
        this.tier = entity.getTier();

        if (entity.getWorld() instanceof ServerWorld serverWorld) {
            ChunkLoadingManager manager = ChunkLoadingManager.get(serverWorld);
            ChunkLoadingManager.LoaderEntry entry = manager.getLoader(blockPos);
            if (entry != null) {
                this.enabled = entry.enabled;
                this.centered = entry.centered;
                this.facing = entry.facing;
                this.customName = entry.customName != null ? entry.customName : "";
                this.ownerName = entry.ownerName != null ? entry.ownerName : "";
            } else {
                this.enabled = false;
                this.centered = true;
                this.facing = Direction.NORTH;
                this.customName = "";
                this.ownerName = "";
            }
        } else {
            this.enabled = false;
            this.centered = true;
            this.facing = Direction.NORTH;
            this.customName = "";
            this.ownerName = "";
        }
    }

    public BlockPos getBlockPos() { return blockPos; }
    public ChunkLoaderTier getTier() { return tier; }
    public boolean isEnabled() { return enabled; }
    public boolean isCentered() { return centered; }
    public Direction getFacing() { return facing; }
    public String getCustomName() { return customName; }
    public String getOwnerName() { return ownerName; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setCentered(boolean centered) { this.centered = centered; }
    public void setCustomName(String name) { this.customName = name; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.squaredDistanceTo(
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5
        ) <= 64.0;
    }
}
