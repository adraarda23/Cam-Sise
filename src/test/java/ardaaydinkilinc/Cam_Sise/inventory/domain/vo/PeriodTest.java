package ardaaydinkilinc.Cam_Sise.inventory.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Period Tests")
class PeriodTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 1, 31);

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("geçerli tarih aralığını kabul etmeli")
        void acceptsValidPeriod() {
            Period period = new Period(START, END);
            assertThat(period.startDate()).isEqualTo(START);
            assertThat(period.endDate()).isEqualTo(END);
        }

        @Test
        @DisplayName("aynı başlangıç ve bitiş tarihini kabul etmeli")
        void acceptsSameStartAndEnd() {
            assertThatCode(() -> new Period(START, START)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null başlangıç tarihinde exception fırlatmalı")
        void throwsOnNullStartDate() {
            assertThatThrownBy(() -> new Period(null, END))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null bitiş tarihinde exception fırlatmalı")
        void throwsOnNullEndDate() {
            assertThatThrownBy(() -> new Period(START, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("başlangıç bitiş tarihinden sonra ise exception fırlatmalı")
        void throwsWhenStartAfterEnd() {
            assertThatThrownBy(() -> new Period(END, START))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("before or equal");
        }
    }

    @Nested
    @DisplayName("getDays")
    class GetDays {

        @Test
        @DisplayName("aralıktaki gün sayısını döndürmeli")
        void returnsDayCount() {
            Period period = new Period(START, END);
            assertThat(period.getDays()).isEqualTo(31);
        }

        @Test
        @DisplayName("tek günlük period için 1 döndürmeli")
        void returnsSingleDay() {
            assertThat(new Period(START, START).getDays()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("contains")
    class Contains {

        @Test
        @DisplayName("aralıktaki tarihi içermeli")
        void containsDateInRange() {
            Period period = new Period(START, END);
            assertThat(period.contains(LocalDate.of(2026, 1, 15))).isTrue();
        }

        @Test
        @DisplayName("sınır tarihlerini içermeli")
        void containsBoundaryDates() {
            Period period = new Period(START, END);
            assertThat(period.contains(START)).isTrue();
            assertThat(period.contains(END)).isTrue();
        }

        @Test
        @DisplayName("aralık dışındaki tarihi içermemeli")
        void doesNotContainDateOutOfRange() {
            Period period = new Period(START, END);
            assertThat(period.contains(LocalDate.of(2026, 2, 1))).isFalse();
        }
    }

    @Nested
    @DisplayName("formatted")
    class Formatted {

        @Test
        @DisplayName("formatlanmış string döndürmeli")
        void returnsFormattedString() {
            Period period = new Period(START, END);
            assertThat(period.formatted()).contains("2026-01-01", "2026-01-31", "31 days");
        }
    }
}
