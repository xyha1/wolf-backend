package com.game.werewolf.room.domain;

import com.game.werewolf.common.model.RoleType;
import com.game.werewolf.common.model.TeamType;

public class PlayerState {
    private final String playerId;
    private final int seatNo;
    private final RoleType role;
    private final TeamType team;
    private boolean alive;
    private boolean online;

    public PlayerState(String playerId, int seatNo, RoleType role, TeamType team) {
        this.playerId = playerId;
        this.seatNo = seatNo;
        this.role = role;
        this.team = team;
        this.alive = true;
        this.online = true;
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getSeatNo() {
        return seatNo;
    }

    public RoleType getRole() {
        return role;
    }

    public TeamType getTeam() {
        return team;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
