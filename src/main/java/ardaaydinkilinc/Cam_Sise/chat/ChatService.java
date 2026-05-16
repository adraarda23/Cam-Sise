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
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.settings.domain.CompanySettings;
import ardaaydinkilinc.Cam_Sise.settings.service.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final GeminiService geminiService;
    private final UserRepository userRepository;
    private final FillerRepository fillerRepository;
    private final FillerStockRepository fillerStockRepository;
    private final CollectionRequestRepository collectionRequestRepository;
    private final CollectionPlanRepository collectionPlanRepository;
    private final CompanySettingsService companySettingsService;

    public String chat(String username, Long poolOperatorId, String message, List<ChatRequest.MessagePair> history) {
        User user = userRepository.findByUsernameAndActiveTrue(username).orElse(null);

        if (user != null && user.getRole() == Role.COMPANY_STAFF) {
            String systemPrompt = buildStaffSystemPrompt(poolOperatorId);
            // Entity-grounding: mesajda bir dolumcu adı geçiyorsa o dolumcunun
            // gerçek stok + aktif talep verisini prompt'a enjekte et ki
            // "Coca-Cola stok durumu" gibi sorular cevaplanabilsin.
            String fillerContext = resolveFillerContext(poolOperatorId, message);
            if (!fillerContext.isBlank()) {
                systemPrompt = systemPrompt + "\n\n" + fillerContext;
            }
            return geminiService.chat(message, history, systemPrompt);
        }

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

        // Salt-okunur Q&A. Talep oluşturma gibi aksiyonlar chat'teki deterministik
        // hızlı-aksiyon butonlarıyla yapılır — LLM asla aksiyon yürütmez.
        String systemPrompt = buildContextualSystemPrompt(fillerName, fillerId, stocks, settings, activeRequests, activeSepRequests);
        return geminiService.chat(message, history, systemPrompt);
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
                Sen Cam-Sise Palet Yönetim Sistemi'nin müşteri (dolumcu) asistanısın.
                Bu sistem dolumculara ait palet ve ayırıcıları toplayan bir havuz operatörü firmasına aittir.
                Türkçe yanıt ver. Kısa, net ve yardımsever ol.

                KULLANICI: %s (Dolumcu ID: %d)

                MEVCUT STOK:
                - Palet: %d adet (eşik: %d, aktif taleplerde: %d, kullanılabilir: %d)
                - Ayırıcı: %d adet (eşik: %d, aktif taleplerde: %d, kullanılabilir: %d)

                MİNİMUM TALEP MİKTARLARI:
                - Palet: minimum %d adet
                - Ayırıcı: minimum %d adet

                AKTİF TALEPLER:
                %s

                SİSTEM HAKKINDA (kullanıcıya nasıl yapılacağını anlatabilirsin):
                - Toplama talebi oluşturma: Sohbet penceresindeki "📦 Talep Oluştur" butonu ile
                  veya "Taleplerim" sayfasından yapılır. Sen sohbet üzerinden talep OLUŞTURAMAZSIN;
                  kullanıcıyı bu butona yönlendir.
                - Stok ve eşik: "Panel" (dashboard) ekranında görünür; eşiği müşteri kendisi
                  "Eşik Güncelle" ile değiştirebilir. Stok eşiğin altına düşünce sistem otomatik
                  talep oluşturur.
                - Yaklaşan toplamalar: Panel'deki "Size Yaklaşan Toplamalar" kartında plan tarihi
                  ve gelecek araç bilgisi görünür.
                - Talep durumları: PENDING (onay bekliyor), APPROVED (onaylandı),
                  SCHEDULED (planlandı), COMPLETED (toplandı), REJECTED (reddedildi).

                KURALLAR:
                - Yukarıdaki gerçek verileri kullanarak soruları yanıtla.
                - Talep oluşturma isteğinde: kullanıcıyı "📦 Talep Oluştur" butonuna yönlendir,
                  istersen minimum/kullanılabilir miktara göre tavsiye ver, ama işlemi sen yapma.
                - Bilmediğin/veride olmayan şeyi uydurma; "Bu bilgi panelde mevcut" gibi yönlendir.
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
        if (user == null) {
            return "Merhaba! Size nasıl yardımcı olabilirim?";
        }
        if (user.getRole() == Role.COMPANY_STAFF) {
            return buildStaffWelcomeMessage(user.getFullName(), poolOperatorId);
        }
        if (user.getFillerId() == null) {
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

    /**
     * Entity-grounding: staff mesajında geçen dolumcu adını çözüp o dolumcunun
     * gerçek stok + aktif talep verisini bir prompt bloğu olarak döndürür.
     * Eşleşme yoksa boş string. İsim/mesaj normalize edilir ("Coca-Cola" ↔ "coca cola").
     */
    private String resolveFillerContext(Long poolOperatorId, String message) {
        if (message == null || message.isBlank()) return "";
        java.util.Set<String> msgTokens = new java.util.HashSet<>(
                java.util.Arrays.asList(normalizeForMatch(message).split(" ")));
        msgTokens.remove("");

        List<Filler> fillers = fillerRepository.findByPoolOperatorId(poolOperatorId);
        Filler matched = null;
        int bestScore = 0;
        for (Filler f : fillers) {
            if (f.getName() == null || f.getName().isBlank()) continue;

            // Dolumcunun ayırt edici kelimeleri (generic/dolgu kelimeler atılır)
            List<String> nameTokens = significantTokens(f.getName());
            if (nameTokens.isEmpty()) continue;

            // Tüm ayırt edici kelimeler mesajda geçiyorsa eşleşir (sırasız).
            boolean allPresent = nameTokens.stream().allMatch(msgTokens::contains);
            if (allPresent) {
                // En çok kelime eşleşen / en spesifik isim kazanır
                int score = nameTokens.size();
                if (score > bestScore) {
                    matched = f;
                    bestScore = score;
                }
            }
        }
        if (matched == null) return "";

        Long fid = matched.getId();
        List<FillerStock> stocks = fillerStockRepository.findByFillerId(fid);
        Optional<FillerStock> pallet = stocks.stream().filter(s -> s.getAssetType() == AssetType.PALLET).findFirst();
        Optional<FillerStock> sep = stocks.stream().filter(s -> s.getAssetType() == AssetType.SEPARATOR).findFirst();

        List<CollectionRequest> activeReqs = collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                fid, AssetType.PALLET, List.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.SCHEDULED));
        List<CollectionRequest> activeSep = collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                fid, AssetType.SEPARATOR, List.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.SCHEDULED));

        StringBuilder reqStr = new StringBuilder();
        if (activeReqs.isEmpty() && activeSep.isEmpty()) {
            reqStr.append("Aktif talep yok.");
        } else {
            activeReqs.forEach(r -> reqStr.append("- Palet talebi #").append(r.getId())
                    .append(": ").append(r.getEstimatedQuantity()).append(" adet, ").append(r.getStatus()).append("\n"));
            activeSep.forEach(r -> reqStr.append("- Ayırıcı talebi #").append(r.getId())
                    .append(": ").append(r.getEstimatedQuantity()).append(" adet, ").append(r.getStatus()).append("\n"));
        }

        return """
                SORULAN DOLUMCU: %s (ID: %d)
                - Palet stok: %d adet (eşik: %d)
                - Ayırıcı stok: %d adet (eşik: %d)
                Aktif talepleri:
                %s

                Kullanıcı bu dolumcu hakkında soru sorduysa YUKARIDAKİ gerçek verileri
                kullanarak yanıtla. "Stoklar sayfasına gidin" gibi yönlendirme YAPMA.
                """.formatted(
                matched.getName(), fid,
                pallet.map(FillerStock::getCurrentQuantity).orElse(0),
                pallet.map(FillerStock::getThresholdQuantity).orElse(0),
                sep.map(FillerStock::getCurrentQuantity).orElse(0),
                sep.map(FillerStock::getThresholdQuantity).orElse(0),
                reqStr.toString().trim()
        );
    }

    /** Eşleştirme için: küçük harf + harf/rakam dışını tek boşluğa indirge. */
    private String normalizeForMatch(String s) {
        return s.toLowerCase(java.util.Locale.forLanguageTag("tr"))
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    // Şirket türü / dolgu kelimeleri — eşleştirmede ayırt edici sayılmaz.
    private static final java.util.Set<String> GENERIC_TOKENS = java.util.Set.of(
            "dolumcu", "a", "as", "aş", "ş", "ltd", "şti", "sti", "san", "tic",
            "sanayi", "ticaret", "anonim", "limited", "şirketi", "sirketi", "ve");

    /** Dolumcu adının ayırt edici kelimeleri (generic ve tek-harf token'lar atılır). */
    private List<String> significantTokens(String name) {
        List<String> out = new java.util.ArrayList<>();
        for (String t : normalizeForMatch(name).split(" ")) {
            if (t.isBlank()) continue;
            boolean numeric = t.chars().allMatch(Character::isDigit);
            if (numeric || (t.length() >= 2 && !GENERIC_TOKENS.contains(t))) {
                out.add(t);
            }
        }
        return out;
    }

    private String buildStaffWelcomeMessage(String fullName, Long poolOperatorId) {
        List<CollectionRequest> allRequests = collectionRequestRepository.findByPoolOperatorId(poolOperatorId);
        List<CollectionPlan> allPlans = collectionPlanRepository.findByPoolOperatorId(poolOperatorId);
        List<FillerStock> allStocks = fillerStockRepository.findByPoolOperatorId(poolOperatorId);

        long pending = allRequests.stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count();
        long approved = allRequests.stream().filter(r -> r.getStatus() == RequestStatus.APPROVED).count();
        long activePlans = allPlans.stream()
                .filter(p -> p.getStatus() == PlanStatus.ASSIGNED || p.getStatus() == PlanStatus.IN_PROGRESS).count();
        long lowStockFillers = allStocks.stream()
                .filter(s -> s.getCurrentQuantity() >= s.getThresholdQuantity()).count();
        long totalFillers = allStocks.stream().map(FillerStock::getFillerId).distinct().count();

        return """
                Merhaba, **%s**! 👋

                **Sistem Özeti:**
                - 📋 Bekleyen talep: %d | Onaylı: %d
                - 🚛 Aktif plan: %d
                - ⚠️ Eşik aşan dolumcu: %d / %d

                **Yapabilecekleriniz:**
                - Talep, plan ve stok durumu sorgulama
                - Belirli bir dolumcu hakkında bilgi alma
                - Sistem istatistikleri ve özet bilgiler

                Nasıl yardımcı olabilirim?
                """.formatted(fullName, pending, approved, activePlans, lowStockFillers, totalFillers);
    }

    private String buildStaffSystemPrompt(Long poolOperatorId) {
        List<CollectionRequest> allRequests = collectionRequestRepository.findByPoolOperatorId(poolOperatorId);
        List<CollectionPlan> allPlans = collectionPlanRepository.findByPoolOperatorId(poolOperatorId);
        List<FillerStock> allStocks = fillerStockRepository.findByPoolOperatorId(poolOperatorId);

        long pending = allRequests.stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count();
        long approved = allRequests.stream().filter(r -> r.getStatus() == RequestStatus.APPROVED).count();
        long scheduled = allRequests.stream().filter(r -> r.getStatus() == RequestStatus.SCHEDULED).count();
        long completed = allRequests.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count();
        long activePlans = allPlans.stream()
                .filter(p -> p.getStatus() == PlanStatus.ASSIGNED || p.getStatus() == PlanStatus.IN_PROGRESS).count();
        long completedPlans = allPlans.stream().filter(p -> p.getStatus() == PlanStatus.COMPLETED).count();
        long totalPalletStock = allStocks.stream().filter(s -> s.getAssetType() == AssetType.PALLET)
                .mapToLong(FillerStock::getCurrentQuantity).sum();
        long totalSepStock = allStocks.stream().filter(s -> s.getAssetType() == AssetType.SEPARATOR)
                .mapToLong(FillerStock::getCurrentQuantity).sum();
        long lowStockFillers = allStocks.stream()
                .filter(s -> s.getCurrentQuantity() >= s.getThresholdQuantity()).count();
        long totalFillers = allStocks.stream().map(FillerStock::getFillerId).distinct().count();

        return """
                Sen Cam-Sise Palet Yönetim Sistemi'nin operasyon asistanısın.
                Şu an COMPANY_STAFF (operasyon personeli) ile konuşuyorsun.
                Türkçe yanıt ver. Kısa ve net ol.

                SİSTEM DURUMU:
                - Talepler: %d beklemede, %d onaylı, %d planlandı, %d tamamlandı
                - Planlar: %d aktif, %d tamamlandı
                - Toplam stok: %d palet, %d ayırıcı
                - Eşik aşan dolumcu: %d / %d

                SİSTEM HAKKINDA (personeli nereye yönlendireceğini bil):
                - Talepleri onaylama/reddetme: "Talepler" sayfası (sol menü → Operasyon).
                - Rota optimizasyonu: "Rota Optimizasyonu" sayfası — onaylı talepler için
                  CVRP ile filo önerisi + rota oluşturur (Clarke-Wright + 2-opt + OSRM yol mesafesi).
                - Oluşan planlar/harita: "Toplama Planları" — her planın rota haritası,
                  gidiş/dönüş polyline'ı, araç atama.
                - Stok/eşik: "Stoklar" sayfası; anomali tespiti z-score ile yapılır,
                  kritik durumda staff'a e-posta + uygulama içi bildirim gider.
                - Dolumcu/araç/müşteri yönetimi: ilgili sol menü sayfaları.

                KURALLAR:
                - Personel sistemi yönetir; sen chat üzerinden talep/plan OLUŞTURMAZSIN,
                  yalnızca bilgi verir ve doğru sayfaya yönlendirirsin.
                - Yukarıdaki gerçek verileri kullan; veride olmayanı uydurma.
                """.formatted(pending, approved, scheduled, completed,
                activePlans, completedPlans,
                totalPalletStock, totalSepStock,
                lowStockFillers, totalFillers);
    }

    private String buildBasicSystemPrompt() {
        return """
                Sen Cam-Sise Palet Yönetim Sistemi'nin müşteri hizmetleri asistanısın.
                Bu sistem cam fabrikalarına ve dolumculara palet ve ayırıcı kiralayan bir havuz operatörüne aittir.
                Türkçe yanıt ver. Kısa ve net ol.
                """;
    }
}
