package com.game.werewolf.room.app;

import com.game.werewolf.common.model.RoleType;
import com.game.werewolf.common.model.SpectatorMode;
import com.game.werewolf.common.model.TeamType;
import com.game.werewolf.dispatcher.RoomCommandDispatcher;
import com.game.werewolf.room.domain.GameRoomState;
import com.game.werewolf.room.domain.PlayerState;
import com.game.werewolf.room.domain.SpectatorState;
import com.game.werewolf.room.repository.RoomRepository;
import com.game.werewolf.rtcbridge.api.RtcOrchestratorClient;
import com.game.werewolf.rtcbridge.model.ProducerRegistryRow;
import com.game.werewolf.voice.domain.ChannelId;
import com.game.werewolf.voice.domain.VoiceChangeReason;
import com.game.werewolf.voice.app.VoicePermissionAppService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class RoomAppService {

    private final RoomRepository roomRepository;
    private final RoomCommandDispatcher dispatcher;
    private final VoicePermissionAppService voicePermissionAppService;
    private final RtcOrchestratorClient rtcOrchestratorClient;

    public RoomAppService(RoomRepository roomRepository,
                          RoomCommandDispatcher dispatcher,
                          VoicePermissionAppService voicePermissionAppService,
                          RtcOrchestratorClient rtcOrchestratorClient) {
        this.roomRepository = roomRepository;
        this.dispatcher = dispatcher;
        this.voicePermissionAppService = voicePermissionAppService;
        this.rtcOrchestratorClient = rtcOrchestratorClient;
    }

    public GameRoomState joinRoom(String roomId, String playerId, int seatNo, RoleType role, TeamType team) {
        return await(dispatcher.dispatch(roomId, () -> {
            GameRoomState state = roomRepository.getOrCreate(roomId);
            PlayerState player = new PlayerState(playerId, seatNo, role, team);
            state.upsertPlayer(player);
            rtcOrchestratorClient.upsertProducer(roomId,
                new ProducerRegistryRow(playerId, "producer-" + playerId, true, true, true, ProducerRegistryRow.SourceType.PLAYER, List.of(ChannelId.DAY_PUBLIC, ChannelId.WOLF_NIGHT)));
            rtcOrchestratorClient.updatePlayerState(roomId, playerId, true);
            roomRepository.save(state);
            voicePermissionAppService.recomputeAndApply(state, VoiceChangeReason.ROOM_INIT);
            return state;
        }));
    }

    public GameRoomState leaveRoom(String roomId, String playerId) {
        return await(dispatcher.dispatch(roomId, () -> {
            GameRoomState state = roomRepository.getOrCreate(roomId);
            state.removePlayer(playerId);
            rtcOrchestratorClient.updatePlayerState(roomId, playerId, false);
            roomRepository.save(state);
            voicePermissionAppService.recomputeAndApply(state, VoiceChangeReason.DISCONNECT);
            return state;
        }));
    }

    public GameRoomState disconnectPlayer(String roomId, String playerId) {
        return await(dispatcher.dispatch(roomId, () -> {
            GameRoomState state = roomRepository.getOrCreate(roomId);
            state.findPlayer(playerId).ifPresent(player -> player.setOnline(false));
            rtcOrchestratorClient.updatePlayerState(roomId, playerId, false);
            roomRepository.save(state);
            voicePermissionAppService.recomputeAndApply(state, VoiceChangeReason.DISCONNECT);
            return state;
        }));
    }

    public GameRoomState reconnectPlayer(String roomId, String playerId) {
        return await(dispatcher.dispatch(roomId, () -> {
            GameRoomState state = roomRepository.getOrCreate(roomId);
            state.findPlayer(playerId).ifPresent(player -> player.setOnline(true));
            rtcOrchestratorClient.updatePlayerState(roomId, playerId, true);
            roomRepository.save(state);
            voicePermissionAppService.recomputeAndApply(state, VoiceChangeReason.RECONNECT);
            return state;
        }));
    }

    public GameRoomState addSpectator(String roomId, String spectatorId) {
        return await(dispatcher.dispatch(roomId, () -> {
            GameRoomState state = roomRepository.getOrCreate(roomId);
            state.upsertSpectator(new SpectatorState(spectatorId, true));
            roomRepository.save(state);
            voicePermissionAppService.recomputeAndApply(state, VoiceChangeReason.SPECTATOR_MODE_CHANGE);
            return state;
        }));
    }

    public GameRoomState updateSpectatorConfig(String roomId, SpectatorMode spectatorMode, int delayMs) {
        return await(dispatcher.dispatch(roomId, () -> {
            GameRoomState state = roomRepository.getOrCreate(roomId);
            state.setSpectatorMode(spectatorMode);
            state.setSpectatorDelayMs(delayMs);
            roomRepository.save(state);
            voicePermissionAppService.recomputeAndApply(state, VoiceChangeReason.SPECTATOR_MODE_CHANGE);
            return state;
        }));
    }

    private <T> T await(java.util.concurrent.CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting room command", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Room command execution failed", e);
        }
    }
}
