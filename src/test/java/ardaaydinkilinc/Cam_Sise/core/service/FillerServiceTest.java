package ardaaydinkilinc.Cam_Sise.core.service;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FillerService Tests")
class FillerServiceTest {

    @Mock private FillerRepository fillerRepository;

    @InjectMocks
    private FillerService service;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long FILLER_ID = 10L;

    private Filler activeFiller;

    @BeforeEach
    void setUp() {
        activeFiller = Filler.register(
                POOL_OPERATOR_ID, "Test Dolumcu",
                new Address("Fabrika Sk.", "İzmir", "İzmir", "35000", "TR"),
                new GeoCoordinates(38.4, 27.1),
                new ContactInfo("05559999999", "test@test.com", "Mehmet Bey"),
                null);
        activeFiller.clearDomainEvents();
    }

    @Nested
    @DisplayName("registerFiller")
    class RegisterFiller {

        @Test
        @DisplayName("saves and returns filler when no duplicate tax ID")
        void registersFiller() {
            when(fillerRepository.existsByTaxId_Value("1234567890")).thenReturn(false);
            when(fillerRepository.save(any(Filler.class))).thenAnswer(inv -> inv.getArgument(0));

            Filler result = service.registerFiller(
                    POOL_OPERATOR_ID, "Yeni Dolumcu",
                    "Cadde 1", "Bursa", "Bursa", "16000", "TR",
                    40.2, 29.1,
                    "05551111111", "yeni@test.com", "Ahmet Bey",
                    "1234567890");

            assertThat(result.getName()).isEqualTo("Yeni Dolumcu");
            assertThat(result.getActive()).isTrue();
            verify(fillerRepository).save(any(Filler.class));
        }

        @Test
        @DisplayName("throws when tax ID already exists")
        void throwsOnDuplicateTaxId() {
            when(fillerRepository.existsByTaxId_Value("9999999999")).thenReturn(true);

            assertThatThrownBy(() -> service.registerFiller(
                    POOL_OPERATOR_ID, "Dolumcu",
                    "Sk. 1", "Ankara", "Ankara", "06000", "TR",
                    39.9, 32.8,
                    "05552222222", "dup@test.com", "Veli Bey",
                    "9999999999"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tax ID already exists");

            verify(fillerRepository, never()).save(any());
        }

        @Test
        @DisplayName("registers filler without tax ID (null)")
        void registersWithoutTaxId() {
            when(fillerRepository.save(any(Filler.class))).thenAnswer(inv -> inv.getArgument(0));

            Filler result = service.registerFiller(
                    POOL_OPERATOR_ID, "Dolumcu B",
                    "Sk. 2", "Konya", "Konya", "42000", "TR",
                    37.9, 32.5,
                    "05553333333", "b@test.com", "Ali Bey",
                    null);

            assertThat(result).isNotNull();
            verify(fillerRepository, never()).existsByTaxId_Value(any());
        }
    }

    @Nested
    @DisplayName("activateFiller")
    class ActivateFiller {

        @Test
        @DisplayName("activates filler and saves")
        void activatesFiller() {
            activeFiller.deactivate();
            when(fillerRepository.findById(FILLER_ID)).thenReturn(Optional.of(activeFiller));
            when(fillerRepository.save(activeFiller)).thenReturn(activeFiller);

            Filler result = service.activateFiller(FILLER_ID);

            assertThat(result.getActive()).isTrue();
            verify(fillerRepository).save(activeFiller);
        }

        @Test
        @DisplayName("throws when filler not found")
        void throwsWhenNotFound() {
            when(fillerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activateFiller(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deactivateFiller")
    class DeactivateFiller {

        @Test
        @DisplayName("deactivates filler and saves")
        void deactivatesFiller() {
            when(fillerRepository.findById(FILLER_ID)).thenReturn(Optional.of(activeFiller));
            when(fillerRepository.save(activeFiller)).thenReturn(activeFiller);

            Filler result = service.deactivateFiller(FILLER_ID);

            assertThat(result.getActive()).isFalse();
        }

        @Test
        @DisplayName("throws when filler not found")
        void throwsWhenNotFound() {
            when(fillerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivateFiller(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("updateFiller")
    class UpdateFiller {

        @Test
        @DisplayName("updates name, address, contact and location")
        void updatesAllFields() {
            when(fillerRepository.findById(FILLER_ID)).thenReturn(Optional.of(activeFiller));
            when(fillerRepository.save(activeFiller)).thenReturn(activeFiller);

            Filler result = service.updateFiller(
                    FILLER_ID, "Güncellenmiş Dolumcu",
                    "Yeni Sk.", "Mersin", "Mersin", "33000", "TR",
                    36.8, 34.6,
                    "05557777777", "updated@test.com", "Yeni Kişi");

            assertThat(result.getName()).isEqualTo("Güncellenmiş Dolumcu");
            verify(fillerRepository).save(activeFiller);
        }

        @Test
        @DisplayName("throws when filler not found")
        void throwsWhenNotFound() {
            when(fillerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateFiller(
                    999L, "X", "X", "X", "X", "X", "X", 0.0, 0.0, "X", "X", "X"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns filler when found")
        void returnsFiller() {
            when(fillerRepository.findById(FILLER_ID)).thenReturn(Optional.of(activeFiller));

            Filler result = service.findById(FILLER_ID);

            assertThat(result).isEqualTo(activeFiller);
        }

        @Test
        @DisplayName("throws when filler not found")
        void throwsWhenNotFound() {
            when(fillerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("findByPoolOperator")
    class FindByPoolOperator {

        @Test
        @DisplayName("returns all fillers when active filter is null")
        void returnsAllFillers() {
            when(fillerRepository.findByPoolOperatorId(POOL_OPERATOR_ID))
                    .thenReturn(List.of(activeFiller));

            List<Filler> result = service.findByPoolOperator(POOL_OPERATOR_ID, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("filters by active=true")
        void filtersActiveFillers() {
            when(fillerRepository.findByPoolOperatorIdAndActive(POOL_OPERATOR_ID, true))
                    .thenReturn(List.of(activeFiller));

            List<Filler> result = service.findByPoolOperator(POOL_OPERATOR_ID, true);

            assertThat(result).hasSize(1);
        }
    }
}
