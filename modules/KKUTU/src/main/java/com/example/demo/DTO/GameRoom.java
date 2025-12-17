package com.example.demo.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class GameRoom {


    @Getter
    @Setter
    public static class PlayerInfo {
        private String uid;
        private String nickname;
        private boolean isBot;

        public PlayerInfo(String uid, String nickname) {
            this.uid = uid;
            this.nickname = nickname;
            this.isBot = uid.startsWith("AI_BOT_");
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlayerInfo that = (PlayerInfo) o;
            return Objects.equals(uid, that.uid);
        }
        @Override
        public int hashCode() {
            return Objects.hash(uid);
        }
    }

    private String roomId;
    private String roomName;
    private int currentTurnIndex;
    private String lastWord;

    private List<PlayerInfo> players = new ArrayList<>();
    private Set<String> usedWords = new HashSet<>();
    private int maxPlayers;
    private int botCount;
    private Map<String, Integer> failureCounts = new HashMap<>();

    public GameRoom(String roomId, String roomName, int maxPlayers, int botCount) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        this.botCount = botCount;

        for (int i = 0; i < botCount; i++) {
            String botId = "AI_BOT_" + (i + 1);
            this.players.add(new PlayerInfo(botId, botId));
            this.failureCounts.put(botId, 0);
        }

        this.currentTurnIndex = 0;
    }

    public boolean addPlayer(String uid, String nickname) {
        if (players.size() >= maxPlayers) {
            return false;
        }

        PlayerInfo newPlayer = new PlayerInfo(uid, nickname);
        players.add(newPlayer);
        failureCounts.put(uid, 0);

        // 첫 사람 턴 설정 로직
        if (players.size() - botCount == 1) {
            this.currentTurnIndex = players.indexOf(newPlayer);
        }
        return true;
    }


    public void removePlayer(String uid) {
        PlayerInfo playerToRemove = getPlayerByUid(uid);
        if (playerToRemove == null) return;

        int removedIndex = players.indexOf(playerToRemove);
        if (removedIndex != -1) {
            players.remove(playerToRemove);
            failureCounts.remove(uid);

            if (removedIndex < currentTurnIndex) {
                currentTurnIndex--;
            }
            if (currentTurnIndex >= players.size()) {
                currentTurnIndex = 0;
            }
        }
    }

    public int incrementFailureCount(String uid) {
        int count = failureCounts.getOrDefault(uid, 0) + 1;
        failureCounts.put(uid, count);
        return count;
    }

    public void resetFailureCount(String uid) {
        failureCounts.put(uid, 0);
    }

    public PlayerInfo getNextPlayer() {
        if (players.isEmpty()) return null;
        this.currentTurnIndex = (this.currentTurnIndex + 1) % players.size();
        return players.get(this.currentTurnIndex);
    }

    public PlayerInfo getCurrentPlayer() {
        if (players.isEmpty() || currentTurnIndex < 0 || currentTurnIndex >= players.size()) {
            return null;
        }
        return players.get(this.currentTurnIndex);
    }

    public PlayerInfo getPlayerByUid(String uid) {
        return players.stream().filter(p -> p.getUid().equals(uid)).findFirst().orElse(null);
    }

    public String getNicknameByUid(String uid) {
        PlayerInfo player = getPlayerByUid(uid);
        return (player != null) ? player.getNickname() : "(알수없음)";
    }
}