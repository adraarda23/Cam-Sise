package ardaaydinkilinc.Cam_Sise.settings.service;

import ardaaydinkilinc.Cam_Sise.settings.domain.CompanySettings;
import ardaaydinkilinc.Cam_Sise.settings.repository.CompanySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanySettingsService {

    private final CompanySettingsRepository repository;

    @Transactional(readOnly = true)
    public CompanySettings getSettings(Long poolOperatorId) {
        return repository.findById(poolOperatorId)
                .orElseGet(() -> repository.save(new CompanySettings(poolOperatorId)));
    }

    public CompanySettings updateSettings(Long poolOperatorId, int minPalletRequestQty, int minSeparatorRequestQty) {
        CompanySettings settings = getSettings(poolOperatorId);
        settings.setMinPalletRequestQty(minPalletRequestQty);
        settings.setMinSeparatorRequestQty(minSeparatorRequestQty);
        return repository.save(settings);
    }
}
