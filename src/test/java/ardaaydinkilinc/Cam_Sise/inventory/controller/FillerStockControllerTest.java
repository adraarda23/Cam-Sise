package ardaaydinkilinc.Cam_Sise.inventory.controller;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.service.FillerStockService;
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
@DisplayName("FillerStockController Tests")
class FillerStockControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FillerStockService fillerStockService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long FILLER_ID = 1L;
    private static final String AUTH_HEADER = "Bearer fake-token";

    private FillerStock palletStock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        lenient().when(jwtUtil.extractPoolOperatorId(any())).thenReturn(POOL_OPERATOR_ID);

        palletStock = FillerStock.initialize(FILLER_ID, AssetType.PALLET, 50, new LossRate(5.0));
        palletStock.clearDomainEvents();
    }

    @Nested
    @DisplayName("POST /api/inventory/stocks/inflow")
    class RecordInflow {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            palletStock.recordInflow(100, "REF-001");
            palletStock.clearDomainEvents();
            when(fillerStockService.recordInflow(anyLong(), any(), anyInt(), any())).thenReturn(palletStock);

            var body = new FillerStockController.RecordInflowRequest(FILLER_ID, AssetType.PALLET, 100, "REF-001");

            mockMvc.perform(post("/api/inventory/stocks/inflow")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/inventory/stocks/collection")
    class RecordCollection {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(fillerStockService.recordCollection(anyLong(), any(), anyInt(), any())).thenReturn(palletStock);

            var body = new FillerStockController.RecordCollectionRequest(FILLER_ID, AssetType.PALLET, 50, "PLAN-001");

            mockMvc.perform(post("/api/inventory/stocks/collection")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/inventory/stocks/{fillerId}/{assetType}/threshold")
    class UpdateThreshold {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(fillerStockService.updateThreshold(anyLong(), any(), anyInt())).thenReturn(palletStock);

            var body = new FillerStockController.UpdateThresholdRequest(100);

            mockMvc.perform(put("/api/inventory/stocks/{fillerId}/{assetType}/threshold", FILLER_ID, "PALLET")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/inventory/stocks/{fillerId}/{assetType}/loss-rate")
    class UpdateLossRate {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(fillerStockService.updateLossRate(anyLong(), any(), anyDouble())).thenReturn(palletStock);

            var body = new FillerStockController.UpdateLossRateRequest(7.5);

            mockMvc.perform(put("/api/inventory/stocks/{fillerId}/{assetType}/loss-rate", FILLER_ID, "PALLET")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/inventory/stocks/{fillerId}/{assetType}")
    class GetStock {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200WhenFound() throws Exception {
            when(fillerStockService.getStock(anyLong(), any())).thenReturn(palletStock);

            mockMvc.perform(get("/api/inventory/stocks/{fillerId}/{assetType}", FILLER_ID, "PALLET"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("404 döndürmeli (bulunamadı)")
        void shouldReturn404WhenNotFound() throws Exception {
            when(fillerStockService.getStock(anyLong(), any())).thenThrow(new ResourceNotFoundException("not found"));

            mockMvc.perform(get("/api/inventory/stocks/999/PALLET"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/inventory/stocks/filler/{fillerId}")
    class GetStocksByFiller {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(fillerStockService.getStocksByFiller(anyLong())).thenReturn(List.of(palletStock));

            mockMvc.perform(get("/api/inventory/stocks/filler/{fillerId}", FILLER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/inventory/stocks")
    class GetAllStocks {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            var pageResponse = new PageResponse<>(List.of(palletStock), 1L, 1, 0, 20);
            when(fillerStockService.findByPoolOperatorIdPaged(anyLong(), any(), anyInt(), anyInt()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/inventory/stocks")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
