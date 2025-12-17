package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // <-- [핵심] 이 어노테이션이 SimpMessagingTemplate 빈을 생성시킵니다.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. 메시지 브로커가 /topic 으로 시작하는 주제(topic)를 구독한 클라이언트들에게 메시지 전파
        registry.enableSimpleBroker("/topic");
        // 2. 클라이언트가 서버로 메시지를 보낼 때 사용할 접두사(prefix)
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 3. 클라이언트가 WebSocket에 처음 연결할 때 사용할 엔드포인트
        registry.addEndpoint("/ws") // ex: http://localhost:8080/ws
                .withSockJS();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1); // 간단한 지연 발송 작업용
        scheduler.setThreadNamePrefix("websocket-task-scheduler-");
        scheduler.initialize();
        return scheduler;
    }

}