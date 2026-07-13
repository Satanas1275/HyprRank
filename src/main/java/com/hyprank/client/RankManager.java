package com.hyprank.client;

import net.minecraft.ChatFormatting;
import java.util.LinkedHashMap;
import java.util.Map;

public class RankManager {
    public enum RenderMode { IMAGE, TEXT }

    private static RenderMode currentMode = RenderMode.IMAGE;
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
    }

    private static void register(char glyph, String name, int color) {
        RankData data = new RankData(name, color);
        localMappings.put(glyph, data);
        rankToChar.put(name, glyph);
    }

    public static void init() {
        StringBuilder sb = new StringBuilder();
        for (Character c : localMappings.keySet()) {
            sb.append(String.format("U+%04X ", (int) c));
        }
        com.hyprank.HyprRank.LOGGER.info("[hyprrank-debug] registered glyph codepoints: {}", sb);
    }

    public static RenderMode getCurrentMode() { return currentMode; }

    public static void toggleMode() {
        currentMode = (currentMode == RenderMode.IMAGE) ? RenderMode.TEXT : RenderMode.IMAGE;
    }

    public static void setMode(RenderMode mode) { currentMode = mode; }

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
