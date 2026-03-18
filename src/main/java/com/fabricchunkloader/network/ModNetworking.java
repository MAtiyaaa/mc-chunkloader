package com.fabricchunkloader.network;

import com.fabricchunkloader.ChunkLoaderTier;
import com.fabricchunkloader.block.ChunkLoaderBlock;
import com.fabricchunkloader.chunk.ChunkLoadingManager;
import com.fabricchunkloader.config.ModConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class ModNetworking {
    // ---- C2S Payloads ----

    public record ToggleEnabledPayload(BlockPos pos) implements CustomPayload {
        public static final Id<ToggleEnabledPayload> ID = new Id<>(Identifier.of("chunkloader", "toggle_enabled"));
        public static final PacketCodec<RegistryByteBuf, ToggleEnabledPayload> CODEC = new PacketCodec<>() {
            @Override
            public ToggleEnabledPayload decode(RegistryByteBuf buf) { return new ToggleEnabledPayload(buf.readBlockPos()); }
            @Override
            public void encode(RegistryByteBuf buf, ToggleEnabledPayload p) { buf.writeBlockPos(p.pos()); }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ToggleModePayload(BlockPos pos) implements CustomPayload {
        public static final Id<ToggleModePayload> ID = new Id<>(Identifier.of("chunkloader", "toggle_mode"));
        public static final PacketCodec<RegistryByteBuf, ToggleModePayload> CODEC = new PacketCodec<>() {
            @Override
            public ToggleModePayload decode(RegistryByteBuf buf) { return new ToggleModePayload(buf.readBlockPos()); }
            @Override
            public void encode(RegistryByteBuf buf, ToggleModePayload p) { buf.writeBlockPos(p.pos()); }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RenameLoaderPayload(BlockPos pos, String name) implements CustomPayload {
        public static final Id<RenameLoaderPayload> ID = new Id<>(Identifier.of("chunkloader", "rename_loader"));
        public static final PacketCodec<RegistryByteBuf, RenameLoaderPayload> CODEC = new PacketCodec<>() {
            @Override
            public RenameLoaderPayload decode(RegistryByteBuf buf) {
                return new RenameLoaderPayload(buf.readBlockPos(), buf.readString(128));
            }
            @Override
            public void encode(RegistryByteBuf buf, RenameLoaderPayload p) {
                buf.writeBlockPos(p.pos()); buf.writeString(p.name(), 128);
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RequestLoaderListPayload(int dummy) implements CustomPayload {
        public static final Id<RequestLoaderListPayload> ID = new Id<>(Identifier.of("chunkloader", "request_loader_list"));
        public static final PacketCodec<RegistryByteBuf, RequestLoaderListPayload> CODEC = new PacketCodec<>() {
            @Override
            public RequestLoaderListPayload decode(RegistryByteBuf buf) { return new RequestLoaderListPayload(buf.readVarInt()); }
            @Override
            public void encode(RegistryByteBuf buf, RequestLoaderListPayload p) { buf.writeVarInt(p.dummy()); }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ---- S2C Payloads ----

    public record LoaderListEntry(
            BlockPos pos, String dimension, int tierOrdinal,
            boolean enabled, boolean centered, int facingHorizontal,
            String customName, String ownerName
    ) {}

    public record LoaderListResponsePayload(List<LoaderListEntry> entries) implements CustomPayload {
        public static final Id<LoaderListResponsePayload> ID = new Id<>(Identifier.of("chunkloader", "loader_list_response"));
        public static final PacketCodec<RegistryByteBuf, LoaderListResponsePayload> CODEC = new PacketCodec<>() {
            @Override
            public LoaderListResponsePayload decode(RegistryByteBuf buf) {
                int count = buf.readVarInt();
                List<LoaderListEntry> entries = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    entries.add(new LoaderListEntry(
                            buf.readBlockPos(), buf.readString(256), buf.readVarInt(),
                            buf.readBoolean(), buf.readBoolean(), buf.readVarInt(),
                            buf.readString(256), buf.readString(256)
                    ));
                }
                return new LoaderListResponsePayload(entries);
            }
            @Override
            public void encode(RegistryByteBuf buf, LoaderListResponsePayload p) {
                buf.writeVarInt(p.entries().size());
                for (LoaderListEntry e : p.entries()) {
                    buf.writeBlockPos(e.pos()); buf.writeString(e.dimension(), 256);
                    buf.writeVarInt(e.tierOrdinal()); buf.writeBoolean(e.enabled());
                    buf.writeBoolean(e.centered()); buf.writeVarInt(e.facingHorizontal());
                    buf.writeString(e.customName(), 256); buf.writeString(e.ownerName(), 256);
                }
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ---- Registration ----

    public static void registerC2SPayloads() {
        PayloadTypeRegistry.playC2S().register(ToggleEnabledPayload.ID, ToggleEnabledPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleModePayload.ID, ToggleModePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RenameLoaderPayload.ID, RenameLoaderPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestLoaderListPayload.ID, RequestLoaderListPayload.CODEC);
    }

    public static void registerS2CPayloads() {
        PayloadTypeRegistry.playS2C().register(LoaderListResponsePayload.ID, LoaderListResponsePayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ToggleEnabledPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.player().server.execute(() -> {
                ServerWorld world = player.getServerWorld();
                ChunkLoadingManager manager = ChunkLoadingManager.get(world);
                ChunkLoadingManager.LoaderEntry entry = manager.getLoader(payload.pos());
                if (entry == null) return;

                if (ModConfig.get().enableOwnership && !entry.owner.equals(player.getUuid())
                        && !player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("You don't own this chunk loader.").formatted(Formatting.RED), true);
                    return;
                }

                boolean newState = manager.toggleEnabled(payload.pos());
                BlockState blockState = world.getBlockState(payload.pos());
                if (blockState.getBlock() instanceof ChunkLoaderBlock) {
                    world.setBlockState(payload.pos(), blockState.with(ChunkLoaderBlock.ACTIVE, newState));
                }
                player.sendMessage(Text.literal("Chunk loader " + (newState ? "enabled" : "disabled") + ".")
                        .formatted(newState ? Formatting.GREEN : Formatting.RED), true);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ToggleModePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.player().server.execute(() -> {
                ServerWorld world = player.getServerWorld();
                ChunkLoadingManager manager = ChunkLoadingManager.get(world);
                ChunkLoadingManager.LoaderEntry entry = manager.getLoader(payload.pos());
                if (entry == null) return;

                if (ModConfig.get().enableOwnership && !entry.owner.equals(player.getUuid())
                        && !player.hasPermissionLevel(2)) {
                    return;
                }

                ModConfig config = ModConfig.get();
                boolean newCentered = manager.toggleMode(payload.pos());
                if (newCentered && !config.allowCenteredMode) {
                    manager.toggleMode(payload.pos());
                    return;
                }
                if (!newCentered && !config.allowDirectionalMode) {
                    manager.toggleMode(payload.pos());
                    return;
                }

                player.sendMessage(Text.literal("Mode: " + (newCentered ? "Centered" : "Directional"))
                        .formatted(Formatting.AQUA), true);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RenameLoaderPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.player().server.execute(() -> {
                ServerWorld world = player.getServerWorld();
                ChunkLoadingManager manager = ChunkLoadingManager.get(world);
                ChunkLoadingManager.LoaderEntry entry = manager.getLoader(payload.pos());
                if (entry == null) return;

                if (ModConfig.get().enableOwnership && !entry.owner.equals(player.getUuid())
                        && !player.hasPermissionLevel(2)) {
                    return;
                }

                String safeName = payload.name().length() > 64 ? payload.name().substring(0, 64) : payload.name();
                manager.setCustomName(payload.pos(), safeName);
                player.sendMessage(Text.literal("Loader renamed to: " + safeName)
                        .formatted(Formatting.YELLOW), true);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestLoaderListPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.player().server.execute(() -> {
                List<LoaderListEntry> entries = new ArrayList<>();
                boolean isAdmin = player.hasPermissionLevel(2) && ModConfig.get().allowAdminManageAll;

                for (ServerWorld world : player.server.getWorlds()) {
                    ChunkLoadingManager manager = ChunkLoadingManager.get(world);
                    List<ChunkLoadingManager.LoaderEntry> loaders = isAdmin
                            ? manager.getAllLoaders()
                            : manager.getLoadersForPlayer(player.getUuid());
                    String dimId = world.getRegistryKey().getValue().toString();

                    for (ChunkLoadingManager.LoaderEntry e : loaders) {
                        entries.add(new LoaderListEntry(
                                e.pos, dimId, e.tier.ordinal(),
                                e.enabled, e.centered, e.facing.getHorizontal(),
                                e.customName != null ? e.customName : "",
                                e.ownerName != null ? e.ownerName : ""
                        ));
                    }
                }

                ServerPlayNetworking.send(player, new LoaderListResponsePayload(entries));
            });
        });
    }
}
