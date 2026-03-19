package com.game.werewolf.rtcbridge.model;

public record TaskExecutionResult(String taskId, boolean success, String errorCode, String errorMessage) {
}
