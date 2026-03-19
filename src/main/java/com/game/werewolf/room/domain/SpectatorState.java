package com.game.werewolf.room.domain;

public class SpectatorState {
    private final String spectatorId;
    private boolean online;

    public SpectatorState(String spectatorId, boolean online) {
        this.spectatorId = spectatorId;
        this.online = online;
    }

    public String getSpectatorId() {
        return spectatorId;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
