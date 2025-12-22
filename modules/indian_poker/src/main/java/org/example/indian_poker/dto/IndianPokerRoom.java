package org.example.indian_poker.dto;

import java.util.*;

public class IndianPokerRoom extends BaseGameRoom {
    private List<Card> deck = new ArrayList<>();
    private Map<String, Card> playerHands = new HashMap<>();
    private List<String> turnOrder = new ArrayList<>();
    private int currentTurnIdx = 0;
    private Set<String> diedPlayers = new HashSet<>();

    // 칩 관리 시스템
    private Map<String, Integer> chips = new HashMap<>();
    private final int STARTING_CHIPS = 30;

    public IndianPokerRoom(String name) {
        super(name);
    }

    // [1] 게임 시작
    public void startGame() {
        if (users.size() < 2) return;

        if (chips.isEmpty() || chips.size() < users.size()) {
            for (String pid : users.keySet()) {
                chips.putIfAbsent(pid, STARTING_CHIPS);
            }
        }

        deck.clear();
        String[] suits = {"S", "D", "H", "C"};
        for (String s : suits) {
            for (int i = 1; i <= 13; i++) deck.add(new Card(s, i));
        }
        Collections.shuffle(deck);

        playerHands.clear();
        diedPlayers.clear();

        turnOrder = new ArrayList<>(users.keySet());
        Collections.shuffle(turnOrder);
        currentTurnIdx = 0;

        for (String pid : turnOrder) {
            if (!deck.isEmpty()) playerHands.put(pid, deck.remove(0));
            int current = chips.getOrDefault(pid, 0);
            chips.put(pid, Math.max(0, current - 1));
        }

        this.playing = true;
    }

    // [2] 액션 처리
    @Override
    public synchronized GameMessage handleAction(GameMessage message) {
        String type = (String) message.getData().get("actionType");
        String senderId = message.getSenderId();

        if ("START".equals(type)) {
            if (users.size() < 2) return makeChat("SYSTEM", "최소 2명이 필요합니다.");
            startGame();
            return makeMessage("UPDATE", "새로운 라운드 시작! (참가비 -1)");
        }

        if (!playing) return null;
        if (!senderId.equals(turnOrder.get(currentTurnIdx))) return null;

        if ("BET".equals(type)) {
            String choice = (String) message.getData().get("betChoice");

            if ("DIE".equals(choice)) {
                diedPlayers.add(senderId);
                int c = chips.getOrDefault(senderId, 0);
                chips.put(senderId, Math.max(0, c - 5));
            }

            currentTurnIdx++;

            if (currentTurnIdx >= turnOrder.size()) {
                return calculateRoundResult();
            } else {
                String nextPlayer = getNickname(turnOrder.get(currentTurnIdx));
                return makeMessage("UPDATE", "다음 턴: " + nextPlayer);
            }
        }
        return null;
    }

    // [3] 결과 정산
    private GameMessage calculateRoundResult() {
        this.playing = false;

        String winnerId = null;
        int maxRank = -1;

        for (String pid : turnOrder) {
            if (diedPlayers.contains(pid)) continue;
            int rank = playerHands.get(pid).getRank();
            if (rank > maxRank) {
                maxRank = rank;
                winnerId = pid;
            }
        }

        String content;
        if (winnerId != null) {
            int c = chips.getOrDefault(winnerId, 0);
            chips.put(winnerId, c + 10);
            content = "🏆 승자: " + getNickname(winnerId) + " (+10칩)";
        } else {
            content = "모두 포기하여 승자가 없습니다.";
        }

        String bankruptUser = null;
        for (Map.Entry<String, Integer> entry : chips.entrySet()) {
            if (entry.getValue() <= 0) {
                bankruptUser = getNickname(entry.getKey());
                break;
            }
        }

        GameMessage msg = new GameMessage();
        msg.setRoomId(roomId);

        if (bankruptUser != null) {
            msg.setType("GAME_OVER");
            msg.setContent("⛔ " + bankruptUser + "님 파산! 게임 종료!");
            chips.clear();
        } else {
            msg.setType("ROUND_END");
            msg.setContent(content);
        }

        Map<String, Object> data = getGameSnapshot();
        data.put("winnerName", winnerId != null ? getNickname(winnerId) : "None");
        msg.setData(data);

        return msg;
    }

    // [4] 데이터 전송 (닉네임 추가됨!)
    @Override
    public Map<String, Object> getGameSnapshot() {
        String currentTurnId = (playing && !turnOrder.isEmpty() && currentTurnIdx < turnOrder.size())
                ? turnOrder.get(currentTurnIdx) : "";

        // 닉네임 맵 생성 (ID -> Nickname)
        Map<String, String> nicknames = new HashMap<>();
        for (String id : users.keySet()) {
            nicknames.put(id, users.get(id).getNickname());
        }

        return new HashMap<>(Map.of(
                "turnId", currentTurnId,
                "playing", playing,
                "hands", playerHands,
                "diedPlayers", diedPlayers,
                "chips", chips,
                "nicknames", nicknames // ★ 핵심: 닉네임 정보 전송
        ));
    }

    private String getNickname(String id) {
        return users.containsKey(id) ? users.get(id).getNickname() : "Unknown";
    }

    private GameMessage makeMessage(String type, String content) {
        GameMessage msg = new GameMessage();
        msg.setType(type);
        msg.setRoomId(roomId);
        msg.setContent(content);
        msg.setData(getGameSnapshot());
        return msg;
    }

    private GameMessage makeChat(String sender, String content) {
        return GameMessage.builder().type("CHAT").roomId(roomId).sender(sender).content(content).build();
    }
}