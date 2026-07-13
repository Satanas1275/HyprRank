package com.hyprank.command;

import com.hyprank.client.RankManager;
import com.hyprank.client.TextFormatParser;
import com.hyprank.client.TextStyleStore;
import com.hyprank.network.ReloadRequestPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HyprRankCommand {

    private static final SuggestionProvider<FabricClientCommandSource> VARIANT_SUGGESTIONS =
            (context, builder) -> {
                for (String variant : RankManager.getAvailableVariants()) {
                    builder.suggest(variant);
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<FabricClientCommandSource> RELOAD_SUGGESTIONS =
            (context, builder) -> {
                builder.suggest("all");
                builder.suggest("styles");
                builder.suggest("textures");
                builder.suggest("texte");
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
                    .then(ClientCommands.literal("reload")
                            .then(ClientCommands.argument("target", StringArgumentType.word())
                                    .suggests(RELOAD_SUGGESTIONS)
                                    .executes(context -> {
                                        String target = StringArgumentType.getString(context, "target");
                                        ClientPlayNetworking.send(new ReloadRequestPayload(target));
                                        context.getSource().sendFeedback(
                                                Component.literal("HyprRank: rechargement '" + target + "' demandé..."));
                                        return 1;
                                    })
                            )
                            .executes(context -> {
                                ClientPlayNetworking.send(new ReloadRequestPayload("all"));
                                context.getSource().sendFeedback(
                                        Component.literal("HyprRank: rechargement complet demandé..."));
                                return 1;
                            })
                    )
            );
        });
    }
}
