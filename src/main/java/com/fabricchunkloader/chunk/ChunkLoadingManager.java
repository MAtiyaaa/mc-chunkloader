package com.fabricchunkloader.chunk;

import com.fabricchunkloader.ChunkLoaderTier;
import com.fabricchunkloader.config.ModConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;

import java.util.*;

/**
 * Manages all chunk loaders in a single dimension.
 * Stored as PersistentState so it loads on server start before any chunks load,
 * solving the chicken-and-egg problem of needing to force-load the chunks
 * that contain chunk loader block entities.
 */
public class ChunkLoadingManager extends PersistentState {
    private static final String DATA_KEY = "chunkloader_data";

    private final Map<BlockPos, LoaderEntry> loaders = new HashMap<>();
    private final Map<ChunkPos, Integer> chunkRefCounts = new HashMap<>();
    private ServerWorld world;

    public static class LoaderEntry {
        public BlockPos pos;
        public UUID owner;
        public String ownerName;
        public ChunkLoaderTier tier;
        public boolean enabled;
        public boolean centered;
        public Direction facing;
        public String customName;
        public Set<ChunkPos> loadedChunks = new HashSet<>();

        public LoaderEntry() {}

        public LoaderEntry(BlockPos pos, UUID owner, String ownerName, ChunkLoaderTier tier, Direction facing) {
            this.pos = pos;
            this.owner = owner;
            this.ownerName = ownerName;
            this.tier = tier;
            this.enabled = true;
            this.centered = true;
            this.facing = facing;
            this.customName = "";
        }

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putLong("pos", pos.asLong());
            nbt.putUuid("owner", owner);
            nbt.putString("ownerName", ownerName != null ? ownerName : "");
            nbt.putString("tier", tier.getId());
            nbt.putBoolean("enabled", enabled);
            nbt.putBoolean("centered", centered);
            nbt.putInt("facing", facing.getId());
            nbt.putString("customName", customName != null ? customName : "");
            return nbt;
        }

