package com.game.werewolf.room.repository;

import com.game.werewolf.room.domain.GameRoomState;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryRoomRepository implements RoomRepository {

    private final ConcurrentMap<String, GameRoomState> states = new ConcurrentHashMap<>();

    @Override
    public GameRoomState getOrCreate(String roomId) {
        return states.computeIfAbsent(roomId, GameRoomState::new);
    }

    @Override
    public Optional<GameRoomState> find(String roomId) {
        return Optional.ofNullable(states.get(roomId));
    }

    @Override
    public void save(GameRoomState state) {
        states.put(state.getRoomId(), state);
    }
}
