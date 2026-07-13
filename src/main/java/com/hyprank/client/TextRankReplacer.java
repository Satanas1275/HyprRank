package com.hyprank.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class TextRankReplacer {

    public static Component processMessage(Component original) {
        if (original == null) return null;
        if (!containsGlyph(original.getString())) return original;

        MutableComponent result = Component.empty();

        // component.visit() resolves the WHOLE tree (literal, translatable args,
        // siblings, etc.) into flat (style, text) chunks. Most chat messages are
        // built with Component.translatable(...), which is why walking getContents()
        // directly (the old approach) silently missed almost every real message.
        original.visit((style, text) -> {
            appendProcessed(result, text, style);
            return Optional.empty();
        }, Style.EMPTY);

        return result;
    }

    private static void appendProcessed(MutableComponent result, String text, Style style) {
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            RankManager.RankData rank = RankManager.getRankByChar(c);

            if (rank != null) {
                if (buffer.length() > 0) {
                    result.append(Component.literal(buffer.toString()).setStyle(style));
                    buffer.setLength(0);
                }

                Identifier font = RankManager.getCurrentFont();
                if (font != null) {
                    MutableComponent glyph = Component.literal(String.valueOf(c));
                    glyph.setStyle(style.withFont(new FontDescription.Resource(font)));
                    result.append(glyph);
                } else {
                    String rankText = "[" + rank.name + "]";
                    result.append(Component.literal(rankText).withStyle(
                            style.withColor(TextColor.fromRgb(rank.color))));
                }
            } else {
                buffer.append(c);
            }
        }

        if (buffer.length() > 0) {
            result.append(Component.literal(buffer.toString()).setStyle(style));
        }
    }

    public static boolean containsGlyph(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (RankManager.isGlyphChar(text.charAt(i))) return true;
        }
        return false;
    }
}
