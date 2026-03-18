package com.fabricchunkloader.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

/**
 * Data sent from server to client when opening a chunk loader screen.
 */
public record ChunkLoaderScreenData(
        BlockPos pos,
        int enabled,
        int tierOrdinal,
        int centered,
        int facingHorizontal,
        String customName,
        String ownerName
) {
    public static final PacketCodec<RegistryByteBuf, ChunkLoaderScreenData> PACKET_CODEC = new PacketCodec<>() {
        @Override
        public ChunkLoaderScreenData decode(RegistryByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int enabled = buf.readVarInt();
            int tierOrdinal = buf.readVarInt();
            int centered = buf.readVarInt();
            int facingHorizontal = buf.readVarInt();
            String customName = buf.readString(256);
            String ownerName = buf.readString(256);
            return new ChunkLoaderScreenData(pos, enabled, tierOrdinal, centered, facingHorizontal, customName, ownerName);
        }

        @Override
        public void encode(RegistryByteBuf buf, ChunkLoaderScreenData data) {
            buf.writeBlockPos(data.pos());
            buf.writeVarInt(data.enabled());
            buf.writeVarInt(data.tierOrdinal());
            buf.writeVarInt(data.centered());
            buf.writeVarInt(data.facingHorizontal());
            buf.writeString(data.customName(), 256);
            buf.writeString(data.ownerName(), 256);
        }
    };
}
