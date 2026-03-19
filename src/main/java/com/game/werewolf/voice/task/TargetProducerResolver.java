package com.game.werewolf.voice.task;

import com.game.werewolf.rtcbridge.model.ProducerRegistryRow;
import com.game.werewolf.voice.domain.ChannelId;
import com.game.werewolf.voice.domain.PlayerPermissionRow;
import com.game.werewolf.voice.domain.RoomVoicePermissionTable;

import java.util.ArrayList;
import java.util.List;

public class TargetProducerResolver {

    public List<String> resolveTargetProducerIds(String listenerPlayerId,
                                                 ChannelId channelId,
                                                 RoomVoicePermissionTable permissionTable,
                                                 List<ProducerRegistryRow> producerRegistry) {
        List<String> targetProducerIds = new ArrayList<>();
        for (ProducerRegistryRow row : producerRegistry) {
            if (!row.online() || row.paused()) {
                continue;
            }
            if (!row.publishAclChannels().contains(channelId)) {
                continue;
            }
            PlayerPermissionRow publisher = permissionTable.rows().get(row.playerId());
            if (publisher == null || !publisher.speakChannels().contains(channelId)) {
                continue;
            }
            if (listenerPlayerId.equals(row.playerId())) {
                continue;
            }
            targetProducerIds.add(row.producerId());
        }
        return targetProducerIds;
    }
}
