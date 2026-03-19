package com.game.werewolf.rtcbridge.client;

import com.game.werewolf.rtcbridge.api.RtcOrchestratorClient;
import com.game.werewolf.rtcbridge.model.ApplyExecutionTasksResponse;
import com.game.werewolf.rtcbridge.model.ProducerRegistryRow;
import com.game.werewolf.rtcbridge.model.TaskExecutionResult;
import com.game.werewolf.voice.task.VoiceExecutionAction;
import com.game.werewolf.voice.task.VoiceExecutionTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@ConditionalOnProperty(name = "rtc.bridge.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryRtcOrchestratorClient implements RtcOrchestratorClient {

    private final ConcurrentMap<String, Map<String, ProducerRegistryRow>> roomProducers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Map<String, Set<String>>> roomConsumers = new ConcurrentHashMap<>();

    @Override
    public ApplyExecutionTasksResponse applyExecutionTasks(String roomId, long plannedRevision, List<VoiceExecutionTask> tasks) {
        List<TaskExecutionResult> results = new ArrayList<>(tasks.size());
        Map<String, ProducerRegistryRow> producers = roomProducers.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>());
        Map<String, Set<String>> consumers = roomConsumers.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>());

        for (VoiceExecutionTask task : tasks) {
            switch (task.action()) {
                case ENSURE_PRODUCER_EXISTS -> ensureProducerExists(producers, task.playerId());
                case RELEASE_PRODUCER -> producers.remove(task.playerId());
                case PAUSE_PRODUCER -> updateProducerPaused(producers, task.playerId(), true);
                case RESUME_PRODUCER -> updateProducerPaused(producers, task.playerId(), false);
                case UPDATE_PUBLISH_ACL -> updatePublishAcl(producers, task.playerId(), task.channelId(), Boolean.TRUE.equals(task.allow()));
                case ADD_CONSUMER -> {
                    String key = consumerKey(task.channelId(), task.targetProducerId());
                    consumers.computeIfAbsent(task.playerId(), ignored -> ConcurrentHashMap.newKeySet()).add(key);
                }
                case REMOVE_CONSUMER -> consumers.computeIfAbsent(task.playerId(), ignored -> ConcurrentHashMap.newKeySet())
                    .removeIf(v -> v.startsWith(task.channelId() + ":"));
                case UPDATE_CONSUME_MODE, SYNC_OBSERVE_STREAM, UPDATE_UI_PERMISSION -> {
                    // no-op in in-memory simulator
                }
            }
            results.add(new TaskExecutionResult(task.taskId(), true, null, null));
        }
        return new ApplyExecutionTasksResponse(true, results, null, null);
    }

    @Override
    public List<ProducerRegistryRow> queryProducerRegistry(String roomId) {
        return new ArrayList<>(roomProducers.getOrDefault(roomId, Map.of()).values());
    }

    @Override
    public void upsertProducer(String roomId, ProducerRegistryRow row) {
        roomProducers.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>()).put(row.playerId(), row);
    }

    @Override
    public void updatePlayerState(String roomId, String playerId, boolean online) {
        Map<String, ProducerRegistryRow> producers = roomProducers.get(roomId);
        if (producers == null) {
            return;
        }
        ProducerRegistryRow old = producers.get(playerId);
        if (old == null) {
            return;
        }
        producers.put(playerId, new ProducerRegistryRow(
            old.playerId(),
            old.producerId(),
            online,
            old.alive(),
            old.paused(),
            old.sourceType(),
            old.publishAclChannels()
        ));
    }

    private void ensureProducerExists(Map<String, ProducerRegistryRow> producers, String playerId) {
        producers.computeIfAbsent(playerId, id -> new ProducerRegistryRow(
            id,
            "producer-" + id,
            true,
            true,
            true,
            ProducerRegistryRow.SourceType.PLAYER,
            new ArrayList<>()
        ));
    }

    private void updateProducerPaused(Map<String, ProducerRegistryRow> producers, String playerId, boolean paused) {
        ProducerRegistryRow old = producers.get(playerId);
        if (old == null) {
            return;
        }
        producers.put(playerId, new ProducerRegistryRow(
            old.playerId(),
            old.producerId(),
            old.online(),
            old.alive(),
            paused,
            old.sourceType(),
            old.publishAclChannels()
        ));
    }

    private void updatePublishAcl(Map<String, ProducerRegistryRow> producers, String playerId, com.game.werewolf.voice.domain.ChannelId channelId, boolean allow) {
        ProducerRegistryRow old = producers.get(playerId);
        if (old == null || channelId == null) {
            return;
        }
        Set<com.game.werewolf.voice.domain.ChannelId> channels = new LinkedHashSet<>(old.publishAclChannels());
        if (allow) {
            channels.add(channelId);
        } else {
            channels.remove(channelId);
        }
        producers.put(playerId, new ProducerRegistryRow(
            old.playerId(),
            old.producerId(),
            old.online(),
            old.alive(),
            old.paused(),
            old.sourceType(),
            new ArrayList<>(channels)
        ));
    }

    private String consumerKey(com.game.werewolf.voice.domain.ChannelId channelId, String producerId) {
        return channelId + ":" + producerId;
    }
}
