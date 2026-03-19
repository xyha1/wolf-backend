package com.game.werewolf.voice.diff;

import com.game.werewolf.voice.domain.ChannelId;

public record ChannelStateDiff(ChannelId channelId,
                               ValueChange<Boolean> activeChanged,
                               ValueChange<Integer> delayedMsChanged) {
}
