package com.hyprank.server;

import com.hyprank.HyprRank;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class ServerHyprRankCommand {

    private static final SuggestionProvider<CommandSourceStack> RELOAD_SUGGESTIONS =
            (context, builder) -> {
                builder.suggest("all");
                builder.suggest("styles");
                builder.suggest("textures");
                builder.suggest("texte");
                for (String style : ServerConfig.getStyles().keySet()) {
                    builder.suggest(style);
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hyprrank")
                .then(Commands.literal("reload")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(RELOAD_SUGGESTIONS)
                                .executes(context -> {
                                    String target = StringArgumentType.getString(context, "target");
                                    return executeReload(context.getSource().getServer(), target);
                                })
                        )
                        .executes(context -> executeReload(context.getSource().getServer(), "all"))
                )
        );
    }

    public static int executeReload(MinecraftServer server, String target) {
        if (server == null) return 0;

        switch (target) {
            case "all" -> {
                ServerConfig.reload();
                ServerTextureManager.reload();
                HyprRank.sendStyleListToAll(server);
                broadcast(server, "HyprRank: reloaded all (config + textures + texte)");
            }
            case "styles" -> {
                ServerConfig.reload();
                broadcast(server, "HyprRank: reloaded styles config");
            }
            case "textures" -> {
                ServerConfig.reload();
                ServerTextureManager.reload();
                HyprRank.sendStyleListToAll(server);
                broadcast(server, "HyprRank: reloaded textures");
            }
            case "texte" -> {
                ServerConfig.reload();
                HyprRank.sendStyleListToAll(server);
                broadcast(server, "HyprRank: reloaded texte styles");
            }
            default -> {
                if (ServerConfig.getStyle(target) != null) {
                    ServerConfig.reload();
                    ServerTextureManager.reload();
                    HyprRank.sendStyleListToAll(server);
                    broadcast(server, "HyprRank: reloaded style '" + target + "'");
                } else {
                    broadcast(server, "HyprRank: unknown target '" + target + "'");
                    return 0;
                }
            }
        }
        return 1;
    }

    private static void broadcast(MinecraftServer server, String message) {
        for (var player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(message));
        }
        HyprRank.LOGGER.info(message);
    }
}
