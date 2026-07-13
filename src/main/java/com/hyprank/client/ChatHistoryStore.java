package com.hyprank.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Keeps a bounded history of RAW (pre-glyph-replacement) chat messages so that
 * changing the active style variant can re-render everything already on screen,
 * instead of only affecting messages received afterward.
 */
public class ChatHistoryStore {

    private static final int MAX_HISTORY = 200;
    private static final Deque<Entry> history = new ArrayDeque<>();

    // Guards against re-recording our own replayed messages as new history entries.
    private static boolean replaying = false;

    private record Entry(Component message, MessageSignature signature, GuiMessageTag tag) {}

    public static boolean isReplaying() { return replaying; }

    public static void record(Component message, MessageSignature signature, GuiMessageTag tag) {
        if (replaying) return;
        history.addLast(new Entry(message, signature, tag));
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }

    public static void replayAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        ChatComponent chat = mc.gui.getChat();
        if (chat == null) return;

        replaying = true;
        try {
            chat.clearMessages(false);
            for (Entry entry : history) {
                chat.addPlayerMessage(entry.message(), entry.signature(), entry.tag());
            }
        } finally {
            replaying = false;
        }
    }

    public static void clear() {
        history.clear();
    }
}
