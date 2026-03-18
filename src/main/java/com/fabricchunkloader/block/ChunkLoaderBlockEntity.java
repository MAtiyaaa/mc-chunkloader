package com.fabricchunkloader.block;

import com.fabricchunkloader.ChunkLoaderTier;
import com.fabricchunkloader.chunk.ChunkLoadingManager;
import com.fabricchunkloader.network.ChunkLoaderScreenData;
import com.fabricchunkloader.registry.ModBlockEntities;
import com.fabricchunkloader.registry.ModScreenHandlers;
import com.fabricchunkloader.screen.ChunkLoaderScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class ChunkLoaderBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<ChunkLoaderScreenData> {
    private ChunkLoaderTier tier;

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHUNK_LOADER_BLOCK_ENTITY, pos, state);
        this.tier = determineTier(state);
    }

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state, ChunkLoaderTier tier) {
        super(ModBlockEntities.CHUNK_LOADER_BLOCK_ENTITY, pos, state);
        this.tier = tier;
    }

    public ChunkLoaderTier getTier() {
        return tier;
    }

    private ChunkLoaderTier determineTier(BlockState state) {
        if (state.getBlock() instanceof ChunkLoaderBlock clb) {
            return clb.getTier();
        }
        return ChunkLoaderTier.BASIC;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("tier")) {
            this.tier = ChunkLoaderTier.fromId(nbt.getString("tier"));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("tier", tier.getId());
    }

    @Override
    public ChunkLoaderScreenData getScreenOpeningData(ServerPlayerEntity player) {
        ChunkLoadingManager manager = ChunkLoadingManager.get((ServerWorld) world);
        ChunkLoadingManager.LoaderEntry entry = manager.getLoader(pos);
        boolean enabled = entry != null && entry.enabled;
        boolean centered = entry == null || entry.centered;
        Direction facing = entry != null ? entry.facing : Direction.NORTH;
        String customName = entry != null && entry.customName != null ? entry.customName : "";
        String ownerName = entry != null && entry.ownerName != null ? entry.ownerName : "";

        return new ChunkLoaderScreenData(
                pos,
                enabled ? 1 : 0,
                tier.ordinal(),
                centered ? 1 : 0,
                facing.getHorizontal(),
                customName,
                ownerName
        );
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ChunkLoaderScreenHandler(syncId, playerInventory, this);
    }
}