        public static LoaderEntry fromNbt(NbtCompound nbt) {
            try {
                LoaderEntry entry = new LoaderEntry();
                entry.pos = BlockPos.fromLong(nbt.getLong("pos"));
                entry.owner = nbt.containsUuid("owner") ? nbt.getUuid("owner") : new UUID(0, 0);
                entry.ownerName = nbt.contains("ownerName") ? nbt.getString("ownerName") : "";
                entry.tier = ChunkLoaderTier.fromId(nbt.getString("tier"));
                entry.enabled = nbt.getBoolean("enabled");
                entry.centered = nbt.getBoolean("centered");
                entry.facing = Direction.byId(nbt.getInt("facing"));
                entry.customName = nbt.contains("customName") ? nbt.getString("customName") : "";
                if (entry.facing.getAxis() == Direction.Axis.Y) {
                    entry.facing = Direction.NORTH;
                }
                return entry;
            } catch (Exception e) {
                System.err.println("[ChunkLoader] Failed to deserialize loader entry: " + e.getMessage());
                return null;
            }
        }
    }

    public ChunkLoadingManager() {
        super();
    }

    public void setWorld(ServerWorld world) {
        this.world = world;
    }

    public static Type<ChunkLoadingManager> getPersistentStateType() {
        return new Type<>(
                ChunkLoadingManager::new,
                (nbt, registryLookup) -> fromNbt(nbt),
                null
        );
    }

    public static ChunkLoadingManager get(ServerWorld world) {
        ChunkLoadingManager manager = world.getPersistentStateManager()
                .getOrCreate(getPersistentStateType(), DATA_KEY);
        manager.setWorld(world);
        return manager;
    }

    private static ChunkLoadingManager fromNbt(NbtCompound nbt) {
        ChunkLoadingManager manager = new ChunkLoadingManager();
        if (nbt.contains("loaders")) {
            NbtList list = nbt.getList("loaders", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                LoaderEntry entry = LoaderEntry.fromNbt(list.getCompound(i));
                if (entry != null) {
                    manager.loaders.put(entry.pos, entry);
                }
            }
        }
        return manager;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (LoaderEntry entry : loaders.values()) {
            list.add(entry.toNbt());
        }
        nbt.put("loaders", list);
        return nbt;
    }

    /**
     * Called on server start after the manager is loaded from disk.
     * Re-enables force-loading for all enabled loaders.
     */
    public void onServerStart() {
        if (world == null) return;
        for (LoaderEntry entry : loaders.values()) {
            if (entry.enabled) {
                enableChunkLoading(entry);
            }
        }
    }

    public void addLoader(BlockPos pos, UUID owner, String ownerName, ChunkLoaderTier tier, Direction facing) {
        ModConfig config = ModConfig.get();

        if (!config.isDimensionAllowed(world.getRegistryKey().getValue().toString())) {
            return;
        }

        long playerCount = loaders.values().stream()
                .filter(e -> e.owner.equals(owner)).count();
        if (playerCount >= config.maxLoadersPerPlayer) {
            return;
        }
        if (loaders.size() >= config.maxLoadersPerWorld) {
            return;
        }

        LoaderEntry entry = new LoaderEntry(pos, owner, ownerName, tier, facing);
        loaders.put(pos, entry);
        enableChunkLoading(entry);
        markDirty();
    }

    public void removeLoader(BlockPos pos) {
        LoaderEntry entry = loaders.remove(pos);
        if (entry != null) {
            disableChunkLoading(entry);
            markDirty();
        }
    }

    public boolean toggleEnabled(BlockPos pos) {
        LoaderEntry entry = loaders.get(pos);
        if (entry == null) return false;

        if (entry.enabled) {
            disableChunkLoading(entry);
            entry.enabled = false;
        } else {
            entry.enabled = true;
            enableChunkLoading(entry);
        }
        markDirty();
        return entry.enabled;
    }

    public boolean toggleMode(BlockPos pos) {
        LoaderEntry entry = loaders.get(pos);
        if (entry == null) return false;

        boolean wasEnabled = entry.enabled;
        if (wasEnabled) {
            disableChunkLoading(entry);
        }

        entry.centered = !entry.centered;

        if (wasEnabled) {
            enableChunkLoading(entry);
        }
        markDirty();
        return entry.centered;
    }

    public void setCustomName(BlockPos pos, String name) {
        LoaderEntry entry = loaders.get(pos);
        if (entry == null) return;
        entry.customName = name != null ? name : "";
        markDirty();
    }

    public LoaderEntry getLoader(BlockPos pos) {
        return loaders.get(pos);
    }

    public List<LoaderEntry> getLoadersForPlayer(UUID owner) {
        List<LoaderEntry> result = new ArrayList<>();
        for (LoaderEntry entry : loaders.values()) {
            if (entry.owner.equals(owner)) {
                result.add(entry);
            }
        }
        return result;
    }

    public List<LoaderEntry> getAllLoaders() {
        return new ArrayList<>(loaders.values());
    }

    public int getLoaderCount() {
        return loaders.size();
    }

    public int getPlayerLoaderCount(UUID owner) {
        return (int) loaders.values().stream()
                .filter(e -> e.owner.equals(owner)).count();
    }

    private void enableChunkLoading(LoaderEntry entry) {
        if (world == null) return;
        Set<ChunkPos> chunks = calculateLoadedChunks(entry);
        entry.loadedChunks = chunks;
        for (ChunkPos chunk : chunks) {
            int count = chunkRefCounts.getOrDefault(chunk, 0);
            if (count == 0) {
                world.setChunkForced(chunk.x, chunk.z, true);
            }
            chunkRefCounts.put(chunk, count + 1);
        }
    }

    private void disableChunkLoading(LoaderEntry entry) {
        if (world == null || entry.loadedChunks == null) return;
        for (ChunkPos chunk : entry.loadedChunks) {
            int count = chunkRefCounts.getOrDefault(chunk, 0) - 1;
            if (count <= 0) {
                world.setChunkForced(chunk.x, chunk.z, false);
                chunkRefCounts.remove(chunk);
            } else {
                chunkRefCounts.put(chunk, count);
            }
        }
        entry.loadedChunks = new HashSet<>();
    }

    /**
     * Calculates which chunks should be force-loaded based on tier, mode, and facing.
     * In centered mode, the area is centered on the block's chunk.
     * In directional mode, the area extends from the block's chunk
     * in the direction the block is facing.
     */
    public static Set<ChunkPos> calculateLoadedChunks(LoaderEntry entry) {
        ModConfig config = ModConfig.get();
        int size = config.getEffectiveSize(entry.tier);
        int chunkX = entry.pos.getX() >> 4;
        int chunkZ = entry.pos.getZ() >> 4;
        Set<ChunkPos> chunks = new HashSet<>();

        if (entry.centered) {
            int half = size / 2;
            int startX = chunkX - half;
            int startZ = chunkZ - half;
            for (int x = startX; x < startX + size; x++) {
                for (int z = startZ; z < startZ + size; z++) {
                    chunks.add(new ChunkPos(x, z));
                }
            }
        } else {
            int half = size / 2;
            switch (entry.facing) {
                case NORTH -> {
                    int startX = chunkX - half;
                    int startZ = chunkZ - size + 1;
                    for (int x = startX; x < startX + size; x++) {
                        for (int z = startZ; z <= chunkZ; z++) {
                            chunks.add(new ChunkPos(x, z));
                        }
                    }
                }
                case SOUTH -> {
                    int startX = chunkX - half;
                    for (int x = startX; x < startX + size; x++) {
                        for (int z = chunkZ; z < chunkZ + size; z++) {
                            chunks.add(new ChunkPos(x, z));
                        }
                    }
                }
                case EAST -> {
                    int startZ = chunkZ - half;
                    for (int x = chunkX; x < chunkX + size; x++) {
                        for (int z = startZ; z < startZ + size; z++) {
                            chunks.add(new ChunkPos(x, z));
                        }
                    }
                }
                case WEST -> {
                    int startZ = chunkZ - half;
                    int startX = chunkX - size + 1;
                    for (int x = startX; x <= chunkX; x++) {
                        for (int z = startZ; z < startZ + size; z++) {
                            chunks.add(new ChunkPos(x, z));
                        }
                    }
                }
                default -> {
                    // Fallback to centered
                    int h = size / 2;
                    int sx = chunkX - h;
                    int sz = chunkZ - h;
                    for (int x = sx; x < sx + size; x++) {
                        for (int z = sz; z < sz + size; z++) {
                            chunks.add(new ChunkPos(x, z));
                        }
                    }
                }
            }
        }
        return chunks;
    }
}
