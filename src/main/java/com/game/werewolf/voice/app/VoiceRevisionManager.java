package com.game.werewolf.voice.app;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class VoiceRevisionManager {

    private final ConcurrentMap<String, Long> planned = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> committed = new ConcurrentHashMap<>();

    public long nextPlannedRevision(String roomId) {
        long next = committed.getOrDefault(roomId, 0L) + 1;
        planned.put(roomId, next);
        return next;
    }

    public void commit(String roomId, long revision) {
        planned.remove(roomId);
        committed.put(roomId, revision);
    }

    public void rollbackPlanned(String roomId) {
        planned.remove(roomId);
    }

    public long committedRevision(String roomId) {
        return committed.getOrDefault(roomId, 0L);
    }
}
