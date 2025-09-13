// src/test/java/com/example/demo/AirouterdemoApplicationTests.java
package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@SpringBootTest
class AirouterdemoApplicationTests {

    @TestConfiguration
    static class TestBeans {
        @Bean
        ChatClient.Builder chatClientBuilder() {
            return mock(ChatClient.Builder.class); // satisfies AiController ctor
        }
        @Bean
        VectorStore vectorStore() {
            return mock(VectorStore.class);        // satisfies RagService ctor
        }
    }

    @Test
    void contextLoads() {
        // passes if application context starts
    }
}
