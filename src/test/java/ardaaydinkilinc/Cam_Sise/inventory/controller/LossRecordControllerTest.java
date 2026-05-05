package ardaaydinkilinc.Cam_Sise.inventory.controller;

import ardaaydinkilinc.Cam_Sise.inventory.domain.LossRecord;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.Period;
import ardaaydinkilinc.Cam_Sise.inventory.service.LossRecordService;
import ardaaydinkilinc.Cam_Sise.shared.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("LossRecordController Tests")
class LossRecordControllerTest {

    @Autowired
    private WebApplicationContext wac;

    private ObjectMapper objectMapper;

    @MockitoBean
    private LossRecordService lossRecordService;

    private MockMvc mockMvc;

    private static final Long FILLER_ID = 1L;

    private LossRecord lossRecord;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Period period = new Period(LocalDate.now().minusDays(30), LocalDate.now());
        lossRecord = LossRecord.createWithEstimate(FILLER_ID, AssetType.PALLET, new LossRate(5.0), period);
        lossRecord.clearDomainEvents();
    }

    @Nested
    @DisplayName("POST /api/inventory/loss-records")
    class CreateLossRecord {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("201 döndürmeli")
        void shouldReturn201() throws Exception {
            when(lossRecordService.createWithEstimate(anyLong(), any(), anyDouble(), any())).thenReturn(lossRecord);

            var body = new LossRecordController.CreateLossRecordRequest(
                    FILLER_ID, AssetType.PALLET, 5.0,
                    LocalDate.now().minusDays(30), LocalDate.now());

            mockMvc.perform(post("/api/inventory/loss-records")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PUT /api/inventory/loss-records/{fillerId}/{assetType}/actual-rate")
    class UpdateActualRate {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(lossRecordService.updateActualRate(anyLong(), any(), anyDouble())).thenReturn(lossRecord);

            var body = new LossRecordController.UpdateActualRateRequest(4.5);

            mockMvc.perform(put("/api/inventory/loss-records/{fillerId}/{assetType}/actual-rate", FILLER_ID, "PALLET")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("404 döndürmeli (bulunamadı)")
        void shouldReturn404WhenNotFound() throws Exception {
            when(lossRecordService.updateActualRate(anyLong(), any(), anyDouble()))
                    .thenThrow(new ResourceNotFoundException("not found"));

            var body = new LossRecordController.UpdateActualRateRequest(4.5);

            mockMvc.perform(put("/api/inventory/loss-records/999/PALLET/actual-rate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/inventory/loss-records/{fillerId}/{assetType}/estimated-rate")
    class RecalculateEstimatedRate {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(lossRecordService.recalculateEstimatedRate(anyLong(), any(), anyDouble(), any())).thenReturn(lossRecord);

            var body = new LossRecordController.RecalculateEstimatedRateRequest(
                    6.0, LocalDate.now().minusDays(30), LocalDate.now());

            mockMvc.perform(put("/api/inventory/loss-records/{fillerId}/{assetType}/estimated-rate", FILLER_ID, "PALLET")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/inventory/loss-records/{fillerId}/{assetType}")
    class GetLossRecord {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200WhenFound() throws Exception {
            when(lossRecordService.getLossRecord(anyLong(), any())).thenReturn(lossRecord);

            mockMvc.perform(get("/api/inventory/loss-records/{fillerId}/{assetType}", FILLER_ID, "PALLET"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/inventory/loss-records/filler/{fillerId}")
    class GetLossRecordsByFiller {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(lossRecordService.getLossRecordsByFiller(anyLong())).thenReturn(List.of(lossRecord));

            mockMvc.perform(get("/api/inventory/loss-records/filler/{fillerId}", FILLER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/inventory/loss-records/asset-type/{assetType}")
    class GetLossRecordsByAssetType {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("200 döndürmeli")
        void shouldReturn200() throws Exception {
            when(lossRecordService.getLossRecordsByAssetType(any())).thenReturn(List.of(lossRecord));

            mockMvc.perform(get("/api/inventory/loss-records/asset-type/PALLET"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}
