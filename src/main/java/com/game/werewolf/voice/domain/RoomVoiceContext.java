package com.game.werewolf.voice.domain;

import com.game.werewolf.common.model.GamePhase;
import com.game.werewolf.common.model.SpectatorMode;

public record RoomVoiceContext(String roomId,
                               long revision,
                               GamePhase phase,
                               int round,
                               String currentSpeakerId,
                               SpectatorMode spectatorMode,
                               int spectatorDelayMs,
                               boolean wolfChatOpen,
                               VoiceChangeReason lastReason) {
}
