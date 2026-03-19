package com.game.werewolf.voice.task;

import com.game.werewolf.voice.domain.ChannelId;

public record VoiceExecutionTask(String taskId,
                                 String roomId,
                                 long revision,
                                 String playerId,
                                 VoiceExecutionAction action,
                                 ChannelId channelId,
                                 String targetProducerId,
                                 Boolean allow,
                                 Integer delayedMs,
                                 int order) {
}
