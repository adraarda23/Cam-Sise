package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.logistics.service.VehicleTypeService;
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
@DisplayName("VehicleTypeController Tests")
class VehicleTypeControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VehicleTypeService vehicleTypeService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final String AUTH_HEADER = "Bearer fake-token";

    private VehicleType vehicleType;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        lenient().when(jwtUtil.extractPoolOperatorId(any())).thenReturn(POOL_OPERATOR_ID);

        vehicleType = VehicleType.create(POOL_OPERATOR_ID, "Kamyon (12 ton)", "Büyük kamyon", new Capacity(120, 80));
        vehicleType.clearDomainEvents();
    }

    @Nested
    @DisplayName("POST /api/logistics/vehicle-types")
    class CreateVehicleType {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("201 döndürmeli")
        void shouldReturn201() throws Exception {
            when(vehicleTypeService.createVehicleType(anyLong(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(vehicleType);

            var body = new VehicleTypeController.CreateVehicleTypeRequest(
                    "Kamyon (12 ton)", "Büyük kamyon", 120, 80);

            mockMvc.perform(post("/api/logistics/vehicle-types")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PUT /api/logistics/vehicle-types/{id}/capacity")
    class UpdateCapacity {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(vehicleTypeService.updateCapacity(anyLong(), anyInt(), anyInt())).thenReturn(vehicleType);

            var body = new VehicleTypeController.UpdateCapacityRequest(150, 100);

            mockMvc.perform(put("/api/logistics/vehicle-types/1/capacity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/vehicle-types/{id}/deactivate")
    class DeactivateVehicleType {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(vehicleTypeService.deactivateVehicleType(1L)).thenReturn(vehicleType);

            mockMvc.perform(post("/api/logistics/vehicle-types/1/deactivate"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/vehicle-types/{id}")
    class GetVehicleType {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200WhenFound() throws Exception {
            when(vehicleTypeService.findById(1L)).thenReturn(vehicleType);

            mockMvc.perform(get("/api/logistics/vehicle-types/1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/vehicle-types")
    class GetAllVehicleTypes {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(vehicleTypeService.findByPoolOperator(anyLong(), any())).thenReturn(List.of(vehicleType));

            mockMvc.perform(get("/api/logistics/vehicle-types")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}
