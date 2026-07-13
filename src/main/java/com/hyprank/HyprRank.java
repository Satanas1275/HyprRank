package com.hyprank;

import com.hyprank.client.RankManager;
import com.hyprank.network.*;
import com.hyprank.server.ServerConfig;
import com.hyprank.server.ServerTextureManager;
import com.hyprank.server.ServerHyprRankCommand;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HyprRank implements ModInitializer {
    public static final String MOD_ID = "hyprrank";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("HyprRank initializing...");

        ServerConfig.init();
        ServerTextureManager.init();

        PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(StyleListPayload.TYPE, StyleListPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TextureDataPayload.TYPE, TextureDataPayload.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(TextureRequestPayload.TYPE, TextureRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ReloadRequestPayload.TYPE, ReloadRequestPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TextureRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            for (String style : payload.styles()) {
                var styleTextures = ServerTextureManager.getStyleTextures(style);
                if (styleTextures == null) continue;
                for (java.util.Map.Entry<String, byte[]> entry : styleTextures.entrySet()) {
                    ServerPlayNetworking.send(player, new TextureDataPayload(
                            style, entry.getKey(), entry.getValue()));
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ReloadRequestPayload.TYPE, (payload, context) -> {
            ServerHyprRankCommand.executeReload(context.player().level().getServer(), payload.target());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String syncJson = buildSyncJson();
            if (syncJson != null) {
                ServerPlayNetworking.send(handler.getPlayer(), new SyncPayload(syncJson));
            }

            String styleJson = buildStyleListJson();
            if (styleJson != null) {
                ServerPlayNetworking.send(handler.getPlayer(), new StyleListPayload(styleJson));
            }
        });

        LOGGER.info("HyprRank initialized!");
    }

    public static void sendStyleListToAll(net.minecraft.server.MinecraftServer server) {
        String styleJson = buildStyleListJson();
        if (styleJson == null) return;
        StyleListPayload payload = new StyleListPayload(styleJson);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static String buildSyncJson() {
        try {
            Gson gson = new Gson();
            JsonObject root = new JsonObject();
            JsonObject ranks = new JsonObject();

            for (java.util.Map.Entry<Character, RankManager.RankData> entry : RankManager.getMappings().entrySet()) {
                JsonObject rankObj = new JsonObject();
                rankObj.addProperty("glyphCode", (int) entry.getKey());
                rankObj.addProperty("color", entry.getValue().color);
                ranks.add(entry.getValue().name, rankObj);
            }

            root.add("ranks", ranks);
            return gson.toJson(root);
        } catch (Exception e) {
            LOGGER.error("Failed to build sync JSON", e);
            return null;
        }
    }

    private static String buildStyleListJson() {
        try {
            Gson gson = new Gson();
            JsonObject root = new JsonObject();
            JsonObject styles = new JsonObject();
            JsonObject textFormats = new JsonObject();

            var hashes = ServerTextureManager.getStyleHashes();
            for (java.util.Map.Entry<String, ServerConfig.StyleDef> entry : ServerConfig.getStyles().entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", entry.getValue().type());
                if (hashes.containsKey(entry.getKey())) {
                    obj.addProperty("hash", hashes.get(entry.getKey()));
                    var textures = ServerTextureManager.getStyleTextures(entry.getKey());
                    if (textures != null) {
                        obj.addProperty("count", textures.size());
                    }
                }
                if (entry.getValue().format() != null) {
                    obj.addProperty("format", entry.getValue().format());
                }
                styles.add(entry.getKey(), obj);
            }

            for (java.util.Map.Entry<String, java.util.Map<String, String>> entry : ServerConfig.getTexteStyles().entrySet()) {
                JsonObject textObj = new JsonObject();
                String defaultFormat = entry.getValue().get("__default__");
                if (defaultFormat != null) {
                    textObj.addProperty("default", defaultFormat);
                }
                JsonObject ranksObj = new JsonObject();
                for (java.util.Map.Entry<String, String> rankEntry : entry.getValue().entrySet()) {
                    if ("__default__".equals(rankEntry.getKey())) continue;
                    ranksObj.addProperty(rankEntry.getKey(), rankEntry.getValue());
                }
                if (ranksObj.size() > 0) {
                    textObj.add("ranks", ranksObj);
                }
                textFormats.add(entry.getKey(), textObj);
            }

            root.add("styles", styles);
            root.add("textFormats", textFormats);
            return gson.toJson(root);
        } catch (Exception e) {
            LOGGER.error("Failed to build style list JSON", e);
            return null;
        }
    }
}
