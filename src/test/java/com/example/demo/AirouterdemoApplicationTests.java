package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@SpringBootTest
class AirouterdemoApplicationTests {

    @TestConfiguration
    static class MockConfig {
        @Bean
        ChatClient.Builder chatClientBuilder() {
            // Provide a dummy bean to satisfy the controller
            return mock(ChatClient.Builder.class);
        }
    }

    @Test
    void contextLoads() {
        // passes if the context starts
    }
}
