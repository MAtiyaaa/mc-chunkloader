package com.fabricchunkloader.command;

import com.fabricchunkloader.ChunkLoaderTier;
import com.fabricchunkloader.chunk.ChunkLoadingManager;
import com.fabricchunkloader.config.ModConfig;
import com.fabricchunkloader.network.ModNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class ModCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("chunkloader")
                .then(CommandManager.literal("list")
                        .executes(ctx -> listLoaders(ctx.getSource(), false)))
                .then(CommandManager.literal("listall")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> listLoaders(ctx.getSource(), true)))
                .then(CommandManager.literal("manage")
                        .executes(ctx -> openManagement(ctx.getSource())))
                .then(CommandManager.literal("reload")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> reloadConfig(ctx.getSource())))
                .then(CommandManager.literal("info")
                        .executes(ctx -> showInfo(ctx.getSource())))
                .then(CommandManager.literal("help")
                        .executes(ctx -> showHelp(ctx.getSource())))
        );
    }

    private static int listLoaders(ServerCommandSource source, boolean all) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendFeedback(() -> Text.literal("This command can only be run by a player."), false);
            return 0;
        }

        List<LoaderInfo> infos = new ArrayList<>();
        for (ServerWorld world : source.getServer().getWorlds()) {
            ChunkLoadingManager manager = ChunkLoadingManager.get(world);
            List<ChunkLoadingManager.LoaderEntry> loaders = all
                    ? manager.getAllLoaders()
                    : manager.getLoadersForPlayer(player.getUuid());
            String dimId = world.getRegistryKey().getValue().toString();
            for (ChunkLoadingManager.LoaderEntry e : loaders) {
                infos.add(new LoaderInfo(e, dimId));
            }
        }

        if (infos.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No chunk loaders found.").formatted(Formatting.YELLOW), false);
            return 0;
        }

        source.sendFeedback(() -> Text.literal("=== Chunk Loaders (" + infos.size() + ") ===").formatted(Formatting.GOLD), false);
        for (LoaderInfo info : infos) {
            ChunkLoadingManager.LoaderEntry e = info.entry;
            ChunkLoaderTier tier = e.tier;
            String name = (e.customName != null && !e.customName.isEmpty()) ? e.customName : tier.getDisplayName();
            String status = e.enabled ? "\u2714 ON" : "\u2718 OFF";
            Formatting statusColor = e.enabled ? Formatting.GREEN : Formatting.RED;
            String mode = e.centered ? "Centered" : "Directional (" + e.facing.getName() + ")";

            MutableText text = Text.literal(" ")
                    .append(Text.literal("[" + name + "]").formatted(Formatting.AQUA))
                    .append(Text.literal(" " + status).formatted(statusColor))
                    .append(Text.literal(" | " + tier.getSize() + "x" + tier.getSize()).formatted(Formatting.WHITE))
                    .append(Text.literal(" | " + mode).formatted(Formatting.GRAY));

            MutableText posText = Text.literal(" @ " + e.pos.getX() + ", " + e.pos.getY() + ", " + e.pos.getZ())
                    .formatted(Formatting.DARK_GRAY);
            if (all) {
                posText.append(Text.literal(" | " + info.dimension).formatted(Formatting.DARK_GRAY));
                posText.append(Text.literal(" | Owner: " + e.ownerName).formatted(Formatting.DARK_GRAY));
            }

            source.sendFeedback(() -> text.append(posText), false);
        }
        return infos.size();
    }

    private static int openManagement(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendFeedback(() -> Text.literal("This command can only be run by a player."), false);
            return 0;
        }

        List<ModNetworking.LoaderListEntry> entries = new ArrayList<>();
        boolean isAdmin = player.hasPermissionLevel(2) && ModConfig.get().allowAdminManageAll;

        for (ServerWorld world : source.getServer().getWorlds()) {
            ChunkLoadingManager manager = ChunkLoadingManager.get(world);
            List<ChunkLoadingManager.LoaderEntry> loaders = isAdmin
                    ? manager.getAllLoaders()
                    : manager.getLoadersForPlayer(player.getUuid());
            String dimId = world.getRegistryKey().getValue().toString();

            for (ChunkLoadingManager.LoaderEntry e : loaders) {
                entries.add(new ModNetworking.LoaderListEntry(
                        e.pos, dimId, e.tier.ordinal(),
                        e.enabled, e.centered, e.facing.getHorizontal(),
                        e.customName != null ? e.customName : "",
                        e.ownerName != null ? e.ownerName : ""
                ));
            }
        }

        ServerPlayNetworking.send(player, new ModNetworking.LoaderListResponsePayload(entries));
        return 1;
    }

    private static int reloadConfig(ServerCommandSource source) {
        ModConfig.load();
        source.sendFeedback(() -> Text.literal("Chunk Loader config reloaded.").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showInfo(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        int playerCount = 0;
        int totalCount = 0;
        for (ServerWorld world : source.getServer().getWorlds()) {
            ChunkLoadingManager manager = ChunkLoadingManager.get(world);
            playerCount += manager.getPlayerLoaderCount(player.getUuid());
            totalCount += manager.getLoaderCount();
        }

        ModConfig config = ModConfig.get();
        int finalPlayerCount = playerCount;
        int finalTotalCount = totalCount;
        source.sendFeedback(() -> Text.literal("=== Chunk Loader Info ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("Your loaders: " + finalPlayerCount + "/" + config.maxLoadersPerPlayer).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("World total: " + finalTotalCount + "/" + config.maxLoadersPerWorld).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Redstone control: " + (config.enableRedstoneControl ? "Enabled" : "Disabled")).formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("Ownership: " + (config.enableOwnership ? "Enabled" : "Disabled")).formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int showHelp(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("=== Chunk Loader Commands ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("/chunkloader list").formatted(Formatting.AQUA)
                .append(Text.literal(" - List your chunk loaders").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("/chunkloader manage").formatted(Formatting.AQUA)
                .append(Text.literal(" - Open management GUI").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("/chunkloader info").formatted(Formatting.AQUA)
                .append(Text.literal(" - Show loader statistics").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("/chunkloader listall").formatted(Formatting.AQUA)
                .append(Text.literal(" - List all loaders (admin)").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("/chunkloader reload").formatted(Formatting.AQUA)
                .append(Text.literal(" - Reload config (admin)").formatted(Formatting.GRAY)), false);
        return 1;
    }

    private record LoaderInfo(ChunkLoadingManager.LoaderEntry entry, String dimension) {}
}
