package com.game.werewolf.voice.app;

import com.game.werewolf.common.model.GamePhase;
import com.game.werewolf.common.model.RoleType;
import com.game.werewolf.common.model.SpectatorMode;
import com.game.werewolf.room.domain.GameRoomState;
import com.game.werewolf.room.domain.PlayerState;
import com.game.werewolf.room.domain.SpectatorState;
import com.game.werewolf.voice.domain.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PermissionTableBuilder {

    public RoomVoicePermissionTable build(GameRoomState roomState, long nextRevision, VoiceChangeReason reason) {
        RoomVoiceContext context = new RoomVoiceContext(
            roomState.getRoomId(),
            nextRevision,
            roomState.getPhase(),
            roomState.getRound(),
            roomState.getCurrentSpeakerId(),
            roomState.getSpectatorMode(),
            roomState.getSpectatorDelayMs(),
            roomState.isWolfChatOpen(),
            reason
        );

        Map<ChannelId, ChannelDefinition> channels = buildChannels(roomState);
        Map<String, PlayerPermissionRow> rows = new LinkedHashMap<>();

        for (PlayerState player : roomState.getPlayers().values()) {
            rows.put(player.getPlayerId(), buildPlayerRow(roomState, player));
        }
        for (SpectatorState spectator : roomState.getSpectators().values()) {
            rows.put(spectator.getSpectatorId(), buildSpectatorRow(roomState, spectator));
        }

        return new RoomVoicePermissionTable(context, channels, rows);
    }

    private Map<ChannelId, ChannelDefinition> buildChannels(GameRoomState roomState) {
        Map<ChannelId, ChannelDefinition> channels = new EnumMap<>(ChannelId.class);
        channels.put(ChannelId.DAY_PUBLIC, new ChannelDefinition(ChannelId.DAY_PUBLIC, ChannelDefinition.Kind.PUBLIC, roomState.getPhase() == GamePhase.DAY, 0, null));
        channels.put(ChannelId.WOLF_NIGHT, new ChannelDefinition(ChannelId.WOLF_NIGHT, ChannelDefinition.Kind.PRIVATE,
            roomState.getPhase() == GamePhase.NIGHT && roomState.isWolfChatOpen(), 0, null));
        channels.put(ChannelId.SPECTATOR_PUBLIC, new ChannelDefinition(ChannelId.SPECTATOR_PUBLIC, ChannelDefinition.Kind.SPECTATOR,
            roomState.getSpectatorMode() != SpectatorMode.OFF, roomState.getSpectatorDelayMs(), ChannelId.DAY_PUBLIC));
        channels.put(ChannelId.SPECTATOR_FULL, new ChannelDefinition(ChannelId.SPECTATOR_FULL, ChannelDefinition.Kind.SPECTATOR,
            roomState.getSpectatorMode() == SpectatorMode.FULL, roomState.getSpectatorDelayMs(), ChannelId.WOLF_NIGHT));
        return channels;
    }

    private PlayerPermissionRow buildPlayerRow(GameRoomState roomState, PlayerState player) {
        if (!player.isOnline()) {
            return offlineRow(player);
        }

        if (roomState.getPhase() == GamePhase.DAY) {
            return dayRow(roomState, player);
        }
        if (roomState.getPhase() == GamePhase.NIGHT) {
            return nightRow(roomState, player);
        }
        return waitingRow(player);
    }

    private PlayerPermissionRow buildSpectatorRow(GameRoomState roomState, SpectatorState spectator) {
        List<ChannelId> observeChannels = roomState.getSpectatorMode() == SpectatorMode.FULL
            ? List.of(ChannelId.SPECTATOR_PUBLIC, ChannelId.SPECTATOR_FULL)
            : (roomState.getSpectatorMode() == SpectatorMode.PUBLIC ? List.of(ChannelId.SPECTATOR_PUBLIC) : List.of());

        return new PlayerPermissionRow(
            spectator.getSpectatorId(),
            null,
            null,
            null,
            true,
            spectator.isOnline(),
            true,
            List.of(),
            List.of(),
            observeChannels,
            new PublishPolicy(false, false, true),
            new ConsumePolicy(ConsumePolicy.Strategy.DELAYED, roomState.getSpectatorDelayMs()),
            new UIHints(false, UIHints.MutedReason.SPECTATOR, UIHints.Mode.OBSERVE)
        );
    }

    private PlayerPermissionRow dayRow(GameRoomState roomState, PlayerState player) {
        boolean isCurrentSpeaker = player.getPlayerId().equals(roomState.getCurrentSpeakerId());
        if (isCurrentSpeaker && player.isAlive()) {
            return new PlayerPermissionRow(
                player.getPlayerId(), player.getSeatNo(), player.getRole(), player.getTeam(), player.isAlive(), true, false,
                List.of(ChannelId.DAY_PUBLIC), List.of(ChannelId.DAY_PUBLIC), List.of(),
                new PublishPolicy(true, true, false),
                new ConsumePolicy(ConsumePolicy.Strategy.DIRECT, null),
                new UIHints(true, null, UIHints.Mode.SPEAK)
            );
        }
        UIHints.MutedReason reason = player.isAlive() ? UIHints.MutedReason.NOT_TURN : UIHints.MutedReason.DEAD;
        return new PlayerPermissionRow(
            player.getPlayerId(), player.getSeatNo(), player.getRole(), player.getTeam(), player.isAlive(), true, false,
            List.of(), List.of(ChannelId.DAY_PUBLIC), List.of(),
            new PublishPolicy(true, true, true),
            new ConsumePolicy(ConsumePolicy.Strategy.DIRECT, null),
            new UIHints(false, reason, UIHints.Mode.LISTEN)
        );
    }

    private PlayerPermissionRow nightRow(GameRoomState roomState, PlayerState player) {
        if (!player.isAlive()) {
            return new PlayerPermissionRow(
                player.getPlayerId(), player.getSeatNo(), player.getRole(), player.getTeam(), false, true, false,
                List.of(), List.of(), List.of(),
                new PublishPolicy(true, true, true),
                new ConsumePolicy(ConsumePolicy.Strategy.NONE, null),
                new UIHints(false, UIHints.MutedReason.DEAD, UIHints.Mode.SILENT)
            );
        }

        boolean wolfCanTalk = player.getRole() == RoleType.WOLF && roomState.isWolfChatOpen();
        if (wolfCanTalk) {
            return new PlayerPermissionRow(
                player.getPlayerId(), player.getSeatNo(), player.getRole(), player.getTeam(), true, true, false,
                List.of(ChannelId.WOLF_NIGHT), List.of(ChannelId.WOLF_NIGHT), List.of(),
                new PublishPolicy(true, true, false),
                new ConsumePolicy(ConsumePolicy.Strategy.DIRECT, null),
                new UIHints(true, null, UIHints.Mode.SPEAK)
            );
        }

        UIHints.MutedReason reason = player.getRole() == RoleType.WOLF ? UIHints.MutedReason.PHASE_RESTRICTED : UIHints.MutedReason.NIGHT_SILENT;
        return new PlayerPermissionRow(
            player.getPlayerId(), player.getSeatNo(), player.getRole(), player.getTeam(), true, true, false,
            List.of(), List.of(), List.of(),
            new PublishPolicy(true, true, true),
            new ConsumePolicy(ConsumePolicy.Strategy.NONE, null),
            new UIHints(false, reason, UIHints.Mode.SILENT)
        );
    }

    private PlayerPermissionRow waitingRow(PlayerState player) {
        return new PlayerPermissionRow(
            player.getPlayerId(), player.getSeatNo(), player.getRole(), player.getTeam(), player.isAlive(), player.isOnline(), false,
            List.of(), List.of(), List.of(),
            new PublishPolicy(true, true, true),
            new ConsumePolicy(ConsumePolicy.Strategy.NONE, null),
            new UIHints(false, UIHints.MutedReason.PHASE_RESTRICTED, UIHints.Mode.SILENT)
        );
    }

    private PlayerPermissionRow offlineRow(PlayerState player) {
        return new PlayerPermissionRow(
            player.getPlayerId(), player.getSeatNo(), player.getRole(), player.getTeam(), player.isAlive(), false, false,
            List.of(), List.of(), List.of(),
            new PublishPolicy(false, true, true),
            new ConsumePolicy(ConsumePolicy.Strategy.NONE, null),
            new UIHints(false, UIHints.MutedReason.OFFLINE, UIHints.Mode.SILENT)
        );
    }
}
