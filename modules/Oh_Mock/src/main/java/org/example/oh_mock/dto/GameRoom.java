package org.example.oh_mock.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.util.*;

@Getter @Setter
public class GameRoom {
    private String roomId;
    private String roomName;

    @JsonIgnore
    private Set<Player> users = new HashSet<>();

    // [오목 게임 상태]
    private int[][] board = new int[15][15]; // 0: 빈칸, 1: 흑, 2: 백
    private String blackPlayerId; // 선공 (1)
    private String whitePlayerId; // 후공 (2)
    private int currentTurn = 1;  // 현재 턴 (1:흑, 2:백)
    private boolean playing = false;
    private String winnerId;

    public static GameRoom create(String name) {
        GameRoom room = new GameRoom();
        room.roomId = UUID.randomUUID().toString();
        room.roomName = name;
        return room;
    }

    // 유저 입장 시 흑/백/관전자 배정
    public void assignSeat(Player player) {
        if (blackPlayerId == null) {
            blackPlayerId = player.getId();
            player.setStoneType(1);
        } else if (whitePlayerId == null) {
            whitePlayerId = player.getId();
            player.setStoneType(2);
        } else {
            player.setStoneType(0); // 관전자
        }
        users.add(player);
    }

    public void addUser(Player player){
        users.add(player);
    }

    public boolean removeUser(Player player) {
        if (player.getId().equals(blackPlayerId)) blackPlayerId = null;
        if (player.getId().equals(whitePlayerId)) whitePlayerId = null;
        return users.remove(player);
    }

    public void resetGame() {
        this.board = new int[15][15]; // 판 초기화
        this.currentTurn = 1;         // 흑돌 선공으로 리셋
        this.playing = true;
        this.winnerId = null;
    }
}