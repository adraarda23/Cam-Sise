package ardaaydinkilinc.Cam_Sise.chat;

import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionRequestService;
import ardaaydinkilinc.Cam_Sise.settings.domain.CompanySettings;
import ardaaydinkilinc.Cam_Sise.settings.service.CompanySettingsService;
import ardaaydinkilinc.Cam_Sise.shared.exception.BusinessRuleViolationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private static final String ACTION_MARKER = "ACTION_JSON:";

    private final GeminiService geminiService;
    private final UserRepository userRepository;
    private final FillerRepository fillerRepository;
    private final FillerStockRepository fillerStockRepository;
    private final CollectionRequestRepository collectionRequestRepository;
    private final CollectionRequestService collectionRequestService;
    private final CompanySettingsService companySettingsService;
    private final ObjectMapper objectMapper;

    public String chat(String username, Long poolOperatorId, String message, List<ChatRequest.MessagePair> history) {
        User user = userRepository.findByUsernameAndActiveTrue(username).orElse(null);

        if (user == null || user.getFillerId() == null) {
            return geminiService.chat(message, history, buildBasicSystemPrompt());
        }

        Long fillerId = user.getFillerId();
        String fillerName = fillerRepository.findById(fillerId).map(Filler::getName).orElse("Bilinmiyor");
        List<FillerStock> stocks = fillerStockRepository.findByFillerId(fillerId);
        CompanySettings settings = companySettingsService.getSettings(poolOperatorId);
        List<CollectionRequest> activeRequests = collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                fillerId, AssetType.PALLET, List.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.SCHEDULED));
        List<CollectionRequest> activeSepRequests = collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                fillerId, AssetType.SEPARATOR, List.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.SCHEDULED));

        String systemPrompt = buildContextualSystemPrompt(fillerName, fillerId, stocks, settings, activeRequests, activeSepRequests);
        String geminiResponse = geminiService.chat(message, history, systemPrompt);

        return parseAndExecute(geminiResponse, fillerId, user.getId(), poolOperatorId);
    }

    private String parseAndExecute(String response, Long fillerId, Long userId, Long poolOperatorId) {
        if (!response.contains(ACTION_MARKER)) {
            return response;
        }

        // Extract explanation text (everything before the first ACTION_JSON)
        String explanationText = response.substring(0, response.indexOf(ACTION_MARKER)).trim();
        StringBuilder results = new StringBuilder();
        if (!explanationText.isBlank()) {
            results.append(explanationText).append("\n\n");
        }

        // Find and execute ALL ACTION_JSON entries
        int searchFrom = 0;
        boolean anyAction = false;
        while (response.indexOf(ACTION_MARKER, searchFrom) != -1) {
            int markerPos = response.indexOf(ACTION_MARKER, searchFrom);
            int jsonStart = response.indexOf("{", markerPos + ACTION_MARKER.length());
            int jsonEnd = response.indexOf("}", jsonStart) + 1;
            if (jsonStart == -1 || jsonEnd == 0) break;

            String json = response.substring(jsonStart, jsonEnd).trim();
            searchFrom = jsonEnd;
            anyAction = true;

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> action = objectMapper.readValue(json, Map.class);
                String type = (String) action.get("type");

                if ("CREATE_REQUEST".equals(type)) {
                    String assetTypeStr = (String) action.get("assetType");
                    int quantity = ((Number) action.get("quantity")).intValue();
                    AssetType assetType = AssetType.valueOf(assetTypeStr);
                    String typeLabel = assetType == AssetType.PALLET ? "palet" : "ayırıcı";

                    try {
                        CollectionRequest created = collectionRequestService.createManual(
                                fillerId, assetType, quantity, userId, poolOperatorId);
                        results.append("✅ ").append(quantity).append(" adet ").append(typeLabel)
                               .append(" için toplama talebi oluşturuldu. Talep numaranız: #").append(created.getId()).append("\n");
                    } catch (BusinessRuleViolationException e) {
                        results.append("❌ ").append(quantity).append(" adet ").append(typeLabel)
                               .append(" talebi oluşturulamadı: ").append(e.getMessage()).append("\n");
                    } catch (Exception e) {
                        log.error("Failed to create collection request from chat", e);
                        results.append("❌ ").append(typeLabel).append(" talebi oluşturulurken hata oluştu.\n");
                    }
                }
            } catch (Exception e) {
                log.warn("Could not parse action JSON: {}", json, e);
            }
        }

        return anyAction ? results.toString().trim() : response.replaceAll("ACTION_JSON:\\{[^}]*}", "").trim();
    }

    private String buildContextualSystemPrompt(
            String fillerName, Long fillerId,
            List<FillerStock> stocks,
            CompanySettings settings,
            List<CollectionRequest> activePalletRequests,
            List<CollectionRequest> activeSepRequests) {

        Optional<FillerStock> palletStock = stocks.stream().filter(s -> s.getAssetType() == AssetType.PALLET).findFirst();
        Optional<FillerStock> sepStock = stocks.stream().filter(s -> s.getAssetType() == AssetType.SEPARATOR).findFirst();

        int palletCurrent = palletStock.map(FillerStock::getCurrentQuantity).orElse(0);
        int palletThreshold = palletStock.map(FillerStock::getThresholdQuantity).orElse(0);
        int sepCurrent = sepStock.map(FillerStock::getCurrentQuantity).orElse(0);
        int sepThreshold = sepStock.map(FillerStock::getThresholdQuantity).orElse(0);

        int palletInActive = activePalletRequests.stream().mapToInt(CollectionRequest::getEstimatedQuantity).sum();
        int sepInActive = activeSepRequests.stream().mapToInt(CollectionRequest::getEstimatedQuantity).sum();
        int availablePallet = palletCurrent - palletInActive;
        int availableSep = sepCurrent - sepInActive;

        StringBuilder activeReqStr = new StringBuilder();
        if (activePalletRequests.isEmpty() && activeSepRequests.isEmpty()) {
            activeReqStr.append("Aktif talep yok.");
        } else {
            activePalletRequests.forEach(r -> activeReqStr.append("- Palet talebi #").append(r.getId())
                    .append(": ").append(r.getEstimatedQuantity()).append(" adet, durum: ").append(r.getStatus()).append("\n"));
            activeSepRequests.forEach(r -> activeReqStr.append("- Ayırıcı talebi #").append(r.getId())
                    .append(": ").append(r.getEstimatedQuantity()).append(" adet, durum: ").append(r.getStatus()).append("\n"));
        }

        return """
                Sen Cam-Sise Palet Yönetim Sistemi'nin müşteri hizmetleri asistanısın.
                Bu sistem dolumculara ait palet ve ayırıcıları toplayan bir havuz operatörü firmasına aittir.
                Türkçe yanıt ver. Kısa ve net ol.

                KULLANICI: %s (Dolumcu ID: %d)

                MEVCUT STOK:
                - Palet: %d adet (eşik: %d, aktif taleplerde: %d, kullanılabilir: %d)
                - Ayırıcı: %d adet (eşik: %d, aktif taleplerde: %d, kullanılabilir: %d)

                MİNİMUM TALEP MİKTARLARI:
                - Palet: minimum %d adet
                - Ayırıcı: minimum %d adet

                AKTİF TALEPLER:
                %s

                EYLEM KURALLARI:
                Kullanıcı toplama talebi oluşturmak istediğinde şu kontrolleri yap:
                1. Miktar minimumun altındaysa reddet ve neden açıkla.
                2. Miktar kullanılabilir stoktan fazlaysa reddet ve neden açıkla.
                3. Her iki koşul da sağlanıyorsa yanıtının SONUNA şu formatı ekle (açıklamadan sonra yeni satırda):
                   ACTION_JSON:{"type":"CREATE_REQUEST","assetType":"PALLET","quantity":50}
                   (PALLET yerine SEPARATOR da olabilir, quantity gerçek miktar)
                4. Birden fazla talep varsa (örn. hem palet hem ayırıcı) her biri için ayrı ACTION_JSON satırı ekle, hepsi işlenir:

                Kullanıcı talep durumu, stok bilgisi gibi sorular sorarsa yukarıdaki verileri kullanarak yanıtla, ACTION_JSON ekleme.
                """.formatted(
                fillerName, fillerId,
                palletCurrent, palletThreshold, palletInActive, availablePallet,
                sepCurrent, sepThreshold, sepInActive, availableSep,
                settings.getMinPalletRequestQty(), settings.getMinSeparatorRequestQty(),
                activeReqStr.toString()
        );
    }

    public String buildWelcomeMessage(String username, Long poolOperatorId) {
        User user = userRepository.findByUsernameAndActiveTrue(username).orElse(null);
        if (user == null || user.getFillerId() == null) {
            return "Merhaba! Size nasıl yardımcı olabilirim?";
        }

        Long fillerId = user.getFillerId();
        String fillerName = fillerRepository.findById(fillerId).map(Filler::getName).orElse("Dolumcu");
        List<FillerStock> stocks = fillerStockRepository.findByFillerId(fillerId);
        CompanySettings settings = companySettingsService.getSettings(poolOperatorId);

        Optional<FillerStock> palletStock = stocks.stream().filter(s -> s.getAssetType() == AssetType.PALLET).findFirst();
        Optional<FillerStock> sepStock = stocks.stream().filter(s -> s.getAssetType() == AssetType.SEPARATOR).findFirst();

        int palletCurrent = palletStock.map(FillerStock::getCurrentQuantity).orElse(0);
        int sepCurrent = sepStock.map(FillerStock::getCurrentQuantity).orElse(0);

        List<CollectionRequest> activePallet = collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                fillerId, AssetType.PALLET, List.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.SCHEDULED));
        List<CollectionRequest> activeSep = collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                fillerId, AssetType.SEPARATOR, List.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.SCHEDULED));

        int palletInActive = activePallet.stream().mapToInt(CollectionRequest::getEstimatedQuantity).sum();
        int sepInActive = activeSep.stream().mapToInt(CollectionRequest::getEstimatedQuantity).sum();

        return """
                Merhaba, **%s**! 👋

                **Mevcut Durumunuz:**
                - 🟦 Palet: %d adet (kullanılabilir: %d)
                - 🟨 Ayırıcı: %d adet (kullanılabilir: %d)

                **Yapabilecekleriniz:**
                - Toplama talebi oluşturma *(min. %d palet / %d ayırıcı)*
                - Stok ve talep durumu sorgulama
                - Aktif talepleriniz hakkında bilgi alma

                Nasıl yardımcı olabilirim?
                """.formatted(
                fillerName,
                palletCurrent, palletCurrent - palletInActive,
                sepCurrent, sepCurrent - sepInActive,
                settings.getMinPalletRequestQty(), settings.getMinSeparatorRequestQty()
        );
    }

    private String buildBasicSystemPrompt() {
        return """
                Sen Cam-Sise Palet Yönetim Sistemi'nin müşteri hizmetleri asistanısın.
                Bu sistem cam fabrikalarına ve dolumculara palet ve ayırıcı kiralayan bir havuz operatörüne aittir.
                Türkçe yanıt ver. Kısa ve net ol.
                """;
    }
}
