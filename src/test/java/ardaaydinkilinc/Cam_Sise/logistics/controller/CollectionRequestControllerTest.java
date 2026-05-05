package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionRequestService;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import ardaaydinkilinc.Cam_Sise.shared.exception.BusinessRuleViolationException;
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
@DisplayName("CollectionRequestController Tests")
class CollectionRequestControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CollectionRequestService collectionRequestService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long FILLER_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final String AUTH_HEADER = "Bearer fake-token";

    private CollectionRequest request;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        lenient().when(jwtUtil.extractPoolOperatorId(any())).thenReturn(POOL_OPERATOR_ID);

        request = CollectionRequest.createManual(FILLER_ID, AssetType.PALLET, 100, USER_ID);
    }

    @Nested
    @DisplayName("POST /api/logistics/collection-requests/manual")
    class CreateManual {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("201 döndürmeli")
        void shouldReturn201() throws Exception {
            when(collectionRequestService.createManual(anyLong(), any(), anyInt(), anyLong(), anyLong()))
                    .thenReturn(request);

            var body = new CollectionRequestController.CreateManualRequestRequest(
                    FILLER_ID, AssetType.PALLET, 100, USER_ID);

            mockMvc.perform(post("/api/logistics/collection-requests/manual")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estimatedQuantity").value(100));
        }

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("400 döndürmeli (minimum miktar altında)")
        void shouldReturn400WhenBelowMinimum() throws Exception {
            when(collectionRequestService.createManual(anyLong(), any(), anyInt(), anyLong(), anyLong()))
                    .thenThrow(new BusinessRuleViolationException("Minimum toplama talebi"));

            var body = new CollectionRequestController.CreateManualRequestRequest(
                    FILLER_ID, AssetType.PALLET, 5, USER_ID);

            mockMvc.perform(post("/api/logistics/collection-requests/manual")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/collection-requests/{requestId}/approve")
    class ApproveRequest {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            request.approve(USER_ID);
            when(collectionRequestService.approve(anyLong(), anyLong())).thenReturn(request);

            var body = new CollectionRequestController.ApproveRequestRequest(USER_ID);

            mockMvc.perform(post("/api/logistics/collection-requests/1/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/collection-requests/{requestId}/reject")
    class RejectRequest {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            request.reject("Yetersiz stok");
            when(collectionRequestService.reject(anyLong(), any())).thenReturn(request);

            var body = new CollectionRequestController.RejectRequestRequest("Yetersiz stok");

            mockMvc.perform(post("/api/logistics/collection-requests/1/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }
    }

    @Nested
    @DisplayName("POST /api/logistics/collection-requests/{requestId}/cancel")
    class CancelRequest {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            request.cancel();
            when(collectionRequestService.cancel(anyLong())).thenReturn(request);

            mockMvc.perform(post("/api/logistics/collection-requests/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/collection-requests/{id}")
    class GetRequest {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200WhenFound() throws Exception {
            when(collectionRequestService.findById(1L)).thenReturn(request);

            mockMvc.perform(get("/api/logistics/collection-requests/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("400 döndürmeli (bulunamadı)")
        void shouldReturn400WhenNotFound() throws Exception {
            when(collectionRequestService.findById(999L)).thenThrow(new IllegalArgumentException("not found"));

            mockMvc.perform(get("/api/logistics/collection-requests/999"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/collection-requests")
    class GetAllRequests {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            var pageResponse = new PageResponse<>(List.of(request), 1L, 1, 0, 20);
            when(collectionRequestService.findByPoolOperatorIdPaged(anyLong(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/logistics/collection-requests")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/logistics/collection-requests/filler/{fillerId}")
    class GetRequestsByFiller {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(collectionRequestService.findByFiller(anyLong(), any())).thenReturn(List.of(request));

            mockMvc.perform(get("/api/logistics/collection-requests/filler/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}
