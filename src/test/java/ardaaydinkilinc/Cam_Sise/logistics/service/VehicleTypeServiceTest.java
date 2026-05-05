package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleTypeRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleTypeService Tests")
class VehicleTypeServiceTest {

    @Mock private VehicleTypeRepository vehicleTypeRepository;

    @InjectMocks
    private VehicleTypeService service;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long VEHICLE_TYPE_ID = 10L;

    private VehicleType vehicleType;

    @BeforeEach
    void setUp() {
        vehicleType = VehicleType.create(POOL_OPERATOR_ID, "Kamyon (12 ton)", "Büyük kamyon",
                new Capacity(120, 80));
        vehicleType.clearDomainEvents();
    }

    @Nested
    @DisplayName("createVehicleType")
    class CreateVehicleType {

        @Test
        @DisplayName("yeni araç tipi oluşturmalı ve kaydetmeli")
        void createsAndSavesVehicleType() {
            when(vehicleTypeRepository.save(any(VehicleType.class))).thenAnswer(inv -> inv.getArgument(0));

            VehicleType result = service.createVehicleType(POOL_OPERATOR_ID, "Kamyon", "Açıklama", 120, 80);

            assertThat(result.getName()).isEqualTo("Kamyon");
            assertThat(result.getCapacity().pallets()).isEqualTo(120);
            verify(vehicleTypeRepository).save(any(VehicleType.class));
        }
    }

    @Nested
    @DisplayName("updateCapacity")
    class UpdateCapacity {

        @Test
        @DisplayName("kapasiteyi güncellemeli ve kaydetmeli")
        void updatesCapacity() {
            when(vehicleTypeRepository.findById(VEHICLE_TYPE_ID)).thenReturn(Optional.of(vehicleType));
            when(vehicleTypeRepository.save(vehicleType)).thenReturn(vehicleType);

            VehicleType result = service.updateCapacity(VEHICLE_TYPE_ID, 150, 100);

            assertThat(result.getCapacity().pallets()).isEqualTo(150);
            verify(vehicleTypeRepository).save(vehicleType);
        }

        @Test
        @DisplayName("araç tipi bulunamazsa exception fırlatmalı")
        void throwsWhenNotFound() {
            when(vehicleTypeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCapacity(999L, 150, 100))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deactivateVehicleType")
    class DeactivateVehicleType {

        @Test
        @DisplayName("araç tipini devre dışı bırakmalı")
        void deactivatesVehicleType() {
            when(vehicleTypeRepository.findById(VEHICLE_TYPE_ID)).thenReturn(Optional.of(vehicleType));
            when(vehicleTypeRepository.save(vehicleType)).thenReturn(vehicleType);

            VehicleType result = service.deactivateVehicleType(VEHICLE_TYPE_ID);

            assertThat(result.getActive()).isFalse();
            verify(vehicleTypeRepository).save(vehicleType);
        }

        @Test
        @DisplayName("araç tipi bulunamazsa exception fırlatmalı")
        void throwsWhenNotFound() {
            when(vehicleTypeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivateVehicleType(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("araç tipini döndürmeli")
        void returnsVehicleType() {
            when(vehicleTypeRepository.findById(VEHICLE_TYPE_ID)).thenReturn(Optional.of(vehicleType));

            VehicleType result = service.findById(VEHICLE_TYPE_ID);

            assertThat(result).isEqualTo(vehicleType);
        }

        @Test
        @DisplayName("bulunamazsa exception fırlatmalı")
        void throwsWhenNotFound() {
            when(vehicleTypeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findByPoolOperator")
    class FindByPoolOperator {

        @Test
        @DisplayName("active filtresiyle araç tiplerini döndürmeli")
        void returnsFilteredByActive() {
            when(vehicleTypeRepository.findByPoolOperatorIdAndActive(POOL_OPERATOR_ID, true))
                    .thenReturn(List.of(vehicleType));

            List<VehicleType> result = service.findByPoolOperator(POOL_OPERATOR_ID, true);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("filtre yoksa tüm araç tiplerini döndürmeli")
        void returnsAllWhenNoFilter() {
            when(vehicleTypeRepository.findByPoolOperatorId(POOL_OPERATOR_ID))
                    .thenReturn(List.of(vehicleType));

            List<VehicleType> result = service.findByPoolOperator(POOL_OPERATOR_ID, null);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("aktif araç tiplerini döndürmeli")
        void returnsActiveVehicleTypes() {
            when(vehicleTypeRepository.findByActive(true)).thenReturn(List.of(vehicleType));

            List<VehicleType> result = service.findAllActive();

            assertThat(result).hasSize(1);
            verify(vehicleTypeRepository).findByActive(true);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("tüm araç tiplerini döndürmeli")
        void returnsAll() {
            when(vehicleTypeRepository.findAll()).thenReturn(List.of(vehicleType));

            List<VehicleType> result = service.findAll();

            assertThat(result).hasSize(1);
        }
    }
}
