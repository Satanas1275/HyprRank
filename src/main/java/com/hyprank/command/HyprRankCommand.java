package com.hyprank.command;

import com.hyprank.client.RankManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

public class HyprRankCommand {

    public static void register() {}

    public static void registerClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("hyprank")
                    .then(ClientCommands.literal("style")
                            .executes(context -> {
                                RankManager.toggleMode();
                                String modeName = RankManager.getCurrentMode() == RankManager.RenderMode.IMAGE
                                        ? "IMAGE (textures)" : "TEXTE (colored text)";
                                context.getSource().sendFeedback(
                                        Component.literal("HyprRank mode: " + modeName));
                                return 1;
                            })
                    )
            );
        });
    }
}
