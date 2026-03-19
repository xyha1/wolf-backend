package com.game.werewolf.gateway.http;

import com.game.werewolf.common.model.GamePhase;
import com.game.werewolf.common.model.RoleType;
import com.game.werewolf.common.model.SpectatorMode;
import com.game.werewolf.common.model.TeamType;
import com.game.werewolf.gameflow.app.GameFlowAppService;
import com.game.werewolf.room.app.RoomAppService;
import com.game.werewolf.room.domain.GameRoomState;
import com.game.werewolf.room.repository.RoomRepository;
import com.game.werewolf.voice.diff.RoomVoiceDiff;
import com.game.werewolf.voice.domain.RoomVoicePermissionTable;
import com.game.werewolf.voice.repository.VoicePermissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomDebugController {

    private final RoomAppService roomAppService;
    private final GameFlowAppService gameFlowAppService;
    private final RoomRepository roomRepository;
    private final VoicePermissionRepository voicePermissionRepository;

    public RoomDebugController(RoomAppService roomAppService,
                               GameFlowAppService gameFlowAppService,
                               RoomRepository roomRepository,
                               VoicePermissionRepository voicePermissionRepository) {
        this.roomAppService = roomAppService;
        this.gameFlowAppService = gameFlowAppService;
        this.roomRepository = roomRepository;
        this.voicePermissionRepository = voicePermissionRepository;
    }

    @PostMapping("/{roomId}/players")
    @ResponseStatus(HttpStatus.CREATED)
    public GameRoomState joinPlayer(@PathVariable("roomId") String roomId, @RequestBody JoinPlayerRequest request) {
        return roomAppService.joinRoom(
            roomId,
            request.playerId(),
            request.seatNo(),
            RoleType.valueOf(normalizeEnum(request.role())),
            TeamType.valueOf(normalizeEnum(request.team()))
        );
    }

    @PostMapping("/{roomId}/players/{playerId}/disconnect")
    public GameRoomState disconnectPlayer(@PathVariable("roomId") String roomId, @PathVariable("playerId") String playerId) {
        return roomAppService.disconnectPlayer(roomId, playerId);
    }

    @PostMapping("/{roomId}/players/{playerId}/reconnect")
    public GameRoomState reconnectPlayer(@PathVariable("roomId") String roomId, @PathVariable("playerId") String playerId) {
        return roomAppService.reconnectPlayer(roomId, playerId);
    }

    @PostMapping("/{roomId}/spectators")
    @ResponseStatus(HttpStatus.CREATED)
    public GameRoomState addSpectator(@PathVariable("roomId") String roomId, @RequestBody AddSpectatorRequest request) {
        return roomAppService.addSpectator(roomId, request.spectatorId());
    }

    @PostMapping("/{roomId}/spectator-config")
    public GameRoomState updateSpectatorConfig(@PathVariable("roomId") String roomId, @RequestBody SpectatorConfigRequest request) {
        return roomAppService.updateSpectatorConfig(roomId, SpectatorMode.valueOf(normalizeEnum(request.mode())), request.delayMs());
    }

    @PostMapping("/{roomId}/phase")
    public GameRoomState setPhase(@PathVariable("roomId") String roomId, @RequestBody SetPhaseRequest request) {
        return gameFlowAppService.setPhase(roomId, GamePhase.valueOf(normalizeEnum(request.phase())));
    }

    @PostMapping("/{roomId}/speaker")
    public GameRoomState setSpeaker(@PathVariable("roomId") String roomId, @RequestBody SetSpeakerRequest request) {
        return gameFlowAppService.setSpeaker(roomId, request.speakerId());
    }

    @PostMapping("/{roomId}/speaker/advance")
    public GameRoomState advanceSpeaker(@PathVariable("roomId") String roomId) {
        return gameFlowAppService.advanceSpeaker(roomId);
    }

    @PostMapping("/{roomId}/wolf-chat")
    public GameRoomState setWolfChat(@PathVariable("roomId") String roomId, @RequestBody WolfChatRequest request) {
        return gameFlowAppService.setWolfChatOpen(roomId, request.open());
    }

    @GetMapping("/{roomId}")
    public GameRoomState roomState(@PathVariable("roomId") String roomId) {
        return roomRepository.find(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "room not found"));
    }

    @GetMapping("/{roomId}/voice")
    public Map<String, Object> voiceState(@PathVariable("roomId") String roomId) {
        RoomVoicePermissionTable table = voicePermissionRepository.findTable(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "voice table not found"));
        RoomVoiceDiff lastDiff = voicePermissionRepository.findLastDiff(roomId).orElse(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("table", table);
        payload.put("lastDiff", lastDiff);
        return payload;
    }

    private record JoinPlayerRequest(String playerId, int seatNo, String role, String team) {
    }

    private record AddSpectatorRequest(String spectatorId) {
    }

    private record SpectatorConfigRequest(String mode, int delayMs) {
    }

    private record SetPhaseRequest(String phase) {
    }

    private record SetSpeakerRequest(String speakerId) {
    }

    private record WolfChatRequest(boolean open) {
    }

    private String normalizeEnum(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
