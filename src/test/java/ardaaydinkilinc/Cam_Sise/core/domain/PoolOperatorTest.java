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

@DisplayName("PoolOperator Domain Tests")
class PoolOperatorTest {

    @Test
    @DisplayName("Should register new pool operator with valid data")
    void shouldRegisterNewPoolOperator() {
        String companyName = "Test Pool Operator";
        TaxId taxId = new TaxId("1234567890");
        ContactInfo contactInfo = new ContactInfo("05551234567", "test@example.com", "John Doe");

        PoolOperator poolOperator = PoolOperator.register(companyName, taxId, contactInfo);

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
        PoolOperator poolOperator = PoolOperator.register(
                "Test Company",
                new TaxId("1234567890"),
                new ContactInfo("05551234567", "test@example.com", "John Doe")
        );
        poolOperator.deactivate();
        poolOperator.clearDomainEvents();

        poolOperator.activate();

        assertThat(poolOperator.getActive()).isTrue();
        assertThat(poolOperator.getDomainEvents()).hasSize(1);
        assertThat(poolOperator.getDomainEvents().get(0)).isInstanceOf(PoolOperatorActivated.class);
    }

    @Test
    @DisplayName("Should deactivate active pool operator")
    void shouldDeactivatePoolOperator() {
        PoolOperator poolOperator = PoolOperator.register(
                "Test Company",
                new TaxId("1234567890"),
                new ContactInfo("05551234567", "test@example.com", "John Doe")
        );
        poolOperator.clearDomainEvents();

        poolOperator.deactivate();

        assertThat(poolOperator.getActive()).isFalse();
        assertThat(poolOperator.getDomainEvents()).hasSize(1);
        assertThat(poolOperator.getDomainEvents().get(0)).isInstanceOf(PoolOperatorDeactivated.class);
    }

    @Test
    @DisplayName("Should update contact information")
    void shouldUpdateContactInfo() {
        PoolOperator poolOperator = PoolOperator.register(
                "Test Company",
                new TaxId("1234567890"),
                new ContactInfo("05551234567", "test@example.com", "John Doe")
        );

        ContactInfo newContactInfo = new ContactInfo("05559999999", "new@example.com", "Jane Smith");

        poolOperator.updateContactInfo(newContactInfo);

        assertThat(poolOperator.getContactInfo()).isEqualTo(newContactInfo);
        assertThat(poolOperator.getContactInfo().phone()).isEqualTo("05559999999");
        assertThat(poolOperator.getContactInfo().email()).isEqualTo("new@example.com");
        assertThat(poolOperator.getContactInfo().contactPersonName()).isEqualTo("Jane Smith");
    }

    @Test
    @DisplayName("Should throw exception when activating already active pool operator")
    void shouldThrowExceptionWhenActivatingAlreadyActivePoolOperator() {
        PoolOperator poolOperator = PoolOperator.register(
                "Test Company",
                new TaxId("1234567890"),
                new ContactInfo("05551234567", "test@example.com", "John Doe")
        );

        assertThatThrownBy(poolOperator::activate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already active");
    }

    @Test
    @DisplayName("Should throw exception when deactivating already inactive pool operator")
    void shouldThrowExceptionWhenDeactivatingAlreadyInactivePoolOperator() {
        PoolOperator poolOperator = PoolOperator.register(
                "Test Company",
                new TaxId("1234567890"),
                new ContactInfo("05551234567", "test@example.com", "John Doe")
        );
        poolOperator.deactivate();

        assertThatThrownBy(poolOperator::deactivate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already inactive");
    }

    @Test
    @DisplayName("Should publish PoolOperatorRegistered event on registration")
    void shouldPublishPoolOperatorRegisteredEvent() {
        String companyName = "Test Company";
        TaxId taxId = new TaxId("1234567890");
        ContactInfo contactInfo = new ContactInfo("05551234567", "test@example.com", "John Doe");

        PoolOperator poolOperator = PoolOperator.register(companyName, taxId, contactInfo);

        assertThat(poolOperator.getDomainEvents()).hasSize(1);
        PoolOperatorRegistered event = (PoolOperatorRegistered) poolOperator.getDomainEvents().get(0);
        assertThat(event.companyName()).isEqualTo(companyName);
        assertThat(event.taxId()).isEqualTo(taxId.value());
    }
}
