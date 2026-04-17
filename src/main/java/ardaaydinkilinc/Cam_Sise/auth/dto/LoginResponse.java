package ardaaydinkilinc.Cam_Sise.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kullanıcı giriş response DTO")
public class LoginResponse {

    @Schema(description = "JWT token (Authorization header'ında kullanılmalı)", example = "eyJhbGciOiJIUzI1NiIs...")
    private String token;

    @Schema(description = "Kullanıcı adı", example = "admin")
    private String username;

    @Schema(description = "Kullanıcı rolü (ADMIN, COMPANY_STAFF, CUSTOMER)", example = "ADMIN")
    private String role;

    @Schema(description = "Kullanıcı tam adı", example = "Admin User")
    private String fullName;

    @Schema(description = "Dolumcu ID (sadece CUSTOMER rolü için)", example = "1")
    private Long fillerId; // CUSTOMER rolü için
}
