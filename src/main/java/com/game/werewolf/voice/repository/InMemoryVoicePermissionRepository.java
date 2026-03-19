package com.game.werewolf.voice.repository;

import com.game.werewolf.voice.diff.RoomVoiceDiff;
import com.game.werewolf.voice.domain.RoomVoicePermissionTable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryVoicePermissionRepository implements VoicePermissionRepository {

    private final ConcurrentMap<String, RoomVoicePermissionTable> tables = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RoomVoiceDiff> lastDiffs = new ConcurrentHashMap<>();

    @Override
    public Optional<RoomVoicePermissionTable> findTable(String roomId) {
        return Optional.ofNullable(tables.get(roomId));
    }

    @Override
    public Optional<RoomVoiceDiff> findLastDiff(String roomId) {
        return Optional.ofNullable(lastDiffs.get(roomId));
    }

    @Override
    public void save(String roomId, RoomVoicePermissionTable table, RoomVoiceDiff diff) {
        tables.put(roomId, table);
        if (diff != null) {
            lastDiffs.put(roomId, diff);
        }
    }
}
