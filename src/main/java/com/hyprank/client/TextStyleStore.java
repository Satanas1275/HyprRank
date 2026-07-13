package com.hyprank.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyprank.HyprRank;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TextStyleStore {

    private static final Gson GSON = new Gson();
    // styleName -> rankName -> format string (e.g. "§4[§3MeMbRe§4] §r")
    private static final Map<String, Map<String, String>> textStyles = new ConcurrentHashMap<>();
    // styleName -> default format string
    private static final Map<String, String> defaultFormats = new ConcurrentHashMap<>();

    public static void handleStyleList(String jsonData) {
        try {
            JsonObject root = GSON.fromJson(jsonData, JsonObject.class);
            JsonObject styles = root.getAsJsonObject("styles");
            if (styles == null) return;

            for (Map.Entry<String, JsonElement> entry : styles.entrySet()) {
                String styleName = entry.getKey();
                JsonObject obj = entry.getValue().getAsJsonObject();
                String type = obj.has("type") ? obj.get("type").getAsString() : "texture";

                if ("text".equals(type) && obj.has("format")) {
                    String format = obj.get("format").getAsString();
                    defaultFormats.put(styleName, format);
                }
            }
        } catch (Exception e) {
            HyprRank.LOGGER.error("[HyprRank] Failed to parse text styles from style list", e);
        }
    }

    public static void setTextFormats(String jsonData) {
        try {
            JsonObject root = GSON.fromJson(jsonData, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String styleName = entry.getKey();
                JsonObject obj = entry.getValue().getAsJsonObject();

                Map<String, String> rankFormats = new ConcurrentHashMap<>();
                String defaultFormat = null;

                if (obj.has("default")) {
                    defaultFormat = obj.get("default").getAsString();
                }
                if (obj.has("ranks")) {
                    for (Map.Entry<String, JsonElement> rankEntry : obj.getAsJsonObject("ranks").entrySet()) {
                        rankFormats.put(rankEntry.getKey(), rankEntry.getValue().getAsString());
                    }
                }

                textStyles.put("texte_" + styleName, rankFormats);
                if (defaultFormat != null) {
                    defaultFormats.put("texte_" + styleName, defaultFormat);
                }

                RankManager.registerVariant("texte_" + styleName, null);

                HyprRank.LOGGER.info("[HyprRank] Received text style '{}' with {} rank overrides",
                        styleName, rankFormats.size());
            }
        } catch (Exception e) {
            HyprRank.LOGGER.error("[HyprRank] Failed to parse text formats", e);
        }
    }

    public static String getRankFormat(String styleName, String rankName) {
        Map<String, String> ranks = textStyles.get(styleName);
        if (ranks != null) {
            String format = ranks.get(rankName);
            if (format != null) return format;
        }
        return defaultFormats.get(styleName);
    }

    public static boolean hasTextStyle(String styleName) {
        return defaultFormats.containsKey(styleName) || textStyles.containsKey(styleName);
    }

    public static void clear() {
        textStyles.clear();
        defaultFormats.clear();
    }
}
