package com.game.werewolf.gameflow.app;

import com.game.werewolf.common.model.GamePhase;
import com.game.werewolf.dispatcher.RoomCommandDispatcher;
import com.game.werewolf.room.domain.GameRoomState;
import com.game.werewolf.room.repository.RoomRepository;
import com.game.werewolf.voice.app.VoicePermissionAppService;
import com.game.werewolf.voice.domain.VoiceChangeReason;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class GameFlowAppService {

    private final RoomRepository roomRepository;
    private final RoomCommandDispatcher dispatcher;
    private final VoicePermissionAppService voicePermissionAppService;

    public GameFlowAppService(RoomRepository roomRepository,
                              RoomCommandDispatcher dispatcher,
                              VoicePermissionAppService voicePermissionAppService) {
        this.roomRepository = roomRepository;
        this.dispatcher = dispatcher;
        this.voicePermissionAppService = voicePermissionAppService;
    }

    public GameRoomState setPhase(String roomId, GamePhase phase) {
        return await(dispatcher.dispatch(roomId, () -> {
            GameRoomState state = roomRepository.getOrCreate(roomId);
            state.setPhase(phase);
            if (phase == GamePhase.NIGHT) {
                state.setCurrentSpeakerId(null);
            }
            roomRepository.save(state);
            voicePermissionAppService.recomputeAndApply(state, VoiceChangeReason.PHASE_CHANGE);
            return state;
        }));
    }

    public GameRoomState setSpeaker(String roomId, String speakerId) {
        return await(dispatcher.dispatch(roomId, () -> {
            GameRoomState state = roomRepository.getOrCreate(roomId);
            state.setPhase(GamePhase.DAY);
            state.setCurrentSpeakerId(speakerId);
            roomRepository.save(state);
            voicePermissionAppService.recomputeAndApply(state, VoiceChangeReason.SPEAKER_CHANGE);
            return state;
        }));
    }

    public GameRoomState advanceSpeaker(String roomId) {
        return await(dispatcher.dispatch(roomId, () -> {
            GameRoomState state = roomRepository.getOrCreate(roomId);
            List<String> alivePlayers = state.getPlayers().values().stream()
                .filter(p -> p.isAlive())
                .map(p -> p.getPlayerId())
                .toList();
            if (alivePlayers.isEmpty()) {
                return state;
            }
            int current = Math.max(alivePlayers.indexOf(state.getCurrentSpeakerId()), -1);
            String nextSpeaker = alivePlayers.get((current + 1) % alivePlayers.size());
            state.setPhase(GamePhase.DAY);
            state.setCurrentSpeakerId(nextSpeaker);
            roomRepository.save(state);
            voicePermissionAppService.recomputeAndApply(state, VoiceChangeReason.SPEAKER_CHANGE);
            return state;
        }));
    }

    public GameRoomState setWolfChatOpen(String roomId, boolean open) {
        return await(dispatcher.dispatch(roomId, () -> {
            GameRoomState state = roomRepository.getOrCreate(roomId);
            state.setPhase(GamePhase.NIGHT);
            state.setWolfChatOpen(open);
            roomRepository.save(state);
            voicePermissionAppService.recomputeAndApply(state, open ? VoiceChangeReason.WOLF_CHAT_OPEN : VoiceChangeReason.WOLF_CHAT_CLOSE);
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
