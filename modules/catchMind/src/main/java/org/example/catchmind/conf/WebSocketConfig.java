package org.example.catchmind.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic 으로 시작하는 메시지는 구독자들에게 바로 뿌려줍니다 (브로드캐스팅)
        config.enableSimpleBroker("/topic");
        // 클라이언트가 서버로 보낼 때는 /app 접두사를 붙입니다
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 프론트에서 접속할 엔드포인트: http://localhost:8080/ws
        // setAllowedOriginPatterns("*")는 로컬 테스트용 (CORS 허용)
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}