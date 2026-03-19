package com.game.werewolf.voice.domain;

import com.game.werewolf.room.domain.GameRoomState;
import com.game.werewolf.rtcbridge.model.ProducerRegistryRow;

import java.util.List;

public record VoicePermissionInput(GameRoomState roomState,
                                   List<ProducerRegistryRow> producerRegistry,
                                   RoomVoicePermissionTable previousTable) {
}
