package org.example.indian_poker.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter @Setter
public abstract class BaseGameRoom {
    protected String roomId;
    protected String roomName;
    protected boolean playing = false;

    // 동시성 제어를 위해 ConcurrentHashMap 사용
    protected Map<String, Player> users = new ConcurrentHashMap<>();

    public BaseGameRoom(String name) {
        this.roomId = UUID.randomUUID().toString();
        this.roomName = name;
    }

    public void enterUser(Player player) {
        users.put(player.getId(), player);
    }

    public void exitUser(String playerId) {
        users.remove(playerId);
    }

    // [추상 메서드] 자식들이 반드시 구현해야 할 로직
    public abstract GameMessage handleAction(GameMessage message);

    public abstract Map<String, Object> getGameSnapshot();
}