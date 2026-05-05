package ardaaydinkilinc.Cam_Sise.settings.controller;

import ardaaydinkilinc.Cam_Sise.settings.domain.CompanySettings;
import ardaaydinkilinc.Cam_Sise.settings.service.CompanySettingsService;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CompanySettingsController Tests")
class CompanySettingsControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanySettingsService companySettingsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final String AUTH_HEADER = "Bearer fake-token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        lenient().when(jwtUtil.extractPoolOperatorId(any())).thenReturn(POOL_OPERATOR_ID);
    }

    @Nested
    @DisplayName("GET /api/settings")
    class GetSettings {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli ve ayarları getirmeli")
        void shouldReturn200WithSettings() throws Exception {
            CompanySettings settings = new CompanySettings(POOL_OPERATOR_ID);
            settings.setMinPalletRequestQty(20);
            settings.setMinSeparatorRequestQty(10);
            when(companySettingsService.getSettings(POOL_OPERATOR_ID)).thenReturn(settings);

            mockMvc.perform(get("/api/settings")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.minPalletRequestQty").value(20));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER rolüyle 200 döndürmeli")
        void shouldReturn200ForCustomer() throws Exception {
            CompanySettings settings = new CompanySettings(POOL_OPERATOR_ID);
            when(companySettingsService.getSettings(POOL_OPERATOR_ID)).thenReturn(settings);

            mockMvc.perform(get("/api/settings")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/settings")
    class UpdateSettings {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli ve ayarları güncellemeli")
        void shouldReturn200WhenUpdateSettings() throws Exception {
            CompanySettings updated = new CompanySettings(POOL_OPERATOR_ID);
            updated.setMinPalletRequestQty(30);
            updated.setMinSeparatorRequestQty(15);
            when(companySettingsService.updateSettings(anyLong(), any(Integer.class), any(Integer.class)))
                    .thenReturn(updated);

            var body = new CompanySettingsController.UpdateSettingsRequest(30, 15);

            mockMvc.perform(put("/api/settings")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.minPalletRequestQty").value(30));
        }
    }
}
