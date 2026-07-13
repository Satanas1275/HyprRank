package com.hyprank.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyprank.HyprRank;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TextureCache {

    private static final Gson GSON = new Gson();

    private static final Map<String, Map<String, byte[]>> textureData = new ConcurrentHashMap<>();
    private static final Map<String, String> serverHashes = new ConcurrentHashMap<>();
    private static final Map<String, Integer> expectedCounts = new ConcurrentHashMap<>();
    private static final Set<String> completeStyles = ConcurrentHashMap.newKeySet();
    private static final Set<String> builtFonts = ConcurrentHashMap.newKeySet();

    public static void handleStyleList(String jsonData) {
        try {
            JsonObject root = GSON.fromJson(jsonData, JsonObject.class);
            JsonObject styles = root.getAsJsonObject("styles");
            if (styles != null) {
                for (Map.Entry<String, JsonElement> entry : styles.entrySet()) {
                    String styleName = entry.getKey();
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    String type = obj.has("type") ? obj.get("type").getAsString() : "texture";
                    String hash = obj.has("hash") ? obj.get("hash").getAsString() : null;
                    int count = obj.has("count") ? obj.get("count").getAsInt() : 0;

                    if (!"texture".equals(type)) continue;

                    if (hash != null) {
                        serverHashes.put(styleName, hash);
                        expectedCounts.put(styleName, count);
                        String localHash = getLocalHash(styleName);
                        if (hash.equals(localHash)) {
                            completeStyles.add(styleName);
                            HyprRank.LOGGER.info("[HyprRank] Style '{}' hash match, using cache", styleName);
                        }
                    }
                }

                List<String> requested = getRequestedStyles();
                if (!requested.isEmpty()) {
                    HyprRank.LOGGER.info("[HyprRank] Requesting {} styles from server: {}", requested.size(), requested);
                }
            }

            if (root.has("textFormats")) {
                TextStyleStore.setTextFormats(GSON.toJson(root.get("textFormats")));
            }
        } catch (Exception e) {
            HyprRank.LOGGER.error("[HyprRank] Failed to parse style list", e);
        }
    }

    public static List<String> getRequestedStyles() {
        List<String> requested = new ArrayList<>();
        for (String style : serverHashes.keySet()) {
            if (!completeStyles.contains(style)) {
                requested.add(style);
            }
        }
        return requested;
    }

    public static void receiveTexture(String style, String rankName, byte[] pngData) {
        textureData.computeIfAbsent(style, k -> new ConcurrentHashMap<>()).put(rankName, pngData);
        checkForCompletion(style);
    }

    /**
     * A style becomes usable as soon as it has received every texture the
     * SERVER declared for it (not a fixed count of 26 - a style with 1 custom
     * rank texture is perfectly valid and should activate too).
     */
    private static void checkForCompletion(String style) {
        if (completeStyles.contains(style)) return;

        Integer expected = expectedCounts.get(style);
        Map<String, byte[]> got = textureData.get(style);
        int gotCount = got == null ? 0 : got.size();
        HyprRank.LOGGER.info("[hyprrank-debug] checkForCompletion('{}') expected={} got={}",
                style, expected, gotCount);

        if (expected == null || expected == 0 || got == null) return;
        if (got.size() < expected) return;

        markComplete(style);

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        HyprRank.LOGGER.info("[hyprrank-debug] style '{}' complete, triggering resource pack reload", style);
        if (mc != null) {
            mc.reloadResourcePacks();
        }
    }

    public static boolean isStyleComplete(String style) {
        Map<String, byte[]> textures = textureData.get(style);
        if (textures == null || textures.isEmpty()) return false;
        return textures.size() >= RankManager.getMappings().size();
    }

    public static Set<String> getCompletePendingStyles() {
        Set<String> result = new HashSet<>();
        for (String style : textureData.keySet()) {
            if (!completeStyles.contains(style) && isStyleComplete(style)) {
                result.add(style);
            }
        }
        return result;
    }

    public static void markComplete(String style) {
        completeStyles.add(style);
        saveLocalHash(style, serverHashes.get(style));
        HyprRank.LOGGER.info("[HyprRank] Style '{}' complete ({} textures)", style,
                textureData.getOrDefault(style, Map.of()).size());
    }

    public static void injectDynamicFonts(Map<Identifier, FontSet> fontSets, TextureManager textureManager) {
        HyprRank.LOGGER.info("[hyprrank-debug] injectDynamicFonts called, completeStyles={} builtFonts={}",
                completeStyles, builtFonts);
        for (String style : completeStyles) {
            if (builtFonts.contains(style)) continue;

            Map<String, byte[]> rankTextures = textureData.get(style);
            if (rankTextures == null || rankTextures.isEmpty()) continue;

            try {
                Identifier fontId = Identifier.parse("hyprrank:rank_glyphs_" + style);

                Map<Integer, UnbakedGlyph> glyphs = new HashMap<>();
                for (Map.Entry<String, byte[]> entry : rankTextures.entrySet()) {
                    Character glyphChar = RankManager.getCharByRank(entry.getKey());
                    if (glyphChar == null) continue;

                    NativeImage image = NativeImage.read(entry.getValue());
                    if (image == null) continue;

                    glyphs.put((int) glyphChar, new DynamicUnbakedGlyph(image, image.getWidth(), image.getHeight(), 8));
                }

                if (glyphs.isEmpty()) continue;

                DynamicGlyphProvider provider = new DynamicGlyphProvider(glyphs);
                GlyphProvider.Conditional conditional = new GlyphProvider.Conditional(provider, FontOption.Filter.ALWAYS_PASS);

                GlyphStitcher stitcher = new GlyphStitcher(textureManager, fontId);
                FontSet fontSet = new FontSet(stitcher);
                fontSet.reload(List.of(conditional), Set.of());

                fontSets.put(fontId, fontSet);
                builtFonts.add(style);

                RankManager.registerVariant(style, fontId);
                HyprRank.LOGGER.info("[HyprRank] Built dynamic font for '{}' with {} glyphs", style, glyphs.size());
            } catch (Exception e) {
                HyprRank.LOGGER.error("[HyprRank] Failed to build dynamic font for '{}'", style, e);
            }
        }
    }

    private static String getLocalHash(String style) { return null; }
    private static void saveLocalHash(String style, String hash) {}

    public static void clear() {
        textureData.clear();
        serverHashes.clear();
        completeStyles.clear();
        builtFonts.clear();
    }

    public static class DynamicGlyphProvider implements GlyphProvider {
        private final Map<Integer, UnbakedGlyph> glyphs;
        private final IntSet supportedGlyphs;

        public DynamicGlyphProvider(Map<Integer, UnbakedGlyph> glyphs) {
            this.glyphs = glyphs;
            this.supportedGlyphs = new IntOpenHashSet(glyphs.keySet());
        }

        @Override
        public UnbakedGlyph getGlyph(int codepoint) {
            return glyphs.get(codepoint);
        }

        @Override
        public IntSet getSupportedGlyphs() {
            return supportedGlyphs;
        }
    }

    public record DynamicUnbakedGlyph(NativeImage image, int width, int height, int ascent) implements UnbakedGlyph {
        @Override
        public GlyphInfo info() {
            return GlyphInfo.simple(width);
        }

        @Override
        public BakedGlyph bake(Stitcher stitcher) {
            return stitcher.stitch(info(), new GlyphBitmap() {
                @Override
                public int getPixelWidth() { return width; }

                @Override
                public int getPixelHeight() { return height; }

                @Override
                public void upload(int x, int y, GpuTexture texture) {
                    CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                    encoder.writeToTexture(texture, image, 0, 0, x, y, width, height, 0, 0);
                }

                @Override
                public boolean isColored() {
                    return image.format().components() > 1;
                }

                @Override
                public float getOversample() {
                    return 1.0f;
                }

                @Override
                public float getBearingTop() {
                    return ascent;
                }
            });
        }
    }
}
