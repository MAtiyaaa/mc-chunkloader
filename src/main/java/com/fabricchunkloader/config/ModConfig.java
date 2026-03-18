package com.fabricchunkloader.config;

import com.fabricchunkloader.ChunkLoaderTier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Handles mod configuration, persisted as a JSON file in the config directory.
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE;

    // Tier toggles
    public boolean enableBasicTier = true;
    public boolean enableCompactTier = true;
    public boolean enableAdvancedTier = true;
    public boolean enableEliteTier = true;
    public boolean enableUltimateTier = true;

    // Chunk size overrides (0 = use default from tier)
    public int basicSize = 0;
    public int compactSize = 0;
    public int advancedSize = 0;
    public int eliteSize = 0;
    public int ultimateSize = 0;

    // Mode settings
    public boolean allowCenteredMode = true;
    public boolean allowDirectionalMode = true;

    // Limits
    public int maxLoadersPerPlayer = 16;
    public int maxLoadersPerWorld = 256;

    // Dimension control
    public List<String> dimensionBlacklist = new ArrayList<>();
    public List<String> dimensionWhitelist = new ArrayList<>();
    public boolean useDimensionWhitelist = false;

    // Optional features
    public boolean enableRedstoneControl = false;
    public boolean requireFuel = false;
    public int fuelTicksPerCoal = 72000; // 1 hour per coal
    public boolean adminOnlyMode = false;

    // Permissions
    public boolean enableOwnership = true;
    public boolean allowAdminManageAll = true;

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("chunkloader.json");
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                INSTANCE = GSON.fromJson(json, ModConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new ModConfig();
                }
            } catch (IOException e) {
                System.err.println("[ChunkLoader] Failed to load config: " + e.getMessage());
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("chunkloader.json");
        try {
            Files.writeString(configPath, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            System.err.println("[ChunkLoader] Failed to save config: " + e.getMessage());
        }
    }

    public boolean isTierEnabled(ChunkLoaderTier tier) {
        return switch (tier) {
            case BASIC -> enableBasicTier;
            case COMPACT -> enableCompactTier;
            case ADVANCED -> enableAdvancedTier;
            case ELITE -> enableEliteTier;
            case ULTIMATE -> enableUltimateTier;
        };
    }

    /**
     * Returns the effective chunk size for a tier, respecting config overrides.
     */
    public int getEffectiveSize(ChunkLoaderTier tier) {
        int override = switch (tier) {
            case BASIC -> basicSize;
            case COMPACT -> compactSize;
            case ADVANCED -> advancedSize;
            case ELITE -> eliteSize;
            case ULTIMATE -> ultimateSize;
        };
        return override > 0 ? override : tier.getSize();
    }

    public boolean isDimensionAllowed(String dimensionId) {
        if (useDimensionWhitelist) {
            return dimensionWhitelist.contains(dimensionId);
        }
        return !dimensionBlacklist.contains(dimensionId);
    }
}
