package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.logistics.service.VehicleService;
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
@DisplayName("VehicleController Tests")
class VehicleControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long DEPOT_ID = 10L;
    private static final String AUTH_HEADER = "Bearer fake-token";

    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        lenient().when(jwtUtil.extractPoolOperatorId(any())).thenReturn(POOL_OPERATOR_ID);

        vehicle = Vehicle.register(DEPOT_ID, 1L, "34ABC001");
        vehicle.clearDomainEvents();
    }

    @Nested
    @DisplayName("POST /api/logistics/vehicles")
    class RegisterVehicle {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("201 döndürmeli")
        void shouldReturn201() throws Exception {
            when(vehicleService.registerVehicle(anyLong(), anyLong(), any())).thenReturn(vehicle);

            var body = new VehicleController.RegisterVehicleRequest(DEPOT_ID, 1L, "34ABC001");

            mockMvc.perform(post("/api/logistics/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("400 döndürmeli (plaka zaten var)")
        void shouldReturn400OnDuplicatePlate() throws Exception {
            when(vehicleService.registerVehicle(anyLong(), anyLong(), any()))
                    .thenThrow(new IllegalArgumentException("plate already exists"));

            var body = new VehicleController.RegisterVehicleRequest(DEPOT_ID, 1L, "34DUP001");

            mockMvc.perform(post("/api/logistics/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/vehicles/{vehicleId}/assign")
    class AssignToRoute {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(vehicleService.assignToRoute(anyLong(), anyLong(), any(), any(), any())).thenReturn(vehicle);

            var body = new VehicleController.AssignToRouteRequest(1L, "Mehmet Yılmaz", "12345678901", "05551234567");

            mockMvc.perform(post("/api/logistics/vehicles/1/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/vehicles/{vehicleId}/return")
    class ReturnToDepot {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(vehicleService.returnToDepot(1L)).thenReturn(vehicle);

            mockMvc.perform(post("/api/logistics/vehicles/1/return"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/logistics/vehicles/{vehicleId}/status")
    class ChangeStatus {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(vehicleService.changeStatus(anyLong(), any())).thenReturn(vehicle);

            var body = new VehicleController.ChangeStatusRequest(VehicleStatus.MAINTENANCE);

            mockMvc.perform(put("/api/logistics/vehicles/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/vehicles/{id}")
    class GetVehicle {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200WhenFound() throws Exception {
            when(vehicleService.findById(1L)).thenReturn(vehicle);

            mockMvc.perform(get("/api/logistics/vehicles/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("404 döndürmeli")
        void shouldReturn400WhenNotFound() throws Exception {
            when(vehicleService.findById(999L)).thenThrow(new ResourceNotFoundException("not found"));

            mockMvc.perform(get("/api/logistics/vehicles/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/vehicles")
    class GetAllVehicles {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            var pageResponse = new PageResponse<>(List.of(vehicle), 1L, 1, 0, 20);
            when(vehicleService.findByPoolOperatorIdPaged(anyLong(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/logistics/vehicles")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
