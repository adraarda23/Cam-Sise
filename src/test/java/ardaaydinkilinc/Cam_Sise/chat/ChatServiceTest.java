package ardaaydinkilinc.Cam_Sise.chat;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionRequestService;
import ardaaydinkilinc.Cam_Sise.settings.domain.CompanySettings;
import ardaaydinkilinc.Cam_Sise.settings.service.CompanySettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Tests")
class ChatServiceTest {

    @Mock private GeminiService geminiService;
    @Mock private UserRepository userRepository;
    @Mock private FillerRepository fillerRepository;
    @Mock private FillerStockRepository fillerStockRepository;
    @Mock private CollectionRequestRepository collectionRequestRepository;
    @Mock private CollectionRequestService collectionRequestService;
    @Mock private CollectionPlanRepository collectionPlanRepository;
    @Mock private CompanySettingsService companySettingsService;

    private ChatService service;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long FILLER_ID = 5L;
    private static final Long USER_ID = 10L;

    private CompanySettings settings;

    @BeforeEach
    void setUp() {
        service = new ChatService(geminiService, userRepository, fillerRepository,
                fillerStockRepository, collectionRequestRepository, collectionRequestService,
                collectionPlanRepository, companySettingsService, new ObjectMapper());

        settings = new CompanySettings(POOL_OPERATOR_ID);
        lenient().when(companySettingsService.getSettings(any())).thenReturn(settings);
        lenient().when(geminiService.chat(any(), any(), any())).thenReturn("Merhaba!");
    }

    @Nested
    @DisplayName("chat()")
    class Chat {

        @Test
        @DisplayName("kullanıcı bulunamazsa temel sistem promptu kullanılmalı")
        void usesBasicPromptWhenUserNotFound() {
            when(userRepository.findByUsernameAndActiveTrue("bilinmeyen")).thenReturn(Optional.empty());

            String result = service.chat("bilinmeyen", POOL_OPERATOR_ID, "Merhaba", List.of());

            assertThat(result).isEqualTo("Merhaba!");
            verify(geminiService).chat(eq("Merhaba"), any(), any());
        }

        @Test
        @DisplayName("kullanıcı COMPANY_STAFF ise sistem promptu 'operasyon personeli' içermeli")
        void usesStaffPromptForCompanyStaff() {
            User staffUser = User.register(POOL_OPERATOR_ID, "staff01", "hashed", "Ali Veli", Role.COMPANY_STAFF, null);
            when(userRepository.findByUsernameAndActiveTrue("staff01")).thenReturn(Optional.of(staffUser));
            when(collectionRequestRepository.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(collectionPlanRepository.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(fillerStockRepository.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());

            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            service.chat("staff01", POOL_OPERATOR_ID, "Durum ne?", List.of());

            verify(geminiService).chat(eq("Durum ne?"), any(), promptCaptor.capture());
            assertThat(promptCaptor.getValue())
                    .contains("COMPANY_STAFF")
                    .doesNotContain("ACTION_JSON");
        }

        @Test
        @DisplayName("kullanıcı CUSTOMER ama fillerId null ise temel prompt (ACTION_JSON yok) kullanılmalı")
        void usesBasicPromptForCustomerWithNullFillerId() {
            User customerNoFiller = User.register(POOL_OPERATOR_ID, "customer01", "hashed", "Müşteri", Role.CUSTOMER, null);
            when(userRepository.findByUsernameAndActiveTrue("customer01")).thenReturn(Optional.of(customerNoFiller));

            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            service.chat("customer01", POOL_OPERATOR_ID, "Merhaba", List.of());

            verify(geminiService).chat(eq("Merhaba"), any(), promptCaptor.capture());
            assertThat(promptCaptor.getValue()).doesNotContain("ACTION_JSON");
        }

        @Test
        @DisplayName("kullanıcı CUSTOMER ve fillerId var ise contextual prompt dolumcu bilgisi içermeli")
        void usesContextualPromptForCustomerWithFillerId() {
            User customerUser = User.register(POOL_OPERATOR_ID, "customer02", "hashed", "Müşteri", Role.CUSTOMER, FILLER_ID);
            when(userRepository.findByUsernameAndActiveTrue("customer02")).thenReturn(Optional.of(customerUser));
            when(fillerRepository.findById(FILLER_ID)).thenReturn(Optional.empty());
            when(fillerStockRepository.findByFillerId(FILLER_ID)).thenReturn(List.of());
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(any(), any(), any())).thenReturn(List.of());

            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            service.chat("customer02", POOL_OPERATOR_ID, "Stok ne kadar?", List.of());

            verify(geminiService).chat(eq("Stok ne kadar?"), any(), promptCaptor.capture());
            assertThat(promptCaptor.getValue())
                    .contains("ACTION_JSON")           // müşteri promptunda eylem kuralları var
                    .contains("Dolumcu ID: " + FILLER_ID);
        }
    }

