package com.game.werewolf.rtcbridge.model;

import com.game.werewolf.voice.domain.ChannelId;

import java.util.List;

public record ProducerRegistryRow(String playerId,
                                  String producerId,
                                  boolean online,
                                  boolean alive,
                                  boolean paused,
                                  SourceType sourceType,
                                  List<ChannelId> publishAclChannels) {
    public enum SourceType {
        PLAYER,
        SYSTEM
    }
}
