package com.fabricchunkloader.block;

import com.fabricchunkloader.ChunkLoaderTier;
import com.fabricchunkloader.chunk.ChunkLoadingManager;
import com.fabricchunkloader.config.ModConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChunkLoaderBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<ChunkLoaderBlock> CODEC = createCodec(
            settings -> new ChunkLoaderBlock(settings, ChunkLoaderTier.BASIC));
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    private final ChunkLoaderTier tier;

    public ChunkLoaderBlock(Settings settings, ChunkLoaderTier tier) {
        super(settings);
        this.tier = tier;
        setDefaultState(getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    public ChunkLoaderTier getTier() {
        return tier;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(ACTIVE, false);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient && placer instanceof ServerPlayerEntity player) {
            ModConfig config = ModConfig.get();
            if (!config.isTierEnabled(tier)) {
                player.sendMessage(Text.literal("This chunk loader tier is disabled.")
                        .formatted(Formatting.RED), true);
                return;
            }

            String dimId = ((ServerWorld) world).getRegistryKey().getValue().toString();
            if (!config.isDimensionAllowed(dimId)) {
                player.sendMessage(Text.literal("Chunk loaders are not allowed in this dimension.")
                        .formatted(Formatting.RED), true);
                return;
            }

            ChunkLoadingManager manager = ChunkLoadingManager.get((ServerWorld) world);
            int playerCount = manager.getPlayerLoaderCount(player.getUuid());
            if (playerCount >= config.maxLoadersPerPlayer) {
                player.sendMessage(Text.literal("You have reached the maximum number of chunk loaders (" + config.maxLoadersPerPlayer + ").")
                        .formatted(Formatting.RED), true);
                return;
            }
            if (manager.getLoaderCount() >= config.maxLoadersPerWorld) {
                player.sendMessage(Text.literal("This world has reached the maximum number of chunk loaders.")
                        .formatted(Formatting.RED), true);
                return;
            }

            Direction facing = state.get(FACING);
            manager.addLoader(pos, player.getUuid(), player.getName().getString(), tier, facing);
            world.setBlockState(pos, state.with(ACTIVE, true));
            player.sendMessage(Text.literal(tier.getDisplayName() + " placed and enabled! (" + tier.getSize() + "x" + tier.getSize() + " chunks)")
                    .formatted(Formatting.GREEN), true);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ChunkLoaderBlockEntity chunkLoaderBE) {
                serverPlayer.openHandledScreen(chunkLoaderBE);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (!world.isClient) {
                ChunkLoadingManager manager = ChunkLoadingManager.get((ServerWorld) world);
                manager.removeLoader(pos);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChunkLoaderBlockEntity(pos, state, tier);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.literal(tier.getSize() + "\u00D7" + tier.getSize() + " chunks (" + tier.getTotalChunks() + " total)")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Right-click to configure").formatted(Formatting.DARK_GRAY));
        if (ModConfig.get().enableRedstoneControl) {
            tooltip.add(Text.literal("Redstone controllable").formatted(Formatting.RED));
        }
    }

    /**
     * Supports redstone control when enabled in config.
     * A redstone signal disables the chunk loader; no signal enables it.
     */
    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient && ModConfig.get().enableRedstoneControl) {
            boolean powered = world.isReceivingRedstonePower(pos);
            ChunkLoadingManager manager = ChunkLoadingManager.get((ServerWorld) world);
            ChunkLoadingManager.LoaderEntry entry = manager.getLoader(pos);
            if (entry != null) {
                if (powered && entry.enabled) {
                    manager.toggleEnabled(pos);
                    world.setBlockState(pos, state.with(ACTIVE, false));
                } else if (!powered && !entry.enabled) {
                    manager.toggleEnabled(pos);
                    world.setBlockState(pos, state.with(ACTIVE, true));
                }
            }
        }
    }
}
