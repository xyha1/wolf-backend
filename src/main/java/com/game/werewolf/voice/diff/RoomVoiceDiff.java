package com.game.werewolf.voice.diff;

import com.game.werewolf.common.model.GamePhase;
import com.game.werewolf.voice.domain.VoiceChangeReason;

import java.util.List;

public record RoomVoiceDiff(String roomId,
                            long prevRevision,
                            long nextRevision,
                            VoiceChangeReason reason,
                            ValueChange<GamePhase> phaseChanged,
                            List<ChannelStateDiff> changedChannels,
                            List<PlayerVoiceDiff> changedPlayers) {
}
