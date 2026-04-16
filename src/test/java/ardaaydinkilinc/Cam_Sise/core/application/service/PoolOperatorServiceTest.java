package ardaaydinkilinc.Cam_Sise.core.service;

import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import ardaaydinkilinc.Cam_Sise.core.repository.PoolOperatorRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PoolOperatorService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PoolOperatorService Tests")
class PoolOperatorServiceTest {

    @Mock
    private PoolOperatorRepository poolOperatorRepository;

    @InjectMocks
    private PoolOperatorService poolOperatorService;

    private String companyName;
    private String taxIdValue;
    private String phone;
    private String email;
    private String contactPersonName;

    @BeforeEach
    void setUp() {
        companyName = "Test Company";
        taxIdValue = "1234567890";
        phone = "05551234567";
        email = "test@example.com";
        contactPersonName = "John Doe";
    }

    @Test
    @DisplayName("Should register new pool operator successfully")
    void shouldRegisterNewPoolOperator() {
        // Given
        when(poolOperatorRepository.existsByTaxId_Value(taxIdValue)).thenReturn(false);
        when(poolOperatorRepository.save(any(PoolOperator.class)))
                .thenAnswer(invocation -> {
                    PoolOperator po = invocation.getArgument(0);
                    // Simulate setting ID after save
                    return po;
                });

        // When
        PoolOperator result = poolOperatorService.registerPoolOperator(
                companyName, taxIdValue, phone, email, contactPersonName
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompanyName()).isEqualTo(companyName);
        assertThat(result.getTaxId().value()).isEqualTo(taxIdValue);
        assertThat(result.getContactInfo().phone()).isEqualTo(phone);
        assertThat(result.getContactInfo().email()).isEqualTo(email);
        assertThat(result.getActive()).isTrue();

        verify(poolOperatorRepository).existsByTaxId_Value(taxIdValue);
        verify(poolOperatorRepository).save(any(PoolOperator.class));
    }

    @Test
    @DisplayName("Should throw exception when tax ID already exists")
    void shouldThrowExceptionWhenTaxIdExists() {
        // Given
        when(poolOperatorRepository.existsByTaxId_Value(taxIdValue)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> poolOperatorService.registerPoolOperator(
                companyName, taxIdValue, phone, email, contactPersonName
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tax ID already exists");

        verify(poolOperatorRepository).existsByTaxId_Value(taxIdValue);
        verify(poolOperatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should activate pool operator successfully")
    void shouldActivatePoolOperator() {
        // Given
        Long poolOperatorId = 1L;
        PoolOperator poolOperator = PoolOperator.register(
                companyName,
                new TaxId(taxIdValue),
                new ContactInfo(phone, email, contactPersonName)
        );
        poolOperator.deactivate();

        when(poolOperatorRepository.findById(poolOperatorId)).thenReturn(Optional.of(poolOperator));
        when(poolOperatorRepository.save(any(PoolOperator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PoolOperator result = poolOperatorService.activatePoolOperator(poolOperatorId);

        // Then
        assertThat(result.getActive()).isTrue();
        verify(poolOperatorRepository).findById(poolOperatorId);
        verify(poolOperatorRepository).save(poolOperator);
    }

    @Test
    @DisplayName("Should deactivate pool operator successfully")
    void shouldDeactivatePoolOperator() {
        // Given
        Long poolOperatorId = 1L;
        PoolOperator poolOperator = PoolOperator.register(
                companyName,
                new TaxId(taxIdValue),
                new ContactInfo(phone, email, contactPersonName)
        );

        when(poolOperatorRepository.findById(poolOperatorId)).thenReturn(Optional.of(poolOperator));
        when(poolOperatorRepository.save(any(PoolOperator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PoolOperator result = poolOperatorService.deactivatePoolOperator(poolOperatorId);

        // Then
        assertThat(result.getActive()).isFalse();
        verify(poolOperatorRepository).findById(poolOperatorId);
        verify(poolOperatorRepository).save(poolOperator);
    }

    @Test
    @DisplayName("Should update contact info successfully")
    void shouldUpdateContactInfo() {
        // Given
        Long poolOperatorId = 1L;
        PoolOperator poolOperator = PoolOperator.register(
                companyName,
                new TaxId(taxIdValue),
                new ContactInfo(phone, email, contactPersonName)
        );

        String newPhone = "05559999999";
        String newEmail = "new@example.com";
        String newContactPersonName = "Jane Smith";

        when(poolOperatorRepository.findById(poolOperatorId)).thenReturn(Optional.of(poolOperator));
        when(poolOperatorRepository.save(any(PoolOperator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PoolOperator result = poolOperatorService.updateContactInfo(
                poolOperatorId, newPhone, newEmail, newContactPersonName
        );

        // Then
        assertThat(result.getContactInfo().phone()).isEqualTo(newPhone);
        assertThat(result.getContactInfo().email()).isEqualTo(newEmail);
        assertThat(result.getContactInfo().contactPersonName()).isEqualTo(newContactPersonName);
        verify(poolOperatorRepository).findById(poolOperatorId);
        verify(poolOperatorRepository).save(poolOperator);
    }

    @Test
    @DisplayName("Should throw exception when pool operator not found")
    void shouldThrowExceptionWhenPoolOperatorNotFound() {
        // Given
        Long poolOperatorId = 999L;
        when(poolOperatorRepository.findById(poolOperatorId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> poolOperatorService.findById(poolOperatorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pool operator not found");

        verify(poolOperatorRepository).findById(poolOperatorId);
    }

    @Test
    @DisplayName("Should find pool operator by ID successfully")
    void shouldFindPoolOperatorById() {
        // Given
        Long poolOperatorId = 1L;
        PoolOperator poolOperator = PoolOperator.register(
                companyName,
                new TaxId(taxIdValue),
                new ContactInfo(phone, email, contactPersonName)
        );

        when(poolOperatorRepository.findById(poolOperatorId)).thenReturn(Optional.of(poolOperator));

        // When
        PoolOperator result = poolOperatorService.findById(poolOperatorId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompanyName()).isEqualTo(companyName);
        verify(poolOperatorRepository).findById(poolOperatorId);
    }
}
