package ardaaydinkilinc.Cam_Sise.inventory.domain;

import ardaaydinkilinc.Cam_Sise.inventory.domain.event.EstimatedLossRateCalculated;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.LossRecordUpdated;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.Period;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LossRecord Domain Tests")
class LossRecordTest {

    private static final Long FILLER_ID = 1L;
    private static final AssetType ASSET_TYPE = AssetType.PALLET;
    private static final LossRate ESTIMATED_RATE = new LossRate(5.0);
    private static final Period PERIOD = new Period(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31)
    );

    private LossRecord createRecord() {
        return LossRecord.createWithEstimate(FILLER_ID, ASSET_TYPE, ESTIMATED_RATE, PERIOD);
    }

    @Nested
    @DisplayName("createWithEstimate()")
    class CreateWithEstimate {

        @Test
        @DisplayName("Doğru filler ve assetType ile kayıt oluşturulmalı")
        void shouldCreateWithCorrectFillerAndAssetType() {
            LossRecord record = createRecord();

            assertThat(record.getFillerId()).isEqualTo(FILLER_ID);
            assertThat(record.getAssetType()).isEqualTo(ASSET_TYPE);
            assertThat(record.getEstimatedRate()).isEqualTo(ESTIMATED_RATE);
            assertThat(record.getCalculationPeriod()).isEqualTo(PERIOD);
        }

        @Test
        @DisplayName("actualRate null olmalı, sadece estimatedRate set edilmeli")
        void shouldHaveNullActualRateOnCreation() {
            LossRecord record = createRecord();

            assertThat(record.getActualRate()).isNull();
            assertThat(record.getEstimatedRate()).isNotNull();
        }

        @Test
        @DisplayName("EstimatedLossRateCalculated eventi yayınlamalı")
        void shouldPublishEstimatedLossRateCalculatedEvent() {
            LossRecord record = createRecord();

            assertThat(record.getDomainEvents()).hasSize(1);
            assertThat(record.getDomainEvents().get(0)).isInstanceOf(EstimatedLossRateCalculated.class);
            EstimatedLossRateCalculated event = (EstimatedLossRateCalculated) record.getDomainEvents().get(0);
            assertThat(event.fillerId()).isEqualTo(FILLER_ID);
            assertThat(event.assetType()).isEqualTo(ASSET_TYPE);
            assertThat(event.estimatedLossRatePercentage()).isEqualTo(ESTIMATED_RATE.percentage());
        }
    }

    @Nested
    @DisplayName("updateActualRate()")
    class UpdateActualRate {

        @Test
        @DisplayName("Actual rate güncellenmeli ve LossRecordUpdated eventi yayınlamalı")
        void shouldUpdateActualRateAndPublishEvent() {
            LossRecord record = createRecord();
            record.clearDomainEvents();
            LossRate actualRate = new LossRate(7.5);

            record.updateActualRate(actualRate);

            assertThat(record.getActualRate()).isEqualTo(actualRate);
            assertThat(record.getDomainEvents()).hasSize(1);
            assertThat(record.getDomainEvents().get(0)).isInstanceOf(LossRecordUpdated.class);
        }

        @Test
        @DisplayName("lastUpdated güncellenmeli")
        void shouldUpdateLastUpdated() {
            LossRecord record = createRecord();

            record.updateActualRate(new LossRate(3.0));

            assertThat(record.getLastUpdated()).isNotNull();
        }
    }

    @Nested
    @DisplayName("recalculateEstimatedRate()")
    class RecalculateEstimatedRate {

        @Test
        @DisplayName("Yeni estimated rate ve period set edilmeli")
        void shouldUpdateEstimatedRateAndPeriod() {
            LossRecord record = createRecord();
            record.clearDomainEvents();
            LossRate newRate = new LossRate(8.0);
            Period newPeriod = new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30));

            record.recalculateEstimatedRate(newRate, newPeriod);

            assertThat(record.getEstimatedRate()).isEqualTo(newRate);
            assertThat(record.getCalculationPeriod()).isEqualTo(newPeriod);
        }

        @Test
        @DisplayName("EstimatedLossRateCalculated eventi yayınlamalı")
        void shouldPublishEstimatedLossRateCalculatedEvent() {
            LossRecord record = createRecord();
            record.clearDomainEvents();

            record.recalculateEstimatedRate(new LossRate(6.0), PERIOD);

            assertThat(record.getDomainEvents()).hasSize(1);
            assertThat(record.getDomainEvents().get(0)).isInstanceOf(EstimatedLossRateCalculated.class);
        }
    }

    @Nested
    @DisplayName("getBestAvailableRate()")
    class GetBestAvailableRate {

        @Test
        @DisplayName("actualRate yokken estimatedRate dönmeli")
        void shouldReturnEstimatedRateWhenActualIsNull() {
            LossRecord record = createRecord();

            LossRate best = record.getBestAvailableRate();

            assertThat(best).isEqualTo(ESTIMATED_RATE);
        }

        @Test
        @DisplayName("actualRate varken actualRate dönmeli")
        void shouldReturnActualRateWhenSet() {
            LossRecord record = createRecord();
            LossRate actual = new LossRate(10.0);
            record.updateActualRate(actual);

            LossRate best = record.getBestAvailableRate();

            assertThat(best).isEqualTo(actual);
        }

        @Test
        @DisplayName("actualRate set edilince estimatedRate'den farklı dönmeli")
        void shouldReturnActualOverEstimated() {
            LossRecord record = createRecord();
            record.updateActualRate(new LossRate(2.0));

            assertThat(record.getBestAvailableRate()).isNotEqualTo(ESTIMATED_RATE);
            assertThat(record.getBestAvailableRate().percentage()).isEqualTo(2.0);
        }
    }
}
