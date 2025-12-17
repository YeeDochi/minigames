package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 봇이 API를 호출할 때 사용할 기본 스레드 수
        executor.setCorePoolSize(5);
        // 동시에 처리할 수 있는 최대 스레드 수
        executor.setMaxPoolSize(10);
        // 대기열 크기
        executor.setQueueCapacity(100);
        // 스레드 이름 접두사 (로그 볼 때 편함)
        executor.setThreadNamePrefix("async-bot-");

        executor.initialize();
        return executor;
    }
}