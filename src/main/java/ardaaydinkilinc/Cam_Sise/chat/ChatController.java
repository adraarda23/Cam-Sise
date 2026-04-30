package ardaaydinkilinc.Cam_Sise.chat;

import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Yapay zeka destekli müşteri hizmetleri chat API'si")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;
    private final JwtUtil jwtUtil;

    @GetMapping("/welcome")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'COMPANY_STAFF')")
    public ResponseEntity<Map<String, String>> welcome(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.extractUsername(token);
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(token);
        String message = chatService.buildWelcomeMessage(username, poolOperatorId);
        return ResponseEntity.ok(Map.of("reply", message));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'COMPANY_STAFF')")
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        String token = httpRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.extractUsername(token);
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(token);

        String reply = chatService.chat(
                username,
                poolOperatorId,
                request.message(),
                request.history() != null ? request.history() : List.of()
        );
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
