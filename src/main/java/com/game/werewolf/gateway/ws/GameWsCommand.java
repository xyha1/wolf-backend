package com.game.werewolf.gateway.ws;

public record GameWsCommand(String type,
                            String roomId,
                            String playerId,
                            Integer seatNo,
                            String role,
                            String team,
                            String speakerId,
                            String spectatorId,
                            Boolean wolfChatOpen,
                            String phase) {
}
