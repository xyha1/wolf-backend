package com.game.werewolf.voice.domain;

public record UIHints(boolean canOpenMicButton, MutedReason showMutedReason, Mode currentMode) {

    public enum MutedReason {
        NOT_TURN,
        DEAD,
        NIGHT_SILENT,
        SPECTATOR,
        OFFLINE,
        PHASE_RESTRICTED
    }

    public enum Mode {
        SPEAK,
        LISTEN,
        OBSERVE,
        SILENT
    }
}
