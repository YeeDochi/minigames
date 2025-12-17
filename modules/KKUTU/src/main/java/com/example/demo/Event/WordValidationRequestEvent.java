package com.example.demo.Event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WordValidationRequestEvent extends ApplicationEvent {
    // Getter...
    private final String roomId;
    private final String word;
    private final String userId;

    public WordValidationRequestEvent(Object source, String roomId, String word, String userId) {
        super(source);
        this.roomId = roomId;
        this.word = word;
        this.userId = userId;
    }

}