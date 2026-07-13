package com.hyprank.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record TextureRequestPayload(List<String> styles) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TextureRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.parse("hyprrank:texture_request"));

    public static final StreamCodec<FriendlyByteBuf, TextureRequestPayload> CODEC =
            StreamCodec.of(TextureRequestPayload::encode, TextureRequestPayload::decode);

    private static void encode(FriendlyByteBuf buf, TextureRequestPayload payload) {
        buf.writeVarInt(payload.styles.size());
        for (String style : payload.styles) {
            buf.writeUtf(style);
        }
    }

    private static TextureRequestPayload decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<String> styles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            styles.add(buf.readUtf());
        }
        return new TextureRequestPayload(styles);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
