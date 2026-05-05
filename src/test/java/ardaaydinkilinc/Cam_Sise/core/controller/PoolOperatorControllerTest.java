package ardaaydinkilinc.Cam_Sise.core.controller;

import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import ardaaydinkilinc.Cam_Sise.core.service.PoolOperatorService;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import ardaaydinkilinc.Cam_Sise.shared.exception.ResourceNotFoundException;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PoolOperatorController Tests")
class PoolOperatorControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PoolOperatorService poolOperatorService;

    private MockMvc mockMvc;

    private PoolOperator poolOperator;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        poolOperator = PoolOperator.register("Test A.Ş.",
                new TaxId("1234567890"),
                new ContactInfo("02121234567", "test@test.com", "Test Person"));
        poolOperator.clearDomainEvents();
    }

    @Nested
    @DisplayName("POST /api/pool-operators")
    class RegisterPoolOperator {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("201 döndürmeli ve yeni pool operator oluşturmalı")
        void shouldReturn201WithNewPoolOperator() throws Exception {
            when(poolOperatorService.registerPoolOperator(any(), any(), any(), any(), any()))
                    .thenReturn(poolOperator);

            var body = new PoolOperatorController.RegisterPoolOperatorRequest(
                    "Test A.Ş.", "1234567890", "02121234567", "test@test.com", "Test Person");

            mockMvc.perform(post("/api/pool-operators")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 döndürmeli (boş companyName)")
        void shouldReturn400OnMissingCompanyName() throws Exception {
            var body = new PoolOperatorController.RegisterPoolOperatorRequest(
                    "", "1234567890", "02121234567", "test@test.com", "Test Person");

            mockMvc.perform(post("/api/pool-operators")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/pool-operators/{id}")
    class GetPoolOperator {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 döndürmeli")
        void shouldReturn200WhenFound() throws Exception {
            when(poolOperatorService.findById(1L)).thenReturn(poolOperator);

            mockMvc.perform(get("/api/pool-operators/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 döndürmeli (bulunamadı)")
        void shouldReturn404WhenNotFound() throws Exception {
            when(poolOperatorService.findById(999L)).thenThrow(new ResourceNotFoundException("not found"));

            mockMvc.perform(get("/api/pool-operators/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/pool-operators")
    class GetAllPoolOperators {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Tüm operatörleri döndürmeli")
        void shouldReturnAll() throws Exception {
            when(poolOperatorService.findAll()).thenReturn(List.of(poolOperator));

            mockMvc.perform(get("/api/pool-operators"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Aktif operatörleri filtreleyerek döndürmeli")
        void shouldReturnActiveOnly() throws Exception {
            when(poolOperatorService.findAllActive()).thenReturn(List.of(poolOperator));

            mockMvc.perform(get("/api/pool-operators?active=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/pool-operators/{id}/activate")
    class ActivatePoolOperator {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(poolOperatorService.activatePoolOperator(1L)).thenReturn(poolOperator);

            mockMvc.perform(post("/api/pool-operators/1/activate"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/pool-operators/{id}/deactivate")
    class DeactivatePoolOperator {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(poolOperatorService.deactivatePoolOperator(1L)).thenReturn(poolOperator);

            mockMvc.perform(post("/api/pool-operators/1/deactivate"))
                    .andExpect(status().isOk());
        }
    }
}
