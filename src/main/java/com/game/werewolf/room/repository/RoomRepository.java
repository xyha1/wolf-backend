package com.game.werewolf.room.repository;

import com.game.werewolf.room.domain.GameRoomState;

import java.util.Optional;

public interface RoomRepository {
    GameRoomState getOrCreate(String roomId);

    Optional<GameRoomState> find(String roomId);

    void save(GameRoomState state);
}
