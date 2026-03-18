package com.fabricchunkloader;

/**
 * Defines the available chunk loader tiers. Each tier specifies
 * a horizontal chunk area (size x size) to keep force-loaded.
 * Full vertical height is always loaded automatically by the game.
 */
public enum ChunkLoaderTier {
    BASIC(2, "basic", "Basic Chunk Loader"),
    COMPACT(4, "compact", "Compact Chunk Loader"),
    ADVANCED(8, "advanced", "Advanced Chunk Loader"),
    ELITE(16, "elite", "Elite Chunk Loader"),
    ULTIMATE(32, "ultimate", "Ultimate Chunk Loader");

    private final int size;
    private final String id;
    private final String displayName;

    ChunkLoaderTier(int size, String id, String displayName) {
        this.size = size;
        this.id = id;
        this.displayName = displayName;
    }

    /** Horizontal chunk count per axis (e.g. 4 means 4x4 = 16 chunks). */
    public int getSize() {
        return size;
    }

    /** Lowercase identifier used in registry names and lang keys. */
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Total number of chunks this tier covers. */
    public int getTotalChunks() {
        return size * size;
    }

    public static ChunkLoaderTier fromOrdinal(int ordinal) {
        ChunkLoaderTier[] values = values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return BASIC;
    }

    public static ChunkLoaderTier fromId(String id) {
        for (ChunkLoaderTier tier : values()) {
            if (tier.id.equals(id)) {
                return tier;
            }
        }
        return BASIC;
    }
}
