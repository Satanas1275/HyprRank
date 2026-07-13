package com.hyprank.client;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RankManager {

    /**
     * Built-in variant name for the bracket-text fallback ("[Member]" etc.),
     * always available even if no texture variants are registered.
     */
    public static final String TEXT_VARIANT = "text";

    /** Default built-in texture variant, backed by the bundled font. */
    public static final String DEFAULT_TEXTURE_VARIANT = "texture";

    private static String currentVariant = DEFAULT_TEXTURE_VARIANT;

    // variant name -> font Identifier. null value = bracket-text mode (TEXT_VARIANT).
    private static final Map<String, Identifier> variants = new LinkedHashMap<>();

    private static final Map<Character, RankData> localMappings = new LinkedHashMap<>();
    private static final Map<String, Character> rankToChar = new LinkedHashMap<>();

    static {
        register('\uE800', "Member", 0x55FF55);
        register('\uE801', "Media", 0x55FFFF);
        register('\uE802', "Manager", 0x00AAAA);
        register('\uE803', "Helper", 0xFFFF55);
        register('\uE804', "MVP", 0x55FFFF);
        register('\uE805', "MVP+", 0x55FFFF);
        register('\uE806', "MVP++", 0x55FFFF);
        register('\uE807', "Mod", 0x55FF55);
        register('\uE808', "Noob", 0xAAAAAA);
        register('\uE809', "Owner", 0xFF5555);
        register('\uE80A', "Admin", 0xFF5555);
        register('\uE80B', "Pro", 0xFFAA00);
        register('\uE80C', "Twitch", 0xAA55FF);
        register('\uE80D', "OG", 0xAA00AA);
        register('\uE80E', "Builder", 0xFFFF55);
        register('\uE80F', "VIP", 0x55FF55);
        register('\uE810', "VIP+", 0x55FF55);
        register('\uE811', "Dev", 0x55FFFF);
        register('\uE812', "Designer", 0xFF55FF);
        register('\uE813', "God", 0xFFAA00);
        register('\uE814', "Elite", 0x55FFFF);
        register('\uE815', "Elite+", 0x55FFFF);
        register('\uE816', "YouTube", 0xFF5555);
        register('\uE817', "Broadcast", 0xFF5555);
        register('\uE818', "Partner", 0xFFAA00);
        register('\uE819', "NPC", 0xFFFF55);

        variants.put(TEXT_VARIANT, null);
        variants.put(DEFAULT_TEXTURE_VARIANT, Identifier.parse("hyprrank:rank_glyphs"));
    }

    private static void register(char glyph, String name, int color) {
        RankData data = new RankData(name, color);
        localMappings.put(glyph, data);
        rankToChar.put(name, glyph);
    }

    public static void init() {
        com.hyprank.HyprRank.LOGGER.info("[hyprrank] {} ranks, {} style variants available: {}",
                localMappings.size(), variants.size(), variants.keySet());
    }

    // ---- Style / variant handling ----

    public static String getCurrentVariant() { return currentVariant; }

    public static Set<String> getAvailableVariants() { return variants.keySet(); }

    public static boolean isValidVariant(String name) { return variants.containsKey(name); }

    /**
     * Registers (or overrides) a style variant. Pass fontId=null for a bracket-text
     * style, or a font resource Identifier (e.g. "hyprrank:rank_glyphs_neon") for an
     * image style backed by a font provider bundled in a resource pack.
     */
    public static void registerVariant(String name, Identifier fontId) {
        variants.put(name, fontId);
    }

    /**
     * Switches the active style and triggers a live re-render of the chat history
     * so already-displayed messages pick up the new style immediately.
     */
    public static boolean setVariant(String name) {
        if (!isValidVariant(name)) return false;
        if (name.equals(currentVariant)) return true;
        currentVariant = name;
        ChatHistoryStore.replayAll();
        return true;
    }

    /** Font to use for the current variant, or null if the current variant is bracket-text. */
    public static Identifier getCurrentFont() {
        return variants.get(currentVariant);
    }

    public static boolean isCurrentVariantText() {
        return getCurrentFont() == null;
    }

    // ---- Rank char lookup ----

    public static RankData getRankByChar(char c) { return localMappings.get(c); }

    public static Character getCharByRank(String name) { return rankToChar.get(name); }

    public static boolean isGlyphChar(char c) { return localMappings.containsKey(c); }

    public static void updateFromServer(Map<Character, RankData> serverData) {
        localMappings.clear();
        rankToChar.clear();
        localMappings.putAll(serverData);
        for (Map.Entry<Character, RankData> entry : serverData.entrySet()) {
            rankToChar.put(entry.getValue().name, entry.getKey());
        }
    }

    public static Map<Character, RankData> getMappings() { return localMappings; }

    public static class RankData {
        public String name;
        public int color;

        public RankData(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }
}
