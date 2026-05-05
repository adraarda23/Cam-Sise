package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.domain.Depot;
import ardaaydinkilinc.Cam_Sise.logistics.service.DepotService;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
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
@DisplayName("DepotController Tests")
class DepotControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DepotService depotService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long DEPOT_ID = 10L;
    private static final Long VEHICLE_ID = 20L;
    private static final String AUTH_HEADER = "Bearer fake-token";

    private Depot depot;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        lenient().when(jwtUtil.extractPoolOperatorId(any())).thenReturn(POOL_OPERATOR_ID);

        depot = Depot.create(POOL_OPERATOR_ID, "Ana Depo",
                new Address("Sanayi Cd.", "İstanbul", "İstanbul", "34000", "TR"),
                new GeoCoordinates(41.0, 29.0));
        depot.clearDomainEvents();
    }

    @Nested
    @DisplayName("POST /api/logistics/depots")
    class CreateDepot {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("201 döndürmeli")
        void shouldReturn201() throws Exception {
            when(depotService.createDepot(anyLong(), any(), any(), any(), any(), any(), any(), anyDouble(), anyDouble()))
                    .thenReturn(depot);

            var body = new DepotController.CreateDepotRequest(
                    "Ana Depo", "Sanayi Cd.", "İstanbul", "İstanbul", "34000", "TR", 41.0, 29.0);

            mockMvc.perform(post("/api/logistics/depots")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/depots/{depotId}/vehicles/{vehicleId}")
    class AddVehicle {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(depotService.addVehicle(DEPOT_ID, VEHICLE_ID)).thenReturn(depot);

            mockMvc.perform(post("/api/logistics/depots/{depotId}/vehicles/{vehicleId}", DEPOT_ID, VEHICLE_ID))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/logistics/depots/{depotId}/vehicles/{vehicleId}")
    class RemoveVehicle {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(depotService.removeVehicle(DEPOT_ID, VEHICLE_ID)).thenReturn(depot);

            mockMvc.perform(delete("/api/logistics/depots/{depotId}/vehicles/{vehicleId}", DEPOT_ID, VEHICLE_ID))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/depots/{id}")
    class GetDepot {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200WhenFound() throws Exception {
            when(depotService.findById(DEPOT_ID)).thenReturn(depot);

            mockMvc.perform(get("/api/logistics/depots/{id}", DEPOT_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("404 döndürmeli")
        void shouldReturn400WhenNotFound() throws Exception {
            when(depotService.findById(999L)).thenThrow(new ResourceNotFoundException("not found"));

            mockMvc.perform(get("/api/logistics/depots/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/depots")
    class GetAllDepots {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(depotService.findByPoolOperator(anyLong(), any())).thenReturn(List.of(depot));

            mockMvc.perform(get("/api/logistics/depots")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}
