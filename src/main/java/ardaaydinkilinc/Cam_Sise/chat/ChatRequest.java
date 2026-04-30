package ardaaydinkilinc.Cam_Sise.chat;

import java.util.List;

public record ChatRequest(
        String message,
        List<MessagePair> history
) {
    public record MessagePair(String role, String text) {}
}
