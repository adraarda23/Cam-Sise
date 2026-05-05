package ardaaydinkilinc.Cam_Sise.auth.controller;

import ardaaydinkilinc.Cam_Sise.auth.dto.LoginRequest;
import ardaaydinkilinc.Cam_Sise.auth.dto.LoginResponse;
import ardaaydinkilinc.Cam_Sise.auth.service.AuthService;
import ardaaydinkilinc.Cam_Sise.shared.exception.AuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("Geçerli kimlik bilgileriyle 200 ve token döndürmeli")
        void shouldReturn200WithTokenOnValidCredentials() throws Exception {
            LoginResponse response = LoginResponse.builder()
                    .token("eyJhbGciOi.test")
                    .username("staff01")
                    .role("COMPANY_STAFF")
                    .fullName("Ali Veli")
                    .build();
            when(authService.login(any())).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("staff01", "sifre123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("eyJhbGciOi.test"))
                    .andExpect(jsonPath("$.username").value("staff01"))
                    .andExpect(jsonPath("$.role").value("COMPANY_STAFF"));
        }

        @Test
        @DisplayName("Geçersiz kimlik bilgileriyle 401 döndürmeli")
        void shouldReturn401OnInvalidCredentials() throws Exception {
            when(authService.login(any())).thenThrow(new AuthenticationException("Hatalı şifre"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("staff01", "yanlis"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("@Valid hatası 400 döndürmeli (boş username)")
        void shouldReturn400OnMissingUsername() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("", "sifre123"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("@Valid hatası 400 döndürmeli (boş şifre)")
        void shouldReturn400OnMissingPassword() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("staff01", ""))))
                    .andExpect(status().isBadRequest());
        }
    }
}
