package com.hyprank.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StyleListPayload(String jsonData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StyleListPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.parse("hyprrank:style_list"));

    public static final StreamCodec<FriendlyByteBuf, StyleListPayload> CODEC =
            StreamCodec.of(StyleListPayload::encode, StyleListPayload::decode);

    private static void encode(FriendlyByteBuf buf, StyleListPayload payload) {
        buf.writeUtf(payload.jsonData);
    }

    private static StyleListPayload decode(FriendlyByteBuf buf) {
        return new StyleListPayload(buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
