package org.example.oh_mock.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.oh_mock.dto.GameRoom;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Service
@RequiredArgsConstructor
public class RoomService {

    // 메모리에 방 저장
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public GameRoom createRoom(String name) {
        GameRoom room = GameRoom.create(name);
        rooms.put(room.getRoomId(), room);
        return room;
    }

    public GameRoom findRoom(String roomId){
        return rooms.get(roomId);
    }

    public List<GameRoom> findAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public void deleteRoom(String roomId) {
        rooms.remove(roomId);
    }

}
