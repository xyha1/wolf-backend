package com.game.werewolf.voice.repository;

import com.game.werewolf.voice.diff.RoomVoiceDiff;
import com.game.werewolf.voice.domain.RoomVoicePermissionTable;

import java.util.Optional;

public interface VoicePermissionRepository {

    Optional<RoomVoicePermissionTable> findTable(String roomId);

    Optional<RoomVoiceDiff> findLastDiff(String roomId);

    void save(String roomId, RoomVoicePermissionTable table, RoomVoiceDiff diff);
}
