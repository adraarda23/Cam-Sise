package ardaaydinkilinc.Cam_Sise.auth.controller;

import ardaaydinkilinc.Cam_Sise.auth.dto.LoginRequest;
import ardaaydinkilinc.Cam_Sise.auth.dto.LoginResponse;
import ardaaydinkilinc.Cam_Sise.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for authentication and authorization.
 * Handles user login and JWT token generation.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Kimlik doğrulama ve yetkilendirme API'leri")
public class AuthController {

    private final AuthService authService;

    /**
     * User login endpoint.
     * Authenticates user credentials and returns JWT token.
     */
    @Operation(
            summary = "Kullanıcı girişi",
            description = "Kullanıcı adı ve şifre ile giriş yapar, JWT token döner. " +
                    "Token, sonraki isteklerde Authorization header'ında kullanılmalıdır."
    )
    @ApiResponse(responseCode = "200", description = "Giriş başarılı, JWT token döndü")
    @ApiResponse(responseCode = "401", description = "Geçersiz kullanıcı adı veya şifre")
    @ApiResponse(responseCode = "400", description = "Geçersiz request formatı")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
