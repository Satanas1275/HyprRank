package com.hyprank;

import com.hyprank.client.RankManager;
import com.hyprank.client.ServerSyncHandler;
import com.hyprank.command.HyprRankCommand;
import com.hyprank.network.SyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class HyprRankClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RankManager.init();
        HyprRankCommand.registerClient();

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) -> {
            ServerSyncHandler.handleSync(payload.jsonData());
        });
    }
}
