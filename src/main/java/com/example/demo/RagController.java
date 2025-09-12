package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService rag;
    private final ChatClient chat;

    public RagController(RagService rag, ChatClient.Builder builder) {
        this.rag = rag;
        this.chat = builder.build();
    }

    /** POST /rag/ingest  { "id":"runbook-1", "text":"...content...", "meta": { "tag":"runbook" } } */
    @PostMapping("/ingest")
    Map<String,Object> ingest(@RequestBody Map<String,Object> body) {
        String id   = String.valueOf(body.getOrDefault("id", UUID.randomUUID().toString()));
        String text = String.valueOf(body.getOrDefault("text", ""));
        @SuppressWarnings("unchecked")
        Map<String,Object> meta = (Map<String,Object>) body.getOrDefault("meta", Map.of());

        if (text.isBlank()) {
            return Map.of("status","error","message","text is required");
        }
        rag.ingest(id, text, meta);
        return Map.of("status","ok","id", id, "chars", text.length());
    }

    /** POST /rag/ask  { "q": "question", "k": 3 } — retrieves docs & answers with context */
    @PostMapping("/ask")
    Map<String,Object> ask(@RequestBody Map<String,Object> body) {
        String q = String.valueOf(body.getOrDefault("q",""));
        int k = Integer.parseInt(String.valueOf(body.getOrDefault("k", 3)));

        var docs = rag.retrieve(q, Math.max(1, Math.min(k, 8)));

        String context = docs.stream()
                .map(d -> "- " + d.getContent())
                .collect(Collectors.joining("\n"));

        String system = """
            You are a helpful assistant. Answer using ONLY the provided context.
            If the answer is not in the context, say "I don't know" briefly.

            Return a SHORT answer for the user.
            """;

        String user = "Question:\n" + q + "\n\nContext:\n" + context;

        String answer = chat.prompt()
                .system(system)
                .user(user)
                .call()
                .content();

        // include a small preview of the retrieved docs for transparency
        List<Map<String,Object>> sources = docs.stream().limit(3).map(d -> Map.of(
                "preview", d.getContent().length() > 160 ? d.getContent().substring(0,160) + "..." : d.getContent(),
                "metadata", d.getMetadata()
        )).toList();

        return Map.of("answer", answer, "sources", sources);
    }
}
