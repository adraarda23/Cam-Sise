package ardaaydinkilinc.Cam_Sise.core.controller;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.service.FillerService;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import ardaaydinkilinc.Cam_Sise.shared.exception.ResourceNotFoundException;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("FillerController Tests")
class FillerControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FillerService fillerService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final String AUTH_HEADER = "Bearer fake-token";

    private Filler filler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        lenient().when(jwtUtil.extractPoolOperatorId(any())).thenReturn(POOL_OPERATOR_ID);

        filler = Filler.register(POOL_OPERATOR_ID, "Test Dolumcu",
                new Address("Sanayi Cd.", "Bursa", "Osmangazi", "16200", "TR"),
                new GeoCoordinates(40.2, 29.0),
                new ContactInfo("02241234567", "test@test.com", "Ali Veli"),
                new TaxId("1234567890"));
        filler.clearDomainEvents();
    }

    @Nested
    @DisplayName("POST /api/fillers")
    class RegisterFiller {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("201 döndürmeli ve yeni dolumcu oluşturmalı")
        void shouldReturn201() throws Exception {
            when(fillerService.registerFiller(anyLong(), any(), any(), any(), any(), any(), any(),
                    anyDouble(), anyDouble(), any(), any(), any(), any()))
                    .thenReturn(filler);

            var body = new FillerController.RegisterFillerRequest(
                    "Test Dolumcu", "Sanayi Cd.", "Bursa", "Osmangazi", "16200", "TR",
                    40.2, 29.0, "02241234567", "test@test.com", "Ali Veli", "1234567890");

            mockMvc.perform(post("/api/fillers")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/fillers/{id}")
    class GetFiller {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200WhenFound() throws Exception {
            when(fillerService.findById(1L)).thenReturn(filler);

            mockMvc.perform(get("/api/fillers/1")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("404 döndürmeli (bulunamadı)")
        void shouldReturn404WhenNotFound() throws Exception {
            when(fillerService.findById(999L)).thenThrow(new ResourceNotFoundException("not found"));

            mockMvc.perform(get("/api/fillers/999")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/fillers")
    class GetAllFillers {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            var pageResponse = new PageResponse<>(List.of(filler), 1L, 1, 0, 20);
            when(fillerService.findByPoolOperatorPaged(anyLong(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/fillers")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/fillers/{id}/activate")
    class ActivateFiller {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(fillerService.activateFiller(1L)).thenReturn(filler);

            mockMvc.perform(post("/api/fillers/1/activate")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/fillers/{id}/deactivate")
    class DeactivateFiller {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(fillerService.deactivateFiller(1L)).thenReturn(filler);

            mockMvc.perform(post("/api/fillers/1/deactivate")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/fillers/{id}")
    class UpdateFiller {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli ve dolumcuyu güncellemeli")
        void shouldReturn200() throws Exception {
            when(fillerService.updateFiller(anyLong(), any(), any(), any(), any(), any(), any(),
                    anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(filler);

            var body = new FillerController.UpdateFillerRequest(
                    "Yeni Ad", "Sanayi Cd.", "Bursa", "Osmangazi", "16200", "TR",
                    40.2, 29.0, "02241234567", "test@test.com", "Ali Veli");

            mockMvc.perform(put("/api/fillers/1")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/fillers/{id}/contact")
    class UpdateContactInfo {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(fillerService.updateContactInfo(anyLong(), any(), any(), any())).thenReturn(filler);

            var body = new FillerController.UpdateContactInfoRequest("02241234568", "new@test.com", "Mehmet");

            mockMvc.perform(put("/api/fillers/1/contact")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/fillers/{id}/location")
    class UpdateLocation {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(fillerService.updateLocation(anyLong(), anyDouble(), anyDouble())).thenReturn(filler);

            var body = new FillerController.UpdateLocationRequest(40.25, 29.05);

            mockMvc.perform(put("/api/fillers/1/location")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }
}
