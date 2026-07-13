package com.hyprank.mixin;

import com.hyprank.client.ChatHistoryStore;
import com.hyprank.client.TextRankReplacer;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatHudMixin {

    // A single ModifyVariable hook captures all 3 args (message, signature, tag)
    // by matching the trailing parameters against the method's remaining locals.
    // We record the raw message before replacing it, all in one pass, so there's
    // no ordering dependency between separate injectors.
    @ModifyVariable(
            method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component hyprrank$replaceGlyphs(Component message, Component originalMessage,
                                              MessageSignature signature, GuiMessageTag tag) {
        ChatHistoryStore.record(originalMessage, signature, tag);
        return TextRankReplacer.processMessage(message);
    }
}
