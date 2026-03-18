# Fabric Chunk Loader Mod

A production-quality chunk loading mod for **Minecraft Java Edition 1.21.1** using the **Fabric mod loader**. Place chunk loaders to keep areas loaded for AFK farming across multiple farms simultaneously.

## Features

### Five Chunk Loader Tiers
| Tier | Block Name | Chunk Area | Total Chunks | Key Material |
|------|-----------|------------|-------------|-------------|
| Basic | Basic Chunk Loader | 2x2 | 4 | Iron + Redstone + Ender Pearl |
| Compact | Compact Chunk Loader | 4x4 | 16 | Gold + Ender Pearl + Basic Loader |
| Advanced | Advanced Chunk Loader | 8x8 | 64 | Diamond + Blaze Rod + Compact Loader |
| Elite | Elite Chunk Loader | 16x16 | 256 | Emerald Block + Nether Star + Advanced Loader |
| Ultimate | Ultimate Chunk Loader | 32x32 | 1024 | Netherite + Shulker Shell + Elite Loader |

### Loading Modes
- **Centered Mode**: Loads a square chunk area centered on the block's chunk
- **Directional Mode**: Extends the loaded area in the direction the block faces

### Full GUI System
- **Individual Loader GUI** (right-click block): View status, tier, mode, facing, chunk preview grid. Toggle on/off, switch modes, rename the loader.
- **Management Screen** (`/chunkloader manage` or "Manage All" button): View all your chunk loaders across all dimensions. Toggle loaders remotely.
- **Chunk Preview Grid**: Visual minimap in the GUI showing exactly which chunks are loaded.

### Commands
| Command | Description | Permission |
|---------|-------------|-----------|
| `/chunkloader list` | List your chunk loaders | All players |
| `/chunkloader manage` | Open management GUI | All players |
| `/chunkloader info` | Show statistics | All players |
| `/chunkloader help` | Show command help | All players |
| `/chunkloader listall` | List ALL loaders (admin) | OP Level 2 |
| `/chunkloader reload` | Reload config file | OP Level 2 |

### Technical Features
- Real chunk force-loading via `ServerWorld.setChunkForced()`
- Reference-counted chunk tracking (overlapping loaders handled correctly)
- PersistentState storage survives server restarts
- Automatic re-enabling of chunk loaders on server start
- Safe cleanup when blocks are broken
- Multiplayer-safe with ownership and permissions
- Redstone control support (configurable)
- Per-player and per-world loader limits

## Configuration

Config file is created at `config/chunkloader.json` on first run.

```json
{
  "enableBasicTier": true,
  "enableCompactTier": true,
  "enableAdvancedTier": true,
  "enableEliteTier": true,
  "enableUltimateTier": true,
  "basicSize": 0,           // 0 = use default, or override chunk size
  "compactSize": 0,
  "advancedSize": 0,
  "eliteSize": 0,
  "ultimateSize": 0,
  "allowCenteredMode": true,
  "allowDirectionalMode": true,
  "maxLoadersPerPlayer": 16,
  "maxLoadersPerWorld": 256,
  "dimensionBlacklist": [],
  "dimensionWhitelist": [],
  "useDimensionWhitelist": false,
  "enableRedstoneControl": false,
  "requireFuel": false,
  "fuelTicksPerCoal": 72000,
  "adminOnlyMode": false,
  "enableOwnership": true,
  "allowAdminManageAll": true
}
```

## Folder Structure

```
src/main/
├── java/com/fabricchunkloader/
│   ├── FabricChunkLoader.java          # Main mod initializer
│   ├── FabricChunkLoaderClient.java    # Client initializer (screens, S2C receivers)
│   ├── ChunkLoaderTier.java            # Tier enum (BASIC through ULTIMATE)
│   ├── block/
│   │   ├── ChunkLoaderBlock.java       # HorizontalFacingBlock with ACTIVE state
│   │   └── ChunkLoaderBlockEntity.java # Block entity, implements ExtendedScreenHandlerFactory
│   ├── chunk/
│   │   └── ChunkLoadingManager.java    # PersistentState managing force-loaded chunks
│   ├── config/
│   │   └── ModConfig.java              # JSON config with Gson
│   ├── command/
│   │   └── ModCommands.java            # /chunkloader command tree
│   ├── network/
│   │   ├── ChunkLoaderScreenData.java  # Data record for screen opening
│   │   └── ModNetworking.java          # All C2S/S2C payloads and handlers
│   ├── registry/
│   │   ├── ModBlocks.java              # Block registration
│   │   ├── ModItems.java               # Item + ItemGroup registration
│   │   ├── ModBlockEntities.java       # BlockEntityType registration
│   │   └── ModScreenHandlers.java      # ScreenHandlerType registration
│   └── screen/
│       ├── ChunkLoaderScreenHandler.java  # Server-side screen handler
│       ├── ChunkLoaderScreen.java         # Client GUI for individual loader
│       └── ManagementScreen.java          # Client GUI for managing all loaders
└── resources/
    ├── fabric.mod.json
    ├── assets/chunkloader/
    │   ├── blockstates/           # 5 blockstate files (facing + active variants)
    │   ├── models/block/          # 10 block models (inactive + active per tier)
    │   ├── models/item/           # 5 item models
    │   ├── textures/block/        # 25 textures (front/side/top x active/inactive)
    │   ├── icon.png               # Mod icon
    │   └── lang/en_us.json        # Localization
    └── data/
        ├── chunkloader/
        │   ├── recipe/            # 5 shaped crafting recipes
        │   └── loot_table/blocks/ # 5 block loot tables (drops itself)
        └── minecraft/tags/block/  # Mining tags (pickaxe, needs_iron_tool)
```

## Building

### Prerequisites
- Java 21 (JDK)
- Internet connection (Gradle downloads dependencies)

### Build Commands
```bash
# Build the mod JAR
./gradlew build

# The output JAR is at:
# build/libs/fabric-chunk-loader-1.0.0.jar
```

### Development Environment Setup
```bash
# Generate IDE run configurations (IntelliJ IDEA)
./gradlew genSources

# Run the Minecraft client with the mod loaded
./gradlew runClient

# Run a Minecraft server with the mod loaded
./gradlew runServer
```

### Installation
1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.1
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.1
3. Copy `fabric-chunk-loader-1.0.0.jar` to your `.minecraft/mods/` folder
4. Launch Minecraft with the Fabric profile

## Extending with More Tiers

To add a new chunk loader tier:

1. Add a new value to `ChunkLoaderTier` enum
2. Register a new block in `ModBlocks` with the tier
3. Register the block item in `ModItems` and add to the item group
4. Add the new block to `ModBlockEntities.CHUNK_LOADER_BLOCK_ENTITY` builder
5. Add tier toggle to `ModConfig`
6. Create blockstate, model, and texture files
7. Create recipe and loot table JSON files
8. Add lang entries
9. Add to mining tags

## Dependencies
- Minecraft 1.21.1
- Fabric Loader >= 0.16.5
- Fabric API 0.104.0+1.21.1

## License
MIT License
