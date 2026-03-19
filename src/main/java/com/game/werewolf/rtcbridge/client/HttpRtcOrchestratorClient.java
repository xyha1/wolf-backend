package com.game.werewolf.rtcbridge.client;

import com.game.werewolf.rtcbridge.api.RtcOrchestratorClient;
import com.game.werewolf.rtcbridge.model.ApplyExecutionTasksResponse;
import com.game.werewolf.rtcbridge.model.ProducerRegistryRow;
import com.game.werewolf.voice.domain.ChannelId;
import com.game.werewolf.voice.task.VoiceExecutionAction;
import com.game.werewolf.voice.task.VoiceExecutionTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "rtc.bridge.mode", havingValue = "http")
public class HttpRtcOrchestratorClient implements RtcOrchestratorClient {

    private static final ParameterizedTypeReference<RoomStateResponse> ROOM_STATE_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public HttpRtcOrchestratorClient(RestClient.Builder restClientBuilder,
                                     @Value("${rtc.bridge.base-url:http://127.0.0.1:3100}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public ApplyExecutionTasksResponse applyExecutionTasks(String roomId, long plannedRevision, List<VoiceExecutionTask> tasks) {
        ApplyTasksHttpResponse response = restClient.post()
            .uri("/rooms/{roomId}/apply-execution-tasks", encode(roomId))
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "plannedRevision", plannedRevision,
                "tasks", tasks.stream().map(this::toPayload).toList()
            ))
            .retrieve()
            .body(ApplyTasksHttpResponse.class);

        if (response == null) {
            throw new IllegalStateException("RTC orchestrator returned empty response for apply-execution-tasks");
        }

        String errorMessage = response.allSucceeded() ? null : response.results().stream()
            .filter(result -> !result.success())
            .map(result -> "%s:%s".formatted(result.taskId(), result.errorMessage()))
            .findFirst()
            .orElse("RTC orchestrator apply-execution-tasks failed");

        return new ApplyExecutionTasksResponse(
            response.allSucceeded(),
            response.results().stream()
                .map(result -> new com.game.werewolf.rtcbridge.model.TaskExecutionResult(
                    result.taskId(),
                    result.success(),
                    result.errorCode(),
                    result.errorMessage()
                ))
                .toList(),
            response.allSucceeded() ? null : "RTC_TASK_FAILED",
            errorMessage
        );
    }

    @Override
    public List<ProducerRegistryRow> queryProducerRegistry(String roomId) {
        RoomStateResponse response = restClient.get()
            .uri("/rooms/{roomId}/state", encode(roomId))
            .retrieve()
            .body(ROOM_STATE_TYPE);

        if (response == null || response.producerRegistry() == null) {
            return List.of();
        }

        return response.producerRegistry().stream()
            .map(row -> new ProducerRegistryRow(
                row.playerId(),
                row.producerId(),
                row.online(),
                row.alive(),
                row.paused(),
                ProducerRegistryRow.SourceType.valueOf(row.sourceType().toUpperCase()),
                row.publishAclChannels().stream().map(this::toChannelId).toList()
            ))
            .toList();
    }

    @Override
    public void upsertProducer(String roomId, ProducerRegistryRow row) {
        // Real producer lifecycle belongs to Node.js mediasoup. This method is retained for the in-memory bridge only.
    }

    @Override
    public void updatePlayerState(String roomId, String playerId, boolean online) {
        restClient.post()
            .uri("/rooms/{roomId}/players/state", encode(roomId))
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("playerId", playerId, "online", online))
            .retrieve()
            .toBodilessEntity();
    }

    private String encode(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    private TaskPayload toPayload(VoiceExecutionTask task) {
        return new TaskPayload(
            task.taskId(),
            task.roomId(),
            task.revision(),
            task.playerId(),
            toActionValue(task.action()),
            task.channelId() == null ? null : toChannelValue(task.channelId()),
            task.targetProducerId(),
            task.allow(),
            task.delayedMs(),
            task.order()
        );
    }

    private ChannelId toChannelId(String value) {
        return ChannelId.valueOf(value.trim().toUpperCase());
    }

    private String toChannelValue(ChannelId channelId) {
        return channelId.name().toLowerCase();
    }

    private String toActionValue(VoiceExecutionAction action) {
        return action.name().toLowerCase();
    }

    private record ApplyTasksHttpResponse(String roomId,
                                          long plannedRevision,
                                          long committedRevision,
                                          boolean allSucceeded,
                                          List<TaskResultPayload> results) {
    }

    private record TaskResultPayload(String taskId,
                                     String action,
                                     String playerId,
                                     boolean success,
                                     String errorCode,
                                     String errorMessage) {
    }

    private record TaskPayload(String taskId,
                               String roomId,
                               long revision,
                               String playerId,
                               String action,
                               String channelId,
                               String targetProducerId,
                               Boolean allow,
                               Integer delayedMs,
                               int order) {
    }

    private record RoomStateResponse(String roomId,
                                     long plannedRevision,
                                     long committedRevision,
                                     List<ProducerRegistryPayload> producerRegistry) {
    }

    private record ProducerRegistryPayload(String playerId,
                                           String producerId,
                                           boolean online,
                                           boolean alive,
                                           boolean paused,
                                           String sourceType,
                                           List<String> publishAclChannels) {
    }
}
