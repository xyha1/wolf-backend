package com.game.werewolf.voice.domain;

import java.util.Map;

public record RoomVoicePermissionTable(RoomVoiceContext context,
                                       Map<ChannelId, ChannelDefinition> channels,
                                       Map<String, PlayerPermissionRow> rows) {
}
