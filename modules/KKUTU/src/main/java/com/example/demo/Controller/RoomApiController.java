package com.example.demo.Controller;


import com.example.demo.DTO.CreateRoomRequest;
import com.example.demo.DTO.GameRoom;
import com.example.demo.DTO.RoomInfoDTO;
import com.example.demo.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms") // API 경로는 /api/ 로 시작하는 것을 권장
public class RoomApiController {

    private final GameRoomService gameRoomService;

    @PostMapping
    public Map<String, String> createRoom(@RequestBody CreateRoomRequest request) {
        GameRoom newRoom = gameRoomService.createRoom(
                request.getRoomName(),
                request.getMaxPlayers(),
                request.getBotCount()
        );

        return Map.of("roomId", newRoom.getRoomId());
    }

    @GetMapping
    public List<RoomInfoDTO> getActiveRooms() {
        Map<String, GameRoom> activeRooms = gameRoomService.getActiveGameRooms();

        return activeRooms.values().stream()
                .map(room -> new RoomInfoDTO(
                        room.getRoomId(),
                        room.getRoomName(),
                        room.getPlayers().size(), // 현재 플레이어 수 계산
                        room.getMaxPlayers(),
                        room.getBotCount()
                ))
                .collect(Collectors.toList());
    }
}