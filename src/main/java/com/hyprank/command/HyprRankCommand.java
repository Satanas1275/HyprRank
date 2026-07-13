package com.hyprank.command;

import com.hyprank.client.RankManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class HyprRankCommand {

    private static final SuggestionProvider<FabricClientCommandSource> VARIANT_SUGGESTIONS =
            (context, builder) -> {
                for (String variant : RankManager.getAvailableVariants()) {
                    builder.suggest(variant);
                }
                return builder.buildFuture();
            };

    public static void register() {}

    public static void registerClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("hyprrank")
                    .then(ClientCommands.literal("style")
                            .then(ClientCommands.argument("variant", StringArgumentType.word())
                                    .suggests(VARIANT_SUGGESTIONS)
                                    .executes(context -> {
                                        String variant = StringArgumentType.getString(context, "variant");
                                        boolean ok = RankManager.setVariant(variant);
                                        if (ok) {
                                            context.getSource().sendFeedback(
                                                    Component.literal("HyprRank style: " + variant));
                                        } else {
                                            context.getSource().sendFeedback(Component.literal(
                                                    "Style inconnu '" + variant + "'. Disponibles: "
                                                            + String.join(", ", RankManager.getAvailableVariants())));
                                        }
                                        return ok ? 1 : 0;
                                    })
                            )
                            .executes(context -> {
                                context.getSource().sendFeedback(Component.literal(
                                        "Style actuel: " + RankManager.getCurrentVariant()
                                                + " | Disponibles: "
                                                + String.join(", ", RankManager.getAvailableVariants())));
                                return 1;
                            })
                    )
            );
        });
    }
}
