package com.hyprank.mixin;

import com.hyprank.HyprRank;
import com.hyprank.client.TextRankReplacer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatHudMixin {

    @ModifyVariable(
            method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component hyprrank$replaceGlyphs(Component message) {
        HyprRank.LOGGER.info("[hyprrank-debug] mixin hit, raw string='{}' codepoints={}",
                message.getString(), debugCodepoints(message.getString()));
        Component result = TextRankReplacer.processMessage(message);
        HyprRank.LOGGER.info("[hyprrank-debug] result string='{}'", result.getString());
        return result;
    }

    private static String debugCodepoints(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(String.format("U+%04X ", (int) s.charAt(i)));
        }
        return sb.toString();
    }
}
