package com.hyprank.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hyprank.HyprRank;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerSyncHandler {
    private static final Gson GSON = new Gson();

    public static void handleSync(String jsonData) {
        try {
            JsonObject root = GSON.fromJson(jsonData, JsonObject.class);
            JsonObject ranks = root.getAsJsonObject("ranks");

            if (ranks == null) return;

            Map<Character, RankManager.RankData> serverData = new LinkedHashMap<>();

            for (Map.Entry<String, com.google.gson.JsonElement> entry : ranks.entrySet()) {
                String rankName = entry.getKey();
                JsonObject rankObj = entry.getValue().getAsJsonObject();

                int glyphCode = rankObj.get("glyphCode").getAsInt();
                int color = rankObj.get("color").getAsInt();

                serverData.put((char) glyphCode, new RankManager.RankData(rankName, color));
            }

            if (!serverData.isEmpty()) {
                RankManager.updateFromServer(serverData);
                HyprRank.LOGGER.info("Synced {} ranks from server", serverData.size());
            }
        } catch (Exception e) {
            HyprRank.LOGGER.error("Failed to parse server sync data", e);
        }
    }
}
