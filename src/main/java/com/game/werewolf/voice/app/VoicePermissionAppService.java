package com.game.werewolf.voice.app;

import com.game.werewolf.room.domain.GameRoomState;
import com.game.werewolf.rtcbridge.api.RtcOrchestratorClient;
import com.game.werewolf.rtcbridge.model.ApplyExecutionTasksResponse;
import com.game.werewolf.voice.diff.RoomVoiceDiff;
import com.game.werewolf.voice.domain.RoomVoicePermissionTable;
import com.game.werewolf.voice.domain.VoiceChangeReason;
import com.game.werewolf.voice.repository.VoicePermissionRepository;
import com.game.werewolf.voice.task.ExecutionTaskCompiler;
import com.game.werewolf.voice.task.VoiceExecutionTask;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoicePermissionAppService {

    private final PermissionTableBuilder tableBuilder;
    private final VoiceDiffCalculator diffCalculator;
    private final ExecutionTaskCompiler taskCompiler;
    private final VoicePermissionRepository permissionRepository;
    private final VoiceRevisionManager revisionManager;
    private final RtcOrchestratorClient rtcOrchestratorClient;

    public VoicePermissionAppService(PermissionTableBuilder tableBuilder,
                                     VoiceDiffCalculator diffCalculator,
                                     ExecutionTaskCompiler taskCompiler,
                                     VoicePermissionRepository permissionRepository,
                                     VoiceRevisionManager revisionManager,
                                     RtcOrchestratorClient rtcOrchestratorClient) {
        this.tableBuilder = tableBuilder;
        this.diffCalculator = diffCalculator;
        this.taskCompiler = taskCompiler;
        this.permissionRepository = permissionRepository;
        this.revisionManager = revisionManager;
        this.rtcOrchestratorClient = rtcOrchestratorClient;
    }

    public synchronized RoomVoicePermissionTable recomputeAndApply(GameRoomState roomState, VoiceChangeReason reason) {
        String roomId = roomState.getRoomId();
        long nextRevision = revisionManager.nextPlannedRevision(roomId);

        RoomVoicePermissionTable previous = permissionRepository.findTable(roomId).orElse(null);
        RoomVoicePermissionTable next = tableBuilder.build(roomState, nextRevision, reason);

        RoomVoiceDiff diff = diffCalculator.calculate(previous, next, reason);
        List<VoiceExecutionTask> tasks = taskCompiler.compile(diff, next, rtcOrchestratorClient.queryProducerRegistry(roomId));

        ApplyExecutionTasksResponse applyResult = rtcOrchestratorClient.applyExecutionTasks(roomId, nextRevision, tasks);
        if (!applyResult.success()) {
            revisionManager.rollbackPlanned(roomId);
            throw new IllegalStateException("Failed to apply execution tasks: " + applyResult.errorMessage());
        }

        revisionManager.commit(roomId, nextRevision);
        roomState.setCommittedVoiceRevision(nextRevision);
        permissionRepository.save(roomId, next, diff);
        return next;
    }
}
