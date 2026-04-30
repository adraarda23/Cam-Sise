package ardaaydinkilinc.Cam_Sise.settings.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.settings.domain.CompanySettings;
import ardaaydinkilinc.Cam_Sise.settings.repository.CompanySettingsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanySettingsService Tests")
class CompanySettingsServiceTest {

    @Mock
    private CompanySettingsRepository repository;

    @InjectMocks
    private CompanySettingsService service;

    private static final Long POOL_OPERATOR_ID = 1L;

    @Nested
    @DisplayName("getSettings")
    class GetSettings {

        @Test
        @DisplayName("returns existing settings when found")
        void returnsExistingSettings() {
            CompanySettings existing = new CompanySettings(POOL_OPERATOR_ID);
            existing.setMinPalletRequestQty(30);
            existing.setMinSeparatorRequestQty(15);

            when(repository.findById(POOL_OPERATOR_ID)).thenReturn(Optional.of(existing));

            CompanySettings result = service.getSettings(POOL_OPERATOR_ID);

            assertThat(result.getMinPalletRequestQty()).isEqualTo(30);
            assertThat(result.getMinSeparatorRequestQty()).isEqualTo(15);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("creates and saves default settings when not found")
        void createsDefaultSettingsWhenNotFound() {
            when(repository.findById(POOL_OPERATOR_ID)).thenReturn(Optional.empty());
            when(repository.save(any(CompanySettings.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CompanySettings result = service.getSettings(POOL_OPERATOR_ID);

            assertThat(result).isNotNull();
            assertThat(result.getMinPalletRequestQty()).isEqualTo(20);
            assertThat(result.getMinSeparatorRequestQty()).isEqualTo(10);
            verify(repository).save(any(CompanySettings.class));
        }

        @Test
        @DisplayName("getMinQty returns correct value for PALLET")
        void returnsCorrectMinForPallet() {
            CompanySettings s = new CompanySettings(POOL_OPERATOR_ID);
            s.setMinPalletRequestQty(25);
            when(repository.findById(POOL_OPERATOR_ID)).thenReturn(Optional.of(s));

            CompanySettings result = service.getSettings(POOL_OPERATOR_ID);

            assertThat(result.getMinQty(AssetType.PALLET)).isEqualTo(25);
        }

        @Test
        @DisplayName("getMinQty returns correct value for SEPARATOR")
        void returnsCorrectMinForSeparator() {
            CompanySettings s = new CompanySettings(POOL_OPERATOR_ID);
            s.setMinSeparatorRequestQty(12);
            when(repository.findById(POOL_OPERATOR_ID)).thenReturn(Optional.of(s));

            CompanySettings result = service.getSettings(POOL_OPERATOR_ID);

            assertThat(result.getMinQty(AssetType.SEPARATOR)).isEqualTo(12);
        }
    }

    @Nested
    @DisplayName("updateSettings")
    class UpdateSettings {

        @Test
        @DisplayName("updates both minimum quantities and saves")
        void updatesBothQuantities() {
            CompanySettings existing = new CompanySettings(POOL_OPERATOR_ID);
            when(repository.findById(POOL_OPERATOR_ID)).thenReturn(Optional.of(existing));
            when(repository.save(any(CompanySettings.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CompanySettings result = service.updateSettings(POOL_OPERATOR_ID, 50, 25);

            assertThat(result.getMinPalletRequestQty()).isEqualTo(50);
            assertThat(result.getMinSeparatorRequestQty()).isEqualTo(25);
            verify(repository).save(existing);
        }

        @Test
        @DisplayName("creates default then updates when settings do not exist")
        void createsAndUpdates() {
            when(repository.findById(POOL_OPERATOR_ID)).thenReturn(Optional.empty());
            when(repository.save(any(CompanySettings.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CompanySettings result = service.updateSettings(POOL_OPERATOR_ID, 100, 50);

            assertThat(result.getMinPalletRequestQty()).isEqualTo(100);
            assertThat(result.getMinSeparatorRequestQty()).isEqualTo(50);
            // save called twice: once for auto-create, once for update
            verify(repository, times(2)).save(any(CompanySettings.class));
        }
    }
}
