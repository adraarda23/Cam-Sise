package ardaaydinkilinc.Cam_Sise.core.domain;

import ardaaydinkilinc.Cam_Sise.core.domain.event.PoolOperatorActivated;
import ardaaydinkilinc.Cam_Sise.core.domain.event.PoolOperatorDeactivated;
import ardaaydinkilinc.Cam_Sise.core.domain.event.PoolOperatorRegistered;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for PoolOperator aggregate
 */
@DisplayName("PoolOperator Domain Tests")
class PoolOperatorTest {

    @Test
    @DisplayName("Should register new pool operator with valid data")
    void shouldRegisterNewPoolOperator() {
        // Given
        String companyName = "Test Pool Operator";
        TaxId taxId = new TaxId("1234567890");
        ContactInfo contactInfo = new ContactInfo("05551234567", "test@example.com", "John Doe");

        // When
        PoolOperator poolOperator = PoolOperator.register(companyName, taxId, contactInfo);

        // Then
        assertThat(poolOperator.getCompanyName()).isEqualTo(companyName);
        assertThat(poolOperator.getTaxId()).isEqualTo(taxId);
        assertThat(poolOperator.getContactInfo()).isEqualTo(contactInfo);
        assertThat(poolOperator.getActive()).isTrue();
        assertThat(poolOperator.getDomainEvents()).hasSize(1);
        assertThat(poolOperator.getDomainEvents().get(0)).isInstanceOf(PoolOperatorRegistered.class);
    }

    @Test
    @DisplayName("Should activate inactive pool operator")
    void shouldActivatePoolOperator() {
        // Given
        PoolOperator poolOperator = PoolOperator.register(
                "Test Company",
                new TaxId("1234567890"),
                new ContactInfo("05551234567", "test@example.com", "John Doe")
        );
        poolOperator.deactivate();
        poolOperator.clearDomainEvents();

        // When
        poolOperator.activate();

        // Then
        assertThat(poolOperator.getActive()).isTrue();
        assertThat(poolOperator.getDomainEvents()).hasSize(1);
        assertThat(poolOperator.getDomainEvents().get(0)).isInstanceOf(PoolOperatorActivated.class);
    }

    @Test
    @DisplayName("Should deactivate active pool operator")
    void shouldDeactivatePoolOperator() {
        // Given
        PoolOperator poolOperator = PoolOperator.register(
                "Test Company",
                new TaxId("1234567890"),
                new ContactInfo("05551234567", "test@example.com", "John Doe")
        );
        poolOperator.clearDomainEvents();

        // When
        poolOperator.deactivate();

        // Then
        assertThat(poolOperator.getActive()).isFalse();
        assertThat(poolOperator.getDomainEvents()).hasSize(1);
        assertThat(poolOperator.getDomainEvents().get(0)).isInstanceOf(PoolOperatorDeactivated.class);
    }

    @Test
    @DisplayName("Should update contact information")
    void shouldUpdateContactInfo() {
        // Given
        PoolOperator poolOperator = PoolOperator.register(
                "Test Company",
                new TaxId("1234567890"),
                new ContactInfo("05551234567", "test@example.com", "John Doe")
        );

        ContactInfo newContactInfo = new ContactInfo("05559999999", "new@example.com", "Jane Smith");

        // When
        poolOperator.updateContactInfo(newContactInfo);

        // Then
        assertThat(poolOperator.getContactInfo()).isEqualTo(newContactInfo);
        assertThat(poolOperator.getContactInfo().phone()).isEqualTo("05559999999");
        assertThat(poolOperator.getContactInfo().email()).isEqualTo("new@example.com");
        assertThat(poolOperator.getContactInfo().contactPersonName()).isEqualTo("Jane Smith");
    }

    @Test
    @DisplayName("Should throw exception when activating already active pool operator")
    void shouldThrowExceptionWhenActivatingAlreadyActivePoolOperator() {
        // Given
        PoolOperator poolOperator = PoolOperator.register(
                "Test Company",
                new TaxId("1234567890"),
                new ContactInfo("05551234567", "test@example.com", "John Doe")
        );

        // When/Then
        assertThatThrownBy(poolOperator::activate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already active");
    }

    @Test
    @DisplayName("Should throw exception when deactivating already inactive pool operator")
    void shouldThrowExceptionWhenDeactivatingAlreadyInactivePoolOperator() {
        // Given
        PoolOperator poolOperator = PoolOperator.register(
                "Test Company",
                new TaxId("1234567890"),
                new ContactInfo("05551234567", "test@example.com", "John Doe")
        );
        poolOperator.deactivate();

        // When/Then
        assertThatThrownBy(poolOperator::deactivate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already inactive");
    }

    @Test
    @DisplayName("Should publish PoolOperatorRegistered event on registration")
    void shouldPublishPoolOperatorRegisteredEvent() {
        // Given
        String companyName = "Test Company";
        TaxId taxId = new TaxId("1234567890");
        ContactInfo contactInfo = new ContactInfo("05551234567", "test@example.com", "John Doe");

        // When
        PoolOperator poolOperator = PoolOperator.register(companyName, taxId, contactInfo);

        // Then
        assertThat(poolOperator.getDomainEvents()).hasSize(1);
        PoolOperatorRegistered event = (PoolOperatorRegistered) poolOperator.getDomainEvents().get(0);
        assertThat(event.companyName()).isEqualTo(companyName);
        assertThat(event.taxId()).isEqualTo(taxId.value());
    }
}
