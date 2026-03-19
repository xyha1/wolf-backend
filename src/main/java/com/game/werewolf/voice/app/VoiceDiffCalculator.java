package com.game.werewolf.voice.app;

import com.game.werewolf.voice.diff.*;
import com.game.werewolf.voice.domain.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class VoiceDiffCalculator {

    public RoomVoiceDiff calculate(RoomVoicePermissionTable previous,
                                   RoomVoicePermissionTable next,
                                   VoiceChangeReason reason) {
        List<ChannelStateDiff> channelDiffs = channelDiffs(previous, next);
        List<PlayerVoiceDiff> playerDiffs = playerDiffs(previous, next);

        return new RoomVoiceDiff(
            next.context().roomId(),
            previous == null ? 0 : previous.context().revision(),
            next.context().revision(),
            reason,
            previous == null ? null : phaseDiff(previous.context(), next.context()),
            channelDiffs,
            playerDiffs
        );
    }

    private ValueChange<com.game.werewolf.common.model.GamePhase> phaseDiff(RoomVoiceContext previous, RoomVoiceContext next) {
        if (previous.phase() == next.phase()) {
            return null;
        }
        return new ValueChange<>(previous.phase(), next.phase());
    }

    private List<ChannelStateDiff> channelDiffs(RoomVoicePermissionTable previous, RoomVoicePermissionTable next) {
        if (previous == null) {
            return List.of();
        }
        List<ChannelStateDiff> result = new ArrayList<>();
        for (ChannelId channelId : ChannelId.values()) {
            ChannelDefinition oldDef = previous.channels().get(channelId);
            ChannelDefinition newDef = next.channels().get(channelId);
            ValueChange<Boolean> active = oldDef.active() == newDef.active() ? null : new ValueChange<>(oldDef.active(), newDef.active());
            ValueChange<Integer> delay = oldDef.delayedMs() == newDef.delayedMs() ? null : new ValueChange<>(oldDef.delayedMs(), newDef.delayedMs());
            if (active != null || delay != null) {
                result.add(new ChannelStateDiff(channelId, active, delay));
            }
        }
        return result;
    }

    private List<PlayerVoiceDiff> playerDiffs(RoomVoicePermissionTable previous, RoomVoicePermissionTable next) {
        Map<String, PlayerPermissionRow> oldRows = previous == null ? Map.of() : previous.rows();
        Map<String, PlayerPermissionRow> newRows = next.rows();
        Set<String> playerIds = new LinkedHashSet<>();
        playerIds.addAll(oldRows.keySet());
        playerIds.addAll(newRows.keySet());

        List<PlayerVoiceDiff> diffs = new ArrayList<>();
        for (String playerId : playerIds) {
            PlayerPermissionRow oldRow = oldRows.get(playerId);
            PlayerPermissionRow newRow = newRows.get(playerId);
            if (newRow == null) {
                continue;
            }
            PlayerVoiceDiff diff = diffPlayer(playerId, oldRow, newRow);
            if (diff.hasAnyChange()) {
                diffs.add(diff);
            }
        }
        return diffs;
    }

    private PlayerVoiceDiff diffPlayer(String playerId, PlayerPermissionRow oldRow, PlayerPermissionRow newRow) {
        List<ChannelId> oldSpeak = oldRow == null ? List.of() : oldRow.speakChannels();
        List<ChannelId> oldHear = oldRow == null ? List.of() : oldRow.hearChannels();
        List<ChannelId> oldObserve = oldRow == null ? List.of() : oldRow.observeChannels();

        List<ChannelId> newSpeak = newRow.speakChannels();
        List<ChannelId> newHear = newRow.hearChannels();
        List<ChannelId> newObserve = newRow.observeChannels();

        ValueChange<PublishPolicy> publishChange = oldRow == null || oldRow.publishPolicy().equals(newRow.publishPolicy())
            ? null
            : new ValueChange<>(oldRow.publishPolicy(), newRow.publishPolicy());
        ValueChange<ConsumePolicy> consumeChange = oldRow == null || oldRow.consumePolicy().equals(newRow.consumePolicy())
            ? null
            : new ValueChange<>(oldRow.consumePolicy(), newRow.consumePolicy());
        ValueChange<UIHints> uiChange = oldRow == null || oldRow.uiHints().equals(newRow.uiHints())
            ? null
            : new ValueChange<>(oldRow.uiHints(), newRow.uiHints());

        return new PlayerVoiceDiff(
            playerId,
            added(oldSpeak, newSpeak),
            removed(oldSpeak, newSpeak),
            added(oldHear, newHear),
            removed(oldHear, newHear),
            added(oldObserve, newObserve),
            removed(oldObserve, newObserve),
            publishChange,
            consumeChange,
            uiChange
        );
    }

    private List<ChannelId> added(List<ChannelId> oldList, List<ChannelId> newList) {
        return newList.stream().filter(id -> !oldList.contains(id)).toList();
    }

    private List<ChannelId> removed(List<ChannelId> oldList, List<ChannelId> newList) {
        return oldList.stream().filter(id -> !newList.contains(id)).toList();
    }
}
