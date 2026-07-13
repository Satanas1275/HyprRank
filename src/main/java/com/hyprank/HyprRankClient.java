package com.hyprank;

import com.hyprank.client.RankManager;
import com.hyprank.client.ServerSyncHandler;
import com.hyprank.client.TextureCache;
import com.hyprank.command.HyprRankCommand;
import com.hyprank.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;

public class HyprRankClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RankManager.init();
        HyprRankCommand.registerClient();

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) -> {
            ServerSyncHandler.handleSync(payload.jsonData());
        });

        ClientPlayNetworking.registerGlobalReceiver(StyleListPayload.TYPE, (payload, context) -> {
            TextureCache.handleStyleList(payload.jsonData());
            List<String> requested = TextureCache.getRequestedStyles();
            if (!requested.isEmpty()) {
                ClientPlayNetworking.send(new TextureRequestPayload(requested));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(TextureDataPayload.TYPE, (payload, context) -> {
            TextureCache.receiveTexture(payload.style(), payload.rankName(), payload.pngData());
        });
    }
}
