package com.hyprank.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TextureDataPayload(String style, String rankName, byte[] pngData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TextureDataPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.parse("hyprrank:texture_data"));

    public static final StreamCodec<FriendlyByteBuf, TextureDataPayload> CODEC =
            StreamCodec.of(TextureDataPayload::encode, TextureDataPayload::decode);

    private static void encode(FriendlyByteBuf buf, TextureDataPayload payload) {
        buf.writeUtf(payload.style);
        buf.writeUtf(payload.rankName);
        buf.writeVarInt(payload.pngData.length);
        buf.writeBytes(payload.pngData);
    }

    private static TextureDataPayload decode(FriendlyByteBuf buf) {
        String style = buf.readUtf();
        String rankName = buf.readUtf();
        int length = buf.readVarInt();
        byte[] data = new byte[length];
        buf.readBytes(data);
        return new TextureDataPayload(style, rankName, data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