    @Nested
    @DisplayName("parseAndExecute (ACTION_MARKER üzerinden)")
    class ParseAndExecute {

        @Test
        @DisplayName("ACTION_MARKER yoksa yanıt direkt döndürülmeli")
        void returnsResponseDirectlyWhenNoActionMarker() {
            User customerUser = User.register(POOL_OPERATOR_ID, "cu", "hashed", "Müşteri", Role.CUSTOMER, FILLER_ID);
            when(userRepository.findByUsernameAndActiveTrue("cu")).thenReturn(Optional.of(customerUser));
            when(fillerRepository.findById(FILLER_ID)).thenReturn(Optional.empty());
            when(fillerStockRepository.findByFillerId(FILLER_ID)).thenReturn(List.of());
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(any(), any(), any())).thenReturn(List.of());
            when(geminiService.chat(any(), any(), any())).thenReturn("Sadece bilgi mesajı.");

            String result = service.chat("cu", POOL_OPERATOR_ID, "Soru?", List.of());

            assertThat(result).isEqualTo("Sadece bilgi mesajı.");
            verify(collectionRequestService, never()).createManual(any(), any(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("CREATE_REQUEST action ile toplama talebi oluşturulmalı")
        void createsCollectionRequestOnAction() {
            User customerUser = User.register(POOL_OPERATOR_ID, "cu2", "hashed", "Müşteri", Role.CUSTOMER, FILLER_ID);
            when(userRepository.findByUsernameAndActiveTrue("cu2")).thenReturn(Optional.of(customerUser));
            when(fillerRepository.findById(FILLER_ID)).thenReturn(Optional.empty());
            when(fillerStockRepository.findByFillerId(FILLER_ID)).thenReturn(List.of());
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(any(), any(), any())).thenReturn(List.of());

            CollectionRequest createdReq = CollectionRequest.createManual(FILLER_ID, AssetType.PALLET, 50, USER_ID);
            when(collectionRequestService.createManual(any(), any(), anyInt(), any(), any())).thenReturn(createdReq);
            when(geminiService.chat(any(), any(), any()))
                    .thenReturn("Talebiniz alındı.\nACTION_JSON:{\"type\":\"CREATE_REQUEST\",\"assetType\":\"PALLET\",\"quantity\":50}");

            String result = service.chat("cu2", POOL_OPERATOR_ID, "50 palet istiyorum", List.of());

            verify(collectionRequestService).createManual(eq(FILLER_ID), eq(AssetType.PALLET), eq(50), any(), eq(POOL_OPERATOR_ID));
            assertThat(result).contains("toplama talebi oluşturuldu");
        }

        @Test
        @DisplayName("CREATE_REQUEST BusinessRuleViolationException → hata mesajı döndürmeli")
        void returnsErrorMessageOnBusinessRuleViolation() {
            User customerUser = User.register(POOL_OPERATOR_ID, "cu3", "hashed", "Müşteri", Role.CUSTOMER, FILLER_ID);
            when(userRepository.findByUsernameAndActiveTrue("cu3")).thenReturn(Optional.of(customerUser));
            when(fillerRepository.findById(FILLER_ID)).thenReturn(Optional.empty());
            when(fillerStockRepository.findByFillerId(FILLER_ID)).thenReturn(List.of());
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(any(), any(), any())).thenReturn(List.of());

            when(collectionRequestService.createManual(any(), any(), anyInt(), any(), any()))
                    .thenThrow(new ardaaydinkilinc.Cam_Sise.shared.exception.BusinessRuleViolationException("Stok yetersiz"));
            when(geminiService.chat(any(), any(), any()))
                    .thenReturn("ACTION_JSON:{\"type\":\"CREATE_REQUEST\",\"assetType\":\"PALLET\",\"quantity\":50}");

            String result = service.chat("cu3", POOL_OPERATOR_ID, "50 palet", List.of());

            assertThat(result).contains("oluşturulamadı");
        }

