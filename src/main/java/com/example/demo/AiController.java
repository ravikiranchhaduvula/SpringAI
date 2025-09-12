package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/ai")
class AiController {

    private final ChatClient chat;
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    AiController(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    @PostMapping("/route")
    Map<String, Object> route(@RequestBody Map<String, Object> body) {
        final String q = String.valueOf(body.getOrDefault("q", ""));

        // Programmatic JSON mode + model/temperature.
        OpenAiChatOptions jsonOpts = OpenAiChatOptions.builder()
                .withModel("gpt-4o-mini")
                .withTemperature(0.2f)
                // Force JSON-object from OpenAI (Spring AI 1.0+)
                .withResponseFormat(new OpenAiApi.ChatCompletionRequest.ResponseFormat(
                        OpenAiApi.ChatCompletionRequest.ResponseFormat.Type.JSON_OBJECT
                ))
                .build();

        // Few-shot prompt: 2 examples + schema + rules.
        // system instruction string
        final String system = """
            You are an intent router. Output ONE JSON OBJECT ONLY (no prose, no code fences).
            Schema:
            {
              "action": "createPaymentHold" | null,
              "answer": string | null,
              "invoiceId": string | null,
              "amount": number | null
            }
            Rules:
            - If the user asks to place a payment hold, set action="createPaymentHold" and fill invoiceId and amount (number).
            - Otherwise set a short 'answer' and action=null.
            - No markdown, no commentary, no extra keys.
            """;

        final String exUser1 = "Place a hold on invoice INV-1001 for $120.50";
        final String exAsst1 = """
            {"action":"createPaymentHold","answer":null,"invoiceId":"INV-1001","amount":120.50}
            """;

        final String exUser2 = "How can I reduce p95 latency?";
        final String exAsst2 = """
            {"action":null,"answer":"Increase DB pool size, add caching, and profile slow queries.","invoiceId":null,"amount":null}
            """;

        // ---- Attempt 1 (JSON mode + few-shot)
        String raw1 = chat.prompt()
                .options(jsonOpts)
                .messages(
                new SystemMessage(system),          // your system instructions
                new UserMessage(exUser1),
                new AssistantMessage(exAsst1),
                new UserMessage(exUser2),
                new AssistantMessage(exAsst2),
                new UserMessage(q)                  // the live question
        )
                .call()
                .content();

        JsonNode node = coerceToJsonObject(raw1);

        // ---- Attempt 2 (stricter wording) if needed
        if (node == null) {
            final String stricter = system + "\nReturn ONLY the JSON object. No markdown, no commentary.";
            String raw2 = chat.prompt()
                    .options(jsonOpts)
                    .system(stricter)
                    .user(q)
                    .call()
                    .content();
            node = coerceToJsonObject(raw2);

            // Final fallback: succeed with plain answer to avoid errors blocking you
            if (node == null) {
                String fallback = fallbackAnswerFrom(raw2 != null ? raw2 : raw1);
                return Map.of("answer", fallback);
            }
        }

        // ---- Validate and build response
        String action  = text(node, "action");
        String answer  = text(node, "answer");
        String invoice = text(node, "invoiceId");
        BigDecimal amt = decimal(node, "amount");

        /** This is post processing **/
        if (action != null && !action.isBlank()) {
            if (!"createPaymentHold".equalsIgnoreCase(action)) {
                return Map.of("answer", "Unsupported action '" + action + "'. Returning as plain answer.");
            }
            if (invoice == null || invoice.isBlank() || amt == null) {
                return Map.of("answer", "Detected hold intent but missing invoiceId/amount.");
            }
            return Map.of("action", "createPaymentHold",
                    "args", Map.of("invoiceId", invoice, "amount", amt));
        }
        if (answer == null || answer.isBlank()) {
            return Map.of("answer", "OK. (No actionable intent detected.)");
        }
        return Map.of("answer", answer);
    }

    // ---------- helpers ----------

    private JsonNode coerceToJsonObject(String s) {
        if (s == null) return null;
        String t = s.trim();

        JsonNode n = readTree(t);
        if (n != null && n.isObject()) return n;

        // Strip common code fences
        String noFences = t.replaceAll("^\\s*```(?:json)?\\s*", "")
                .replaceAll("\\s*```\\s*$", "")
                .trim();
        n = readTree(noFences);
        if (n != null && n.isObject()) return n;

        // Extract first {...}
        String extracted = extractFirstObject(noFences);
        if (extracted != null) {
            n = readTree(extracted);
            if (n != null && n.isObject()) return n;
        }
        return null;
    }

    private JsonNode readTree(String s) {
        try { return mapper.readTree(s); }
        catch (JsonProcessingException e) { return null; }
    }

    private String extractFirstObject(String s) {
        int start = s.indexOf('{');
        if (start < 0) return null;
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return s.substring(start, i + 1);
            }
        }
        return null;
    }

    private String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private BigDecimal decimal(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.decimalValue();
        try { return new BigDecimal(v.asText()); } catch (Exception e) { return null; }
    }

    private String fallbackAnswerFrom(String raw) {
        if (raw == null) return "";
        String t = raw.trim()
                .replaceAll("^\\s*```(?:json)?\\s*", "")
                .replaceAll("\\s*```\\s*$", "")
                .trim();
        return t.length() > 800 ? t.substring(0, 800) : t;
    }
}
