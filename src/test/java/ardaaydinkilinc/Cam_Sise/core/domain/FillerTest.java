package ardaaydinkilinc.Cam_Sise.core.domain;

import ardaaydinkilinc.Cam_Sise.core.domain.event.FillerActivated;
import ardaaydinkilinc.Cam_Sise.core.domain.event.FillerDeactivated;
import ardaaydinkilinc.Cam_Sise.core.domain.event.FillerRegistered;
import ardaaydinkilinc.Cam_Sise.core.domain.event.FillerUpdated;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Filler Domain Tests")
class FillerTest {

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final String NAME = "Test Dolumcu A.Ş.";
    private static final Address ADDRESS = new Address("Test Sok. No:1", "İstanbul", "Kadıköy", "34710", "Türkiye");
    private static final GeoCoordinates LOCATION = new GeoCoordinates(41.0, 29.0);
    private static final ContactInfo CONTACT = new ContactInfo("05551234567", "test@dolumcu.com", "Ali Veli");
    private static final TaxId TAX_ID = new TaxId("1234567890");

    private Filler createFiller() {
        return Filler.register(POOL_OPERATOR_ID, NAME, ADDRESS, LOCATION, CONTACT, TAX_ID);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Geçerli verilerle kayıt oluşturmalı ve aktif olmalı")
        void shouldRegisterFillerAndBeActive() {
            Filler filler = createFiller();

            assertThat(filler.getPoolOperatorId()).isEqualTo(POOL_OPERATOR_ID);
            assertThat(filler.getName()).isEqualTo(NAME);
            assertThat(filler.getAddress()).isEqualTo(ADDRESS);
            assertThat(filler.getLocation()).isEqualTo(LOCATION);
            assertThat(filler.getContactInfo()).isEqualTo(CONTACT);
            assertThat(filler.getTaxId()).isEqualTo(TAX_ID);
            assertThat(filler.getActive()).isTrue();
        }

        @Test
        @DisplayName("FillerRegistered eventi yayınlamalı")
        void shouldPublishFillerRegisteredEvent() {
            Filler filler = createFiller();

            assertThat(filler.getDomainEvents()).hasSize(1);
            assertThat(filler.getDomainEvents().get(0)).isInstanceOf(FillerRegistered.class);
            FillerRegistered event = (FillerRegistered) filler.getDomainEvents().get(0);
            assertThat(event.poolOperatorId()).isEqualTo(POOL_OPERATOR_ID);
            assertThat(event.fillerName()).isEqualTo(NAME);
        }

        @Test
        @DisplayName("taxId null olsa da kayıt oluşturulabilmeli")
        void shouldRegisterWithNullTaxId() {
            Filler filler = Filler.register(POOL_OPERATOR_ID, NAME, ADDRESS, LOCATION, CONTACT, null);

            assertThat(filler.getTaxId()).isNull();
            assertThat(filler.getActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("activate()")
    class Activate {

        @Test
        @DisplayName("İnaktif filler aktif edilebilmeli")
        void shouldActivateInactiveFiller() {
            Filler filler = createFiller();
            filler.deactivate();
            filler.clearDomainEvents();

            filler.activate();

            assertThat(filler.getActive()).isTrue();
        }

        @Test
        @DisplayName("Aktivasyon FillerActivated eventi yayınlamalı")
        void shouldPublishFillerActivatedEvent() {
            Filler filler = createFiller();
            filler.deactivate();
            filler.clearDomainEvents();

            filler.activate();

            assertThat(filler.getDomainEvents()).hasSize(1);
            assertThat(filler.getDomainEvents().get(0)).isInstanceOf(FillerActivated.class);
        }

        @Test
        @DisplayName("Zaten aktif filler için exception fırlatmalı")
        void shouldThrowWhenAlreadyActive() {
            Filler filler = createFiller();

            assertThatThrownBy(filler::activate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already active");
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        @Test
        @DisplayName("Aktif filler deaktif edilebilmeli")
        void shouldDeactivateActiveFiller() {
            Filler filler = createFiller();
            filler.clearDomainEvents();

            filler.deactivate();

            assertThat(filler.getActive()).isFalse();
        }

        @Test
        @DisplayName("Deaktivasyon FillerDeactivated eventi yayınlamalı")
        void shouldPublishFillerDeactivatedEvent() {
            Filler filler = createFiller();
            filler.clearDomainEvents();

            filler.deactivate();

            assertThat(filler.getDomainEvents()).hasSize(1);
            assertThat(filler.getDomainEvents().get(0)).isInstanceOf(FillerDeactivated.class);
        }

        @Test
        @DisplayName("Zaten inaktif filler için exception fırlatmalı")
        void shouldThrowWhenAlreadyInactive() {
            Filler filler = createFiller();
            filler.deactivate();

            assertThatThrownBy(filler::deactivate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already inactive");
        }
    }

    @Nested
    @DisplayName("updateName()")
    class UpdateName {

        @Test
        @DisplayName("İsmi güncellemeli ve FillerUpdated eventi yayınlamalı")
        void shouldUpdateNameAndPublishEvent() {
            Filler filler = createFiller();
            filler.clearDomainEvents();
            String newName = "Yeni Dolumcu Ltd.";

            filler.updateName(newName);

            assertThat(filler.getName()).isEqualTo(newName);
            assertThat(filler.getDomainEvents()).hasSize(1);
            FillerUpdated event = (FillerUpdated) filler.getDomainEvents().get(0);
            assertThat(event.updatedField()).isEqualTo("name");
        }
    }

    @Nested
    @DisplayName("updateAddress()")
    class UpdateAddress {

        @Test
        @DisplayName("Adresi güncellemeli ve FillerUpdated eventi yayınlamalı")
        void shouldUpdateAddressAndPublishEvent() {
            Filler filler = createFiller();
            filler.clearDomainEvents();
            Address newAddress = new Address("Yeni Cad. No:5", "Ankara", "Çankaya", "06450", "Türkiye");

            filler.updateAddress(newAddress);

            assertThat(filler.getAddress()).isEqualTo(newAddress);
            assertThat(filler.getDomainEvents()).hasSize(1);
            FillerUpdated event = (FillerUpdated) filler.getDomainEvents().get(0);
            assertThat(event.updatedField()).isEqualTo("address");
        }
    }

    @Nested
    @DisplayName("updateContactInfo()")
    class UpdateContactInfo {

        @Test
        @DisplayName("İletişim bilgilerini güncellemeli ve FillerUpdated eventi yayınlamalı")
        void shouldUpdateContactInfoAndPublishEvent() {
            Filler filler = createFiller();
            filler.clearDomainEvents();
            ContactInfo newContact = new ContactInfo("05559876543", "yeni@dolumcu.com", "Mehmet Yılmaz");

            filler.updateContactInfo(newContact);

            assertThat(filler.getContactInfo()).isEqualTo(newContact);
            assertThat(filler.getDomainEvents()).hasSize(1);
            FillerUpdated event = (FillerUpdated) filler.getDomainEvents().get(0);
            assertThat(event.updatedField()).isEqualTo("contactInfo");
        }
    }

    @Nested
    @DisplayName("updateLocation()")
    class UpdateLocation {

        @Test
        @DisplayName("Konumu güncellemeli ve FillerUpdated eventi yayınlamalı")
        void shouldUpdateLocationAndPublishEvent() {
            Filler filler = createFiller();
            filler.clearDomainEvents();
            GeoCoordinates newLocation = new GeoCoordinates(39.9, 32.8);

            filler.updateLocation(newLocation);

            assertThat(filler.getLocation()).isEqualTo(newLocation);
            assertThat(filler.getDomainEvents()).hasSize(1);
            FillerUpdated event = (FillerUpdated) filler.getDomainEvents().get(0);
            assertThat(event.updatedField()).isEqualTo("location");
        }
    }
}