        @Test
        @DisplayName("geçersiz JSON içeren ACTION_MARKER loglanmalı ve exception fırlatılmamalı")
        void handlesInvalidActionJsonGracefully() {
            User customerUser = User.register(POOL_OPERATOR_ID, "cu4", "hashed", "Müşteri", Role.CUSTOMER, FILLER_ID);
            when(userRepository.findByUsernameAndActiveTrue("cu4")).thenReturn(Optional.of(customerUser));
            when(fillerRepository.findById(FILLER_ID)).thenReturn(Optional.empty());
            when(fillerStockRepository.findByFillerId(FILLER_ID)).thenReturn(List.of());
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(any(), any(), any())).thenReturn(List.of());
            when(geminiService.chat(any(), any(), any()))
                    .thenReturn("ACTION_JSON:geçersiz_json_bu");

            assertThatCode(() -> service.chat("cu4", POOL_OPERATOR_ID, "soru", List.of()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("buildWelcomeMessage()")
    class BuildWelcomeMessage {

        @Test
        @DisplayName("kullanıcı bulunamazsa dolumcu bilgisi olmayan generic mesaj döndürmeli")
        void returnsGenericMessageWhenUserNotFound() {
            when(userRepository.findByUsernameAndActiveTrue("bilinmeyen")).thenReturn(Optional.empty());

            String result = service.buildWelcomeMessage("bilinmeyen", POOL_OPERATOR_ID);

            assertThat(result).contains("yardımcı olabilirim");
            assertThat(result).doesNotContain("Sistem Özeti", "Mevcut Durumunuz");
        }

        @Test
        @DisplayName("kullanıcı COMPANY_STAFF ise adı ve sistem özeti içeren mesaj döndürmeli")
        void returnsStaffWelcomeForCompanyStaff() {
            User staff = User.register(POOL_OPERATOR_ID, "staff01", "hashed", "Ali Veli", Role.COMPANY_STAFF, null);
            when(userRepository.findByUsernameAndActiveTrue("staff01")).thenReturn(Optional.of(staff));
            when(collectionRequestRepository.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(collectionPlanRepository.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(fillerStockRepository.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());

            String result = service.buildWelcomeMessage("staff01", POOL_OPERATOR_ID);

            assertThat(result)
                    .contains("Ali Veli")
                    .contains("Sistem Özeti")
                    .contains("Bekleyen talep");
        }

        @Test
        @DisplayName("kullanıcı CUSTOMER ama fillerId null ise stok içermeyen generic mesaj döndürmeli")
        void returnsGenericMessageForCustomerWithNullFillerId() {
            User customerNoFiller = User.register(POOL_OPERATOR_ID, "cu_nofiller", "hashed", "Müşteri", Role.CUSTOMER, null);
            when(userRepository.findByUsernameAndActiveTrue("cu_nofiller")).thenReturn(Optional.of(customerNoFiller));

            String result = service.buildWelcomeMessage("cu_nofiller", POOL_OPERATOR_ID);

            assertThat(result).contains("yardımcı olabilirim");
            assertThat(result).doesNotContain("Palet", "Ayırıcı");
        }

        @Test
        @DisplayName("kullanıcı CUSTOMER ve fillerId var ise stok bilgisi ve minimum miktarlar içermeli")
        void returnsContextualWelcomeForCustomerWithFillerId() {
            User customerUser = User.register(POOL_OPERATOR_ID, "cu_filler", "hashed", "Müşteri", Role.CUSTOMER, FILLER_ID);
            when(userRepository.findByUsernameAndActiveTrue("cu_filler")).thenReturn(Optional.of(customerUser));
            when(fillerRepository.findById(FILLER_ID)).thenReturn(Optional.empty());
            when(fillerStockRepository.findByFillerId(FILLER_ID)).thenReturn(List.of());
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(any(), any(), any())).thenReturn(List.of());

            String result = service.buildWelcomeMessage("cu_filler", POOL_OPERATOR_ID);

            assertThat(result)
                    .contains("Mevcut Durumunuz")
                    .contains("Palet")
                    .contains("Ayırıcı")
                    .contains("min.");
        }
    }
}
