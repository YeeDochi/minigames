package org.example.oh_mock.conf;

import lombok.RequiredArgsConstructor;
import org.example.oh_mock.dto.GameMessage;
import org.example.oh_mock.service.GameService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final GameService gameService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();

        if (attributes != null) {
            String roomId = (String) attributes.get("roomId");
            String senderId = (String) attributes.get("senderId");
            String sender = (String) attributes.get("sender");

            if (roomId != null && senderId != null) {
                System.out.println("사용자 연결 끊김 감지: " + sender);

                GameMessage exitMsg = GameMessage.builder()
                        .type("EXIT")
                        .sender(sender)
                        .senderId(senderId)
                        .build();

                gameService.exit(roomId, exitMsg);
            }
        }
    }
}