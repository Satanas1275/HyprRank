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
        register('\uE000', "Member", 0x55FF55);
        register('\uE001', "Media", 0x55FFFF);
        register('\uE002', "Manager", 0x00AAAA);
        register('\uE003', "Helper", 0xFFFF55);
        register('\uE004', "MVP", 0x55FFFF);
        register('\uE005', "MVP+", 0x55FFFF);
        register('\uE006', "MVP++", 0x55FFFF);
        register('\uE007', "Mod", 0x55FF55);
        register('\uE008', "Noob", 0xAAAAAA);
        register('\uE009', "Owner", 0xFF5555);
        register('\uE00A', "Admin", 0xFF5555);
        register('\uE00B', "Pro", 0xFFAA00);
        register('\uE00C', "Twitch", 0xAA55FF);
        register('\uE00D', "OG", 0xAA00AA);
        register('\uE00E', "Builder", 0xFFFF55);
        register('\uE00F', "VIP", 0x55FF55);
        register('\uE010', "VIP+", 0x55FF55);
        register('\uE011', "Dev", 0x55FFFF);
        register('\uE012', "Designer", 0xFF55FF);
        register('\uE013', "God", 0xFFAA00);
        register('\uE014', "Elite", 0x55FFFF);
        register('\uE015', "Elite+", 0x55FFFF);
        register('\uE016', "YouTube", 0xFF5555);
        register('\uE017', "Broadcast", 0xFF5555);
        register('\uE018', "Partner", 0xFFAA00);
        register('\uE019', "NPC", 0xFFFF55);
    }

    private static void register(char glyph, String name, int color) {
        RankData data = new RankData(name, color);
        localMappings.put(glyph, data);
        rankToChar.put(name, glyph);
    }

    public static void init() {}

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
