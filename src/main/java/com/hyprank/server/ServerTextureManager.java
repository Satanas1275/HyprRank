package com.hyprank.server;

import com.hyprank.HyprRank;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerTextureManager {

    private static final Map<String, Map<String, byte[]>> textures = new LinkedHashMap<>();
    private static final Map<String, String> styleHashes = new HashMap<>();

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> load());
    }

    public static void load() {
        textures.clear();
        styleHashes.clear();

        for (Map.Entry<String, ServerConfig.StyleDef> entry : ServerConfig.getStyles().entrySet()) {
            String styleName = entry.getKey();
            ServerConfig.StyleDef def = entry.getValue();

            if (!"texture".equals(def.type())) continue;

            Path styleDir = ServerConfig.getTexturesDir().resolve(def.directory());
            if (!Files.isDirectory(styleDir)) {
                HyprRank.LOGGER.warn("[HyprRank] Texture directory not found: {}", styleDir);
                continue;
            }

            Map<String, byte[]> styleTextures = new LinkedHashMap<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(styleDir, "*.png")) {
                for (Path png : stream) {
                    String rankName = png.getFileName().toString().replace(".png", "");
                    if (com.hyprank.client.RankManager.getCharByRank(rankName) == null) {
                        HyprRank.LOGGER.warn(
                                "[HyprRank] '{}' in style '{}' doesn't match any known rank name "
                                        + "(exact match needed, e.g. 'Member.png', 'MVP+.png', 'MVP++.png') "
                                        + "- it will be loaded but won't display anywhere.",
                                png.getFileName(), styleName);
                    }
                    byte[] data = Files.readAllBytes(png);
                    styleTextures.put(rankName, data);
                }
            } catch (IOException e) {
                HyprRank.LOGGER.error("Failed to load textures for style '{}'", styleName, e);
            }

            if (!styleTextures.isEmpty()) {
                textures.put(styleName, styleTextures);
                styleHashes.put(styleName, computeHash(styleTextures));
                HyprRank.LOGGER.info("[HyprRank] Loaded {} textures for style '{}'", styleTextures.size(), styleName);
            }
        }
    }

    public static void reload() {
        load();
    }

    public static Map<String, String> getStyleHashes() {
        return styleHashes;
    }

    public static byte[] getTexture(String style, String rankName) {
        Map<String, byte[]> styleTextures = textures.get(style);
        if (styleTextures == null) return null;
        return styleTextures.get(rankName);
    }

    public static Map<String, byte[]> getStyleTextures(String style) {
        return textures.get(style);
    }

    public static boolean hasStyle(String style) {
        return textures.containsKey(style);
    }

    private static String computeHash(Map<String, byte[]> styleTextures) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Map.Entry<String, byte[]> entry : styleTextures.entrySet()) {
                digest.update(entry.getKey().getBytes());
                digest.update(entry.getValue());
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "error";
        }
    }
}
