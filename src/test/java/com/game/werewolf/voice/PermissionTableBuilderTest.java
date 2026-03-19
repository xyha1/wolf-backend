package com.game.werewolf.voice;

import com.game.werewolf.common.model.GamePhase;
import com.game.werewolf.common.model.RoleType;
import com.game.werewolf.common.model.TeamType;
import com.game.werewolf.room.domain.GameRoomState;
import com.game.werewolf.room.domain.PlayerState;
import com.game.werewolf.voice.app.PermissionTableBuilder;
import com.game.werewolf.voice.domain.ChannelId;
import com.game.werewolf.voice.domain.RoomVoicePermissionTable;
import com.game.werewolf.voice.domain.VoiceChangeReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionTableBuilderTest {

    private final PermissionTableBuilder builder = new PermissionTableBuilder();

    @Test
    void shouldBuildDayAndNightPermissions() {
        GameRoomState roomState = new GameRoomState("room-1");
        PlayerState wolf = new PlayerState("p1", 1, RoleType.WOLF, TeamType.WOLF);
        PlayerState villager = new PlayerState("p2", 2, RoleType.VILLAGER, TeamType.GOOD);
        roomState.upsertPlayer(wolf);
        roomState.upsertPlayer(villager);

        roomState.setPhase(GamePhase.DAY);
        roomState.setCurrentSpeakerId("p1");
        RoomVoicePermissionTable dayTable = builder.build(roomState, 1, VoiceChangeReason.SPEAKER_CHANGE);

        assertEquals(1, dayTable.rows().get("p1").speakChannels().size());
        assertTrue(dayTable.rows().get("p1").speakChannels().contains(ChannelId.DAY_PUBLIC));
        assertTrue(dayTable.rows().get("p2").speakChannels().isEmpty());
        assertTrue(dayTable.rows().get("p2").hearChannels().contains(ChannelId.DAY_PUBLIC));

        roomState.setPhase(GamePhase.NIGHT);
        roomState.setWolfChatOpen(true);
        RoomVoicePermissionTable nightTable = builder.build(roomState, 2, VoiceChangeReason.WOLF_CHAT_OPEN);

        assertTrue(nightTable.rows().get("p1").speakChannels().contains(ChannelId.WOLF_NIGHT));
        assertTrue(nightTable.rows().get("p1").hearChannels().contains(ChannelId.WOLF_NIGHT));
        assertTrue(nightTable.rows().get("p2").speakChannels().isEmpty());
        assertTrue(nightTable.rows().get("p2").hearChannels().isEmpty());
    }

    @Test
    void shouldSetOfflineRow() {
        GameRoomState roomState = new GameRoomState("room-2");
        PlayerState player = new PlayerState("p1", 1, RoleType.VILLAGER, TeamType.GOOD);
        roomState.upsertPlayer(player);
        roomState.setPhase(GamePhase.DAY);
        roomState.setCurrentSpeakerId("p1");

        player.setOnline(false);
        RoomVoicePermissionTable table = builder.build(roomState, 1, VoiceChangeReason.DISCONNECT);

        assertFalse(table.rows().get("p1").online());
        assertTrue(table.rows().get("p1").speakChannels().isEmpty());
        assertTrue(table.rows().get("p1").hearChannels().isEmpty());
    }
}
