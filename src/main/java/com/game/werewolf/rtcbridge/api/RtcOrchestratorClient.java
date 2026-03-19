package com.game.werewolf.rtcbridge.api;

import com.game.werewolf.rtcbridge.model.ApplyExecutionTasksResponse;
import com.game.werewolf.rtcbridge.model.ProducerRegistryRow;
import com.game.werewolf.voice.task.VoiceExecutionTask;

import java.util.List;

public interface RtcOrchestratorClient {

    ApplyExecutionTasksResponse applyExecutionTasks(String roomId, long plannedRevision, List<VoiceExecutionTask> tasks);

    List<ProducerRegistryRow> queryProducerRegistry(String roomId);

    void upsertProducer(String roomId, ProducerRegistryRow row);

    void updatePlayerState(String roomId, String playerId, boolean online);
}
