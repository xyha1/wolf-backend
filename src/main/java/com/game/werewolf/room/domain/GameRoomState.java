package com.game.werewolf.room.domain;

import com.game.werewolf.common.model.GamePhase;
import com.game.werewolf.common.model.SpectatorMode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class GameRoomState {
    private final String roomId;
    private GamePhase phase;
    private int round;
    private String currentSpeakerId;
    private boolean wolfChatOpen;
    private SpectatorMode spectatorMode;
    private int spectatorDelayMs;
    private long committedVoiceRevision;
    private final Map<String, PlayerState> players;
    private final Map<String, SpectatorState> spectators;

    public GameRoomState(String roomId) {
        this.roomId = roomId;
        this.phase = GamePhase.WAITING;
        this.round = 0;
        this.wolfChatOpen = false;
        this.spectatorMode = SpectatorMode.OFF;
        this.spectatorDelayMs = 30_000;
        this.committedVoiceRevision = 0;
        this.players = new LinkedHashMap<>();
        this.spectators = new LinkedHashMap<>();
    }

    public String getRoomId() {
        return roomId;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getCurrentSpeakerId() {
        return currentSpeakerId;
    }

    public void setCurrentSpeakerId(String currentSpeakerId) {
        this.currentSpeakerId = currentSpeakerId;
    }

    public boolean isWolfChatOpen() {
        return wolfChatOpen;
    }

    public void setWolfChatOpen(boolean wolfChatOpen) {
        this.wolfChatOpen = wolfChatOpen;
    }

    public SpectatorMode getSpectatorMode() {
        return spectatorMode;
    }

    public void setSpectatorMode(SpectatorMode spectatorMode) {
        this.spectatorMode = spectatorMode;
    }

    public int getSpectatorDelayMs() {
        return spectatorDelayMs;
    }

    public void setSpectatorDelayMs(int spectatorDelayMs) {
        this.spectatorDelayMs = spectatorDelayMs;
    }

    public long getCommittedVoiceRevision() {
        return committedVoiceRevision;
    }

    public void setCommittedVoiceRevision(long committedVoiceRevision) {
        this.committedVoiceRevision = committedVoiceRevision;
    }

    public Map<String, PlayerState> getPlayers() {
        return players;
    }

    public Map<String, SpectatorState> getSpectators() {
        return spectators;
    }

    public void upsertPlayer(PlayerState player) {
        players.put(player.getPlayerId(), player);
    }

    public Optional<PlayerState> findPlayer(String playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
        if (playerId.equals(currentSpeakerId)) {
            currentSpeakerId = null;
        }
    }

    public void upsertSpectator(SpectatorState spectator) {
        spectators.put(spectator.getSpectatorId(), spectator);
    }
}
