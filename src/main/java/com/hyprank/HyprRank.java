package com.hyprank;

import com.hyprank.client.RankManager;
import com.hyprank.network.SyncPayload;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HyprRank implements ModInitializer {
    public static final String MOD_ID = "hyprrank";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("HyprRank initializing...");

        PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String json = buildSyncJson();
            if (json != null) {
                ServerPlayNetworking.send(handler.getPlayer(), new SyncPayload(json));
            }
        });

        LOGGER.info("HyprRank initialized!");
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
}
