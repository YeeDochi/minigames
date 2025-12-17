package org.example.catchmind.controller;

import lombok.RequiredArgsConstructor;
import org.example.catchmind.dto.GameRoom;
import org.example.catchmind.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    // 방 목록 조회
    @GetMapping
    public List<GameRoom> findAllRooms() {
        return roomService.findAllRooms();
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoom> getRoomInfo(@PathVariable String roomId) {
        GameRoom room = roomService.findRoom(roomId);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(room);
    }

    // 방 생성
    @PostMapping
    public GameRoom createRoom(
            @RequestParam String name,
            @RequestParam(defaultValue = "5") int rounds // [추가] 라운드 수 받기
    ) {
        return roomService.createRoom(name, rounds);
    }
}