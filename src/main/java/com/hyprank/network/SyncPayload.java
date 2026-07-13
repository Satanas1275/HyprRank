package com.hyprank.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncPayload(String jsonData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.parse("hyprrank:sync"));

    public static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC =
            StreamCodec.of(SyncPayload::encode, SyncPayload::decode);

    private static void encode(FriendlyByteBuf buf, SyncPayload payload) {
        buf.writeUtf(payload.jsonData);
    }

    private static SyncPayload decode(FriendlyByteBuf buf) {
        return new SyncPayload(buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
