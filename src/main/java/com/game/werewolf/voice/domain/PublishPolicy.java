package com.game.werewolf.voice.domain;

public record PublishPolicy(boolean micCaptureEnabled, boolean producerRequired, boolean producerPaused) {
}
