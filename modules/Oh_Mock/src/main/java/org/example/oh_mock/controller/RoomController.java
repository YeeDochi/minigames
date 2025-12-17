package org.example.oh_mock.controller;

import lombok.RequiredArgsConstructor;
import org.example.oh_mock.dto.GameRoom;
import org.example.oh_mock.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
            @RequestParam String name
    ) {
        return roomService.createRoom(name);
    }
}