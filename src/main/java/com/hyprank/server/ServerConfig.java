package com.hyprank.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyprank.HyprRank;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configDir;
    private static Path stylesFile;
    private static Path texturesDir;
    private static Path texteDir;

    private static final Map<String, StyleDef> styles = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> texteStyles = new LinkedHashMap<>();
    private static String defaultStyle = "texture";

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            configDir = FabricLoader.getInstance().getConfigDir().resolve("HyprRank");
            stylesFile = configDir.resolve("styles.json");
            texturesDir = configDir.resolve("textures");
            texteDir = configDir.resolve("texte");
            load();
        });
    }

    public static void load() {
        try {
            Files.createDirectories(configDir);
            Files.createDirectories(texturesDir);
            Files.createDirectories(texteDir);
        } catch (IOException e) {
            HyprRank.LOGGER.error("Failed to create config directories", e);
            return;
        }

        if (!Files.exists(stylesFile)) {
            createDefaults();
        }

        try {
            String json = Files.readString(stylesFile);
            JsonObject root = GSON.fromJson(json, JsonObject.class);

            styles.clear();
            if (root.has("default_style")) {
                defaultStyle = root.get("default_style").getAsString();
            }
            if (root.has("styles")) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("styles").entrySet()) {
                    String name = entry.getKey();
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    String type = obj.has("type") ? obj.get("type").getAsString() : "texture";
                    String directory = obj.has("directory") ? obj.get("directory").getAsString() : name;
                    String description = obj.has("description") ? obj.get("description").getAsString() : "";
                    String format = obj.has("format") ? obj.get("format").getAsString() : null;
                    styles.put(name, new StyleDef(type, directory, description, format));
                }
            }

            loadTexteStyles();
            discoverNewTextureFolders();

            HyprRank.LOGGER.info("[HyprRank] Loaded {} styles + {} texte styles from server config",
                    styles.size(), texteStyles.size());
        } catch (Exception e) {
            HyprRank.LOGGER.error("Failed to load styles.json", e);
            createDefaults();
        }
    }

    /**
     * Picks up any folder under textures/ that isn't referenced by an existing
     * style yet and registers it automatically as a new texture-type style
     * (named after the folder), so admins can just drop a folder + PNGs and
     * run /hyprrank reload instead of hand-editing styles.json.
     */
    private static void discoverNewTextureFolders() {
        if (!Files.isDirectory(texturesDir)) return;

        boolean changed = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(texturesDir)) {
            for (Path folder : stream) {
                if (!Files.isDirectory(folder)) continue;
                String folderName = folder.getFileName().toString();

                boolean alreadyUsed = styles.values().stream()
                        .anyMatch(def -> folderName.equals(def.directory()));
                if (alreadyUsed) continue;

                styles.put(folderName, new StyleDef("texture", folderName,
                        "Style auto-detecte depuis textures/" + folderName, null));
                changed = true;
                HyprRank.LOGGER.info("[HyprRank] Auto-registered new texture style '{}' from folder '{}'",
                        folderName, folderName);
            }
        } catch (IOException e) {
            HyprRank.LOGGER.error("Failed to scan textures directory for new styles", e);
        }

        if (changed) save();
    }

    private static void loadTexteStyles() {
        texteStyles.clear();

        if (!Files.isDirectory(texteDir)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(texteDir, "*.json")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString().replace(".json", "");
                try {
                    String json = Files.readString(file);
                    JsonObject root = GSON.fromJson(json, JsonObject.class);

                    Map<String, String> rankFormats = new LinkedHashMap<>();
                    if (root.has("ranks")) {
                        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("ranks").entrySet()) {
                            rankFormats.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }

                    String defaultFormat = root.has("default") ? root.get("default").getAsString() : null;

                    Map<String, String> styleData = new LinkedHashMap<>();
                    if (defaultFormat != null) {
                        styleData.put("__default__", defaultFormat);
                    }
                    styleData.putAll(rankFormats);

                    texteStyles.put(fileName, styleData);

                    styles.put("texte_" + fileName, new StyleDef("text", fileName,
                            "Style texte custom: " + fileName, null));

                    HyprRank.LOGGER.info("[HyprRank] Loaded texte style '{}' ({} rank overrides)",
                            fileName, rankFormats.size());
                } catch (Exception e) {
                    HyprRank.LOGGER.error("Failed to load texte style '{}'", fileName, e);
                }
            }
        } catch (IOException e) {
            HyprRank.LOGGER.error("Failed to read texte directory", e);
        }
    }

    private static void createDefaults() {
        styles.clear();
        texteStyles.clear();
        styles.put("text", new StyleDef("text", "text", "Style texte classique", null));
        styles.put("texture", new StyleDef("texture", "default", "Style texture par defaut", null));
        defaultStyle = "texture";
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(configDir);
            JsonObject root = new JsonObject();
            root.addProperty("default_style", defaultStyle);

            JsonObject stylesObj = new JsonObject();
            for (Map.Entry<String, StyleDef> entry : styles.entrySet()) {
                if (entry.getValue().type().equals("text") && entry.getKey().startsWith("texte_")) continue;
                JsonObject obj = new JsonObject();
                obj.addProperty("type", entry.getValue().type());
                obj.addProperty("directory", entry.getValue().directory());
                obj.addProperty("description", entry.getValue().description());
                if (entry.getValue().format() != null) {
                    obj.addProperty("format", entry.getValue().format());
                }
                stylesObj.add(entry.getKey(), obj);
            }
            root.add("styles", stylesObj);

            Files.writeString(stylesFile, GSON.toJson(root));
        } catch (IOException e) {
            HyprRank.LOGGER.error("Failed to save styles.json", e);
        }
    }

    public static void reload() {
        load();
    }

    public static Map<String, StyleDef> getStyles() { return styles; }
    public static StyleDef getStyle(String name) { return styles.get(name); }
    public static String getDefaultStyle() { return defaultStyle; }
    public static Path getTexturesDir() { return texturesDir; }
    public static Path getTexteDir() { return texteDir; }
    public static Path getConfigDir() { return configDir; }
    public static Map<String, Map<String, String>> getTexteStyles() { return texteStyles; }

    public record StyleDef(String type, String directory, String description, String format) {}
}
