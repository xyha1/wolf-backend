package com.game.werewolf.voice.domain;

import com.game.werewolf.voice.diff.RoomVoiceDiff;

public record VoicePermissionComputationResult(RoomVoicePermissionTable table, RoomVoiceDiff diff) {
}
