package com.game.werewolf.voice.domain;

public record ConsumePolicy(Strategy strategy, Integer delayedMs) {
    public enum Strategy {
        NONE,
        DIRECT,
        DELAYED
    }
}
