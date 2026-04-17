package ardaaydinkilinc.Cam_Sise.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kullanıcı giriş request DTO")
public class LoginRequest {

    @Schema(description = "Kullanıcı adı", example = "admin", required = true)
    @NotBlank(message = "Kullanıcı adı boş olamaz")
    private String username;

    @Schema(description = "Kullanıcı şifresi", example = "password123", required = true)
    @NotBlank(message = "Şifre boş olamaz")
    private String password;
}
