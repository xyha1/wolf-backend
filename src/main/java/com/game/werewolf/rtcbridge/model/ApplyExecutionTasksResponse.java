package com.game.werewolf.rtcbridge.model;

import java.util.List;

public record ApplyExecutionTasksResponse(boolean success,
                                          List<TaskExecutionResult> results,
                                          String errorCode,
                                          String errorMessage) {
}
