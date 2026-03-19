package com.game.werewolf.voice.task;

import com.game.werewolf.rtcbridge.model.ProducerRegistryRow;
import com.game.werewolf.voice.diff.PlayerVoiceDiff;
import com.game.werewolf.voice.diff.RoomVoiceDiff;
import com.game.werewolf.voice.domain.ChannelId;
import com.game.werewolf.voice.domain.PublishPolicy;
import com.game.werewolf.voice.domain.RoomVoicePermissionTable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ExecutionTaskCompiler {

    private final TargetProducerResolver targetProducerResolver = new TargetProducerResolver();

    public List<VoiceExecutionTask> compile(RoomVoiceDiff diff,
                                            RoomVoicePermissionTable table,
                                            List<ProducerRegistryRow> producerRegistry) {
        List<VoiceExecutionTask> tasks = new ArrayList<>();
        AtomicInteger order = new AtomicInteger(1);
        for (PlayerVoiceDiff playerDiff : diff.changedPlayers()) {
            compileOnePlayer(diff.roomId(), diff.nextRevision(), playerDiff, table, producerRegistry, order, tasks);
        }
        tasks.sort(Comparator.comparingInt(VoiceExecutionTask::order));
        return tasks;
    }

    private void compileOnePlayer(String roomId,
                                  long revision,
                                  PlayerVoiceDiff playerDiff,
                                  RoomVoicePermissionTable table,
                                  List<ProducerRegistryRow> producerRegistry,
                                  AtomicInteger order,
                                  List<VoiceExecutionTask> tasks) {
        String playerId = playerDiff.playerId();

        for (ChannelId channelId : playerDiff.removedObserveChannels()) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.SYNC_OBSERVE_STREAM, channelId, null, false, null, order));
        }

        for (ChannelId channelId : playerDiff.removedHearChannels()) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.REMOVE_CONSUMER, channelId, null, null, null, order));
        }

        for (ChannelId channelId : playerDiff.removedSpeakChannels()) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.UPDATE_PUBLISH_ACL, channelId, null, false, null, order));
        }

        PublishPolicy fromPolicy = playerDiff.publishPolicyChanged() == null ? null : playerDiff.publishPolicyChanged().from();
        PublishPolicy toPolicy = playerDiff.publishPolicyChanged() == null ? null : playerDiff.publishPolicyChanged().to();
        if (fromPolicy != null && !fromPolicy.producerPaused() && toPolicy != null && toPolicy.producerPaused()) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.PAUSE_PRODUCER, null, null, null, null, order));
        }
        if (fromPolicy != null && !fromPolicy.producerRequired() && toPolicy != null && toPolicy.producerRequired()) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.ENSURE_PRODUCER_EXISTS, null, null, null, null, order));
        }

        for (ChannelId channelId : playerDiff.addedHearChannels()) {
            for (String producerId : targetProducerResolver.resolveTargetProducerIds(playerId, channelId, table, producerRegistry)) {
                tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.ADD_CONSUMER, channelId, producerId, null, null, order));
            }
        }

        Integer delayedMs = table.context().spectatorDelayMs();
        for (ChannelId channelId : playerDiff.addedObserveChannels()) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.SYNC_OBSERVE_STREAM, channelId, null, true, delayedMs, order));
        }

        for (ChannelId channelId : playerDiff.addedSpeakChannels()) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.UPDATE_PUBLISH_ACL, channelId, null, true, null, order));
        }

        if (fromPolicy != null && fromPolicy.producerPaused() && toPolicy != null && !toPolicy.producerPaused()) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.RESUME_PRODUCER, null, null, null, null, order));
        }
        if (fromPolicy != null && fromPolicy.producerRequired() && toPolicy != null && !toPolicy.producerRequired()) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.RELEASE_PRODUCER, null, null, null, null, order));
        }

        if (playerDiff.consumePolicyChanged() != null) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.UPDATE_CONSUME_MODE, null, null, null, null, order));
        }

        if (playerDiff.uiHintsChanged() != null) {
            tasks.add(task(roomId, revision, playerId, VoiceExecutionAction.UPDATE_UI_PERMISSION, null, null, null, null, order));
        }
    }

    private VoiceExecutionTask task(String roomId,
                                    long revision,
                                    String playerId,
                                    VoiceExecutionAction action,
                                    ChannelId channelId,
                                    String producerId,
                                    Boolean allow,
                                    Integer delayedMs,
                                    AtomicInteger order) {
        return new VoiceExecutionTask(
            UUID.randomUUID().toString(),
            roomId,
            revision,
            playerId,
            action,
            channelId,
            producerId,
            allow,
            delayedMs,
            order.getAndIncrement()
        );
    }
}
