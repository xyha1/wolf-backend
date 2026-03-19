package com.game.werewolf.voice.domain;

public record ChannelDefinition(ChannelId channelId,
                                Kind kind,
                                boolean active,
                                int delayedMs,
                                ChannelId sourceChannelId) {
    public enum Kind {
        PUBLIC,
        PRIVATE,
        SPECTATOR
    }
}
