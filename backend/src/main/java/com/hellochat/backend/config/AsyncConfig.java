package com.hellochat.backend.config;

import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(HighConcurrencyProperties.class)
public class AsyncConfig {

    @Bean("eventDispatchExecutor")
    public Executor eventDispatchExecutor(HighConcurrencyProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("hello-chat-event-");
        executor.setCorePoolSize(properties.getEventCorePoolSize());
        executor.setMaxPoolSize(properties.getEventMaxPoolSize());
        executor.setQueueCapacity(properties.getEventQueueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
