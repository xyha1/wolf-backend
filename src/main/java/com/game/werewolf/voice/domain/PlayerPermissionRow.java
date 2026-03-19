package com.game.werewolf.voice.domain;

import com.game.werewolf.common.model.RoleType;
import com.game.werewolf.common.model.TeamType;

import java.util.List;

public record PlayerPermissionRow(String playerId,
                                  Integer seatNo,
                                  RoleType role,
                                  TeamType team,
                                  boolean alive,
                                  boolean online,
                                  boolean spectator,
                                  List<ChannelId> speakChannels,
                                  List<ChannelId> hearChannels,
                                  List<ChannelId> observeChannels,
                                  PublishPolicy publishPolicy,
                                  ConsumePolicy consumePolicy,
                                  UIHints uiHints) {
}
