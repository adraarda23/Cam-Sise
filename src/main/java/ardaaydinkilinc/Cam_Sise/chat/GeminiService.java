package ardaaydinkilinc.Cam_Sise.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key={apiKey}";
    @Value("${gemini.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(String userMessage, List<ChatRequest.MessagePair> history, String systemPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Gemini API anahtarı yapılandırılmamış.";
        }

        try {
            List<Map<String, Object>> contents = new ArrayList<>();
            for (ChatRequest.MessagePair msg : history) {
                contents.add(Map.of(
                        "role", msg.role(),
                        "parts", List.of(Map.of("text", msg.text()))
                ));
            }
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userMessage))
            ));

            Map<String, Object> body = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", systemPrompt))
                    ),
                    "contents", contents
            );

            @SuppressWarnings("unchecked")
            Map<?, ?> response = restTemplate.postForObject(GEMINI_URL, body, Map.class, apiKey);
            return extractText(response);

        } catch (Exception e) {
            log.error("Gemini API error", e);
            return "Şu an yanıt verilemiyor. Lütfen daha sonra tekrar deneyin.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> body) {
        try {
            var candidates = (List<?>) body.get("candidates");
            var candidate = (Map<?, ?>) candidates.get(0);
            var content = (Map<?, ?>) candidate.get("content");
            var parts = (List<?>) content.get("parts");
            var part = (Map<?, ?>) parts.get(0);
            return (String) part.get("text");
        } catch (Exception e) {
            return "Yanıt alınamadı. Lütfen tekrar deneyin.";
        }
    }
}
