package com.hyprank.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReloadRequestPayload(String target) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReloadRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.parse("hyprrank:reload_request"));

    public static final StreamCodec<FriendlyByteBuf, ReloadRequestPayload> CODEC =
            StreamCodec.of(ReloadRequestPayload::encode, ReloadRequestPayload::decode);

    private static void encode(FriendlyByteBuf buf, ReloadRequestPayload payload) {
        buf.writeUtf(payload.target);
    }

    private static ReloadRequestPayload decode(FriendlyByteBuf buf) {
        return new ReloadRequestPayload(buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
