package com.game.werewolf.voice;

import com.game.werewolf.common.model.GamePhase;
import com.game.werewolf.common.model.RoleType;
import com.game.werewolf.common.model.TeamType;
import com.game.werewolf.room.domain.GameRoomState;
import com.game.werewolf.room.domain.PlayerState;
import com.game.werewolf.rtcbridge.model.ProducerRegistryRow;
import com.game.werewolf.voice.app.PermissionTableBuilder;
import com.game.werewolf.voice.app.VoiceDiffCalculator;
import com.game.werewolf.voice.diff.RoomVoiceDiff;
import com.game.werewolf.voice.domain.ChannelId;
import com.game.werewolf.voice.domain.VoiceChangeReason;
import com.game.werewolf.voice.task.ExecutionTaskCompiler;
import com.game.werewolf.voice.task.VoiceExecutionAction;
import com.game.werewolf.voice.task.VoiceExecutionTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionTaskCompilerTest {

    @Test
    void shouldCompileRevokeBeforeGrantForSpeakerSwitch() {
        PermissionTableBuilder builder = new PermissionTableBuilder();
        VoiceDiffCalculator diffCalculator = new VoiceDiffCalculator();
        ExecutionTaskCompiler compiler = new ExecutionTaskCompiler();

        GameRoomState roomState = new GameRoomState("room-1");
        roomState.setPhase(GamePhase.DAY);
        roomState.upsertPlayer(new PlayerState("a", 1, RoleType.VILLAGER, TeamType.GOOD));
        roomState.upsertPlayer(new PlayerState("b", 2, RoleType.VILLAGER, TeamType.GOOD));

        roomState.setCurrentSpeakerId("a");
        var oldTable = builder.build(roomState, 1, VoiceChangeReason.SPEAKER_CHANGE);

        roomState.setCurrentSpeakerId("b");
        var newTable = builder.build(roomState, 2, VoiceChangeReason.SPEAKER_CHANGE);

        RoomVoiceDiff diff = diffCalculator.calculate(oldTable, newTable, VoiceChangeReason.SPEAKER_CHANGE);
        List<ProducerRegistryRow> registry = List.of(
            new ProducerRegistryRow("a", "pa", true, true, false, ProducerRegistryRow.SourceType.PLAYER, List.of(ChannelId.DAY_PUBLIC)),
            new ProducerRegistryRow("b", "pb", true, true, false, ProducerRegistryRow.SourceType.PLAYER, List.of(ChannelId.DAY_PUBLIC))
        );
        List<VoiceExecutionTask> tasks = compiler.compile(diff, newTable, registry);

        int revokeIndex = indexOf(tasks, "a", VoiceExecutionAction.UPDATE_PUBLISH_ACL, false);
        int grantIndex = indexOf(tasks, "b", VoiceExecutionAction.UPDATE_PUBLISH_ACL, true);
        assertTrue(revokeIndex >= 0);
        assertTrue(grantIndex >= 0);
        assertTrue(revokeIndex < grantIndex);
    }

    private int indexOf(List<VoiceExecutionTask> tasks, String playerId, VoiceExecutionAction action, boolean allow) {
        for (int i = 0; i < tasks.size(); i++) {
            VoiceExecutionTask task = tasks.get(i);
            if (task.playerId().equals(playerId) && task.action() == action && task.allow() != null && task.allow() == allow) {
                return i;
            }
        }
        return -1;
    }
}
