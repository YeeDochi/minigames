package org.example.indian_poker.service;

import org.example.indian_poker.dto.BaseGameRoom;
import org.example.indian_poker.dto.IndianPokerRoom;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    private final Map<String, IndianPokerRoom> rooms = new ConcurrentHashMap<>();

    public IndianPokerRoom createRoom(String name) {
        IndianPokerRoom room = new IndianPokerRoom(name);

        rooms.put(room.getRoomId(), room);
        return room;
    }

    public BaseGameRoom findRoom(String roomId) {
        return rooms.get(roomId);
    }

    public List<BaseGameRoom> findAll() {
        return new ArrayList<>(rooms.values());
    }

    public void deleteRoom(String roomId) {
        rooms.remove(roomId);
    }
}