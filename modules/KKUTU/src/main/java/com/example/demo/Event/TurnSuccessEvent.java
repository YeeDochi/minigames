package com.example.demo.Event;


import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
@Getter
@Setter
public class TurnSuccessEvent extends ApplicationEvent {
    private final String roomId;
    // nextPlayerId -> nextPlayerUid
    private final String nextPlayerUid;
    private final String lastword;

    // userId -> uid
    public TurnSuccessEvent(Object source, String roomId, String uid, String word) {
        super(source);
        this.roomId = roomId;
        this.nextPlayerUid = uid; // [!!!]
        this.lastword = word;
    }

    // (호환성을 위해) 기존 Getter 유지, 새 Getter 추가
    public String getNextPlayerId() {
        return nextPlayerUid;
    }
    public String getNextPlayerUid() {
        return nextPlayerUid;
    }
}