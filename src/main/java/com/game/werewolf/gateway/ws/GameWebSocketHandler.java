package com.game.werewolf.gateway.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.werewolf.common.model.GamePhase;
import com.game.werewolf.common.model.RoleType;
import com.game.werewolf.common.model.TeamType;
import com.game.werewolf.gameflow.app.GameFlowAppService;
import com.game.werewolf.room.app.RoomAppService;
import com.game.werewolf.room.domain.GameRoomState;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final RoomAppService roomAppService;
    private final GameFlowAppService gameFlowAppService;

    public GameWebSocketHandler(ObjectMapper objectMapper,
                                RoomAppService roomAppService,
                                GameFlowAppService gameFlowAppService) {
        this.objectMapper = objectMapper;
        this.roomAppService = roomAppService;
        this.gameFlowAppService = gameFlowAppService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        GameWsCommand command = objectMapper.readValue(message.getPayload(), GameWsCommand.class);
        GameRoomState roomState = switch (command.type()) {
            case "join" -> roomAppService.joinRoom(command.roomId(), command.playerId(), command.seatNo(), RoleType.valueOf(command.role()), TeamType.valueOf(command.team()));
            case "leave" -> roomAppService.leaveRoom(command.roomId(), command.playerId());
            case "disconnect" -> roomAppService.disconnectPlayer(command.roomId(), command.playerId());
            case "reconnect" -> roomAppService.reconnectPlayer(command.roomId(), command.playerId());
            case "add_spectator" -> roomAppService.addSpectator(command.roomId(), command.spectatorId());
            case "set_phase" -> gameFlowAppService.setPhase(command.roomId(), GamePhase.valueOf(command.phase()));
            case "set_speaker" -> gameFlowAppService.setSpeaker(command.roomId(), command.speakerId());
            case "advance_speaker" -> gameFlowAppService.advanceSpeaker(command.roomId());
            case "set_wolf_chat" -> gameFlowAppService.setWolfChatOpen(command.roomId(), Boolean.TRUE.equals(command.wolfChatOpen()));
            default -> throw new IllegalArgumentException("Unsupported command: " + command.type());
        };

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", true);
        payload.put("roomId", roomState.getRoomId());
        payload.put("phase", roomState.getPhase());
        payload.put("speaker", roomState.getCurrentSpeakerId());
        payload.put("revision", roomState.getCommittedVoiceRevision());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }
}
