package com.game.werewolf.voice.diff;

import com.game.werewolf.voice.domain.*;

import java.util.List;

public record PlayerVoiceDiff(String playerId,
                              List<ChannelId> addedSpeakChannels,
                              List<ChannelId> removedSpeakChannels,
                              List<ChannelId> addedHearChannels,
                              List<ChannelId> removedHearChannels,
                              List<ChannelId> addedObserveChannels,
                              List<ChannelId> removedObserveChannels,
                              ValueChange<PublishPolicy> publishPolicyChanged,
                              ValueChange<ConsumePolicy> consumePolicyChanged,
                              ValueChange<UIHints> uiHintsChanged) {

    public boolean hasAnyChange() {
        return !addedSpeakChannels.isEmpty()
            || !removedSpeakChannels.isEmpty()
            || !addedHearChannels.isEmpty()
            || !removedHearChannels.isEmpty()
            || !addedObserveChannels.isEmpty()
            || !removedObserveChannels.isEmpty()
            || publishPolicyChanged != null
            || consumePolicyChanged != null
            || uiHintsChanged != null;
    }
}
