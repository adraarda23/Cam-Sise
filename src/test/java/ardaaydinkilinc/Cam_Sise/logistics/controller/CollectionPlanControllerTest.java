package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionPlanService;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Distance;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Duration;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CollectionPlanController Tests")
class CollectionPlanControllerTest {

    @Autowired
    private WebApplicationContext wac;

    private ObjectMapper objectMapper;

    @MockitoBean
    private CollectionPlanService collectionPlanService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long DEPOT_ID = 10L;
    private static final Long PLAN_ID = 20L;
    private static final Long VEHICLE_ID = 5L;
    private static final String AUTH_HEADER = "Bearer fake-token";

    private CollectionPlan plan;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        lenient().when(jwtUtil.extractPoolOperatorId(any())).thenReturn(POOL_OPERATOR_ID);

        plan = CollectionPlan.generate(DEPOT_ID, new Distance(150), new Duration(90),
                300, 200, LocalDate.now().plusDays(1), "[]");
        plan.clearDomainEvents();
    }

    @Nested
    @DisplayName("POST /api/logistics/collection-plans")
    class GeneratePlan {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("201 döndürmeli")
        void shouldReturn201() throws Exception {
            when(collectionPlanService.generatePlan(anyLong(), anyDouble(), anyInt(), anyInt(), anyInt(), any(), any()))
                    .thenReturn(plan);

            var body = new CollectionPlanController.GeneratePlanRequest(
                    DEPOT_ID, 150.0, 90, 300, 200, LocalDate.now().plusDays(1), "[]");

            mockMvc.perform(post("/api/logistics/collection-plans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("GENERATED"));
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/collection-plans/{planId}/assign-vehicle")
    class AssignVehicle {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            plan.assignVehicle(VEHICLE_ID);
            plan.clearDomainEvents();
            when(collectionPlanService.assignVehicle(anyLong(), anyLong())).thenReturn(plan);

            var body = new CollectionPlanController.AssignVehicleRequest(VEHICLE_ID);

            mockMvc.perform(post("/api/logistics/collection-plans/{planId}/assign-vehicle", PLAN_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ASSIGNED"));
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/collection-plans/{planId}/start")
    class StartCollection {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            plan.assignVehicle(VEHICLE_ID);
            plan.start();
            plan.clearDomainEvents();
            when(collectionPlanService.startCollection(PLAN_ID)).thenReturn(plan);

            mockMvc.perform(post("/api/logistics/collection-plans/{planId}/start", PLAN_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/collection-plans/{planId}/complete")
    class CompleteCollection {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            plan.assignVehicle(VEHICLE_ID);
            plan.start();
            plan.complete(280, 190);
            plan.clearDomainEvents();
            when(collectionPlanService.completeCollection(anyLong(), anyInt(), anyInt())).thenReturn(plan);

            var body = new CollectionPlanController.CompleteCollectionRequest(280, 190);

            mockMvc.perform(post("/api/logistics/collection-plans/{planId}/complete", PLAN_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/collection-plans/{planId}/cancel")
    class CancelPlan {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            plan.cancel();
            plan.clearDomainEvents();
            when(collectionPlanService.cancelPlan(PLAN_ID)).thenReturn(plan);

            mockMvc.perform(post("/api/logistics/collection-plans/{planId}/cancel", PLAN_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/collection-plans/{id}")
    class GetPlan {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200WhenFound() throws Exception {
            when(collectionPlanService.findById(PLAN_ID)).thenReturn(plan);

            mockMvc.perform(get("/api/logistics/collection-plans/{id}", PLAN_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("GENERATED"));
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/collection-plans")
    class GetAllPlans {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            var pageResponse = new PageResponse<>(List.of(plan), 1L, 1, 0, 20);
            when(collectionPlanService.findByPoolOperatorIdPaged(anyLong(), any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/logistics/collection-plans")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/collection-plans/depot/{depotId}")
    class GetPlansByDepot {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(collectionPlanService.findByDepot(anyLong(), any())).thenReturn(List.of(plan));

            mockMvc.perform(get("/api/logistics/collection-plans/depot/{depotId}", DEPOT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}
