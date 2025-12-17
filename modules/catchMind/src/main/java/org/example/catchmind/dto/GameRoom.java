package org.example.catchmind.dto;

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

    private String currentDrawer; // 현재 출제자 닉네임
    private String currentAnswer;
    private String currentDrawerId;
    private boolean playing = false;

    private Integer maxRounds = 5; // 기본 5판 (정답 5번 나오면 끝)
    private Integer currentRound = 0;

    public static GameRoom create(String name, int maxRounds) {
        GameRoom room = new GameRoom();
        room.roomId = UUID.randomUUID().toString();
        room.roomName = name;
        room.maxRounds = maxRounds; // 설정값 적용
        return room;
    }

    public void setDrawer(String playerId) {
        for (Player p : users) {
            if (p.getId().equals(playerId)) {
                this.currentDrawer = p.getNickname();
                this.currentDrawerId = p.getId();
                return;
            }
        }
        // 못 찾으면(퇴장했을 경우) 랜덤으로 돌림
        nextRandomDrawer();
    }

    public void nextRandomDrawer() {
        if (users.isEmpty()) return;
        List<Player> userList = new ArrayList<>(users);
        Player selected = userList.get(new Random().nextInt(userList.size()));
        this.currentDrawer = selected.getNickname();
        this.currentDrawerId = selected.getId();
    }
    public void addScore(String playerId, int score) {
        for (Player p : users) {
            if (p.getId().equals(playerId)) {
                p.addPoint(score);
                break;
            }
        }
    }
    public List<Player> getRanking() {
        List<Player> list = new ArrayList<>(users);
        list.sort((p1, p2) -> p2.getPoint() - p1.getPoint()); // 점수 높은 순
        return list;
    }
    public void addUser(Player player){
        users.add(player);
    }
    public boolean removeUser(Player player){
        return users.remove(player); // 지워졌으면 true, 원래 없었으면 false
    }
    public void nextTurn() {
        if (users.isEmpty()) {
            System.out.println("ERROR: 방에 사람이 없어서 출제자를 뽑을 수 없습니다.");
            return;
        }
        List<Player> userList = new ArrayList<>(users);

        userList.sort(Comparator.comparing(Player::getNickname)
                .thenComparing(Player::getId));
        int currentIndex = -1;
        if (this.currentDrawerId != null) {
            for (int i = 0; i < userList.size(); i++) {
                if (userList.get(i).getId().equals(this.currentDrawerId)) {
                    currentIndex = i;
                    break;
                }
            }
        }

        int nextIndex = (currentIndex + 1) % userList.size();

        Player nextPlayer = userList.get(nextIndex);
        this.currentDrawer = nextPlayer.getNickname();
        this.currentDrawerId = nextPlayer.getId();

    }

    public void start(){
        this.playing = true;
    }
    public void stop(){
        this.playing = false;

    }
    public void resetGame() {
        this.currentRound = 0;
        this.currentAnswer = null;
        this.playing = true;

        // 모든 유저 점수 0으로 초기화
        for (Player p : users) {
            p.setPoint(0);
        }
    }
}