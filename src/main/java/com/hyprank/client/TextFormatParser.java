package com.hyprank.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;

public class TextFormatParser {

    public static Component parse(String format, String rankName, int defaultColor) {
        if (format == null || format.isEmpty()) {
            return Component.literal("[" + rankName + "]")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(defaultColor)));
        }

        String resolved = format.replace("{rank}", rankName);
        return parseFormattingCodes(resolved, defaultColor);
    }

    public static MutableComponent parseFormattingCodes(String text, int defaultColor) {
        MutableComponent result = Component.empty();
        StringBuilder currentText = new StringBuilder();
        Style currentStyle = Style.EMPTY.withColor(TextColor.fromRgb(defaultColor));

        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '\u00A7' && i + 1 < text.length()) {
                if (currentText.length() > 0) {
                    result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                    currentText.setLength(0);
                }

                char code = Character.toLowerCase(text.charAt(i + 1));
                ChatFormatting formatting = getByCode(code);

                if (formatting != null) {
                    if (formatting == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (formatting.isColor()) {
                        currentStyle = currentStyle.withColor(formatting.getColor());
                    } else {
                        currentStyle = currentStyle.applyLegacyFormat(formatting);
                    }
                } else if (code >= '0' && code <= '9' || code >= 'a' && code <= 'f') {
                    int colorIndex = code >= '0' && code <= '9' ? code - '0' : code - 'a' + 10;
                    currentStyle = currentStyle.withColor(TextColor.fromRgb(COLOR_MAP[colorIndex]));
                }

                i += 2;
            } else {
                currentText.append(text.charAt(i));
                i++;
            }
        }

        if (currentText.length() > 0) {
            result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
        }

        return result;
    }

    private static final int[] COLOR_MAP = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
        0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
        0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private static ChatFormatting getByCode(char code) {
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.getChar() == code) {
                return formatting;
            }
        }
        return null;
    }
}
