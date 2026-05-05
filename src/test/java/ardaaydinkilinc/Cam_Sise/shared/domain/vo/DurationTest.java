package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Duration Tests")
class DurationTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("geçerli süreyi kabul etmeli")
        void acceptsValidDuration() {
            Duration d = new Duration(90);
            assertThat(d.minutes()).isEqualTo(90);
        }

        @Test
        @DisplayName("sıfır süreyi kabul etmeli")
        void acceptsZero() {
            assertThat(new Duration(0).minutes()).isEqualTo(0);
        }

        @Test
        @DisplayName("negatif sürede exception fırlatmalı")
        void throwsOnNegative() {
            assertThatThrownBy(() -> new Duration(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("toHours")
    class ToHours {

        @Test
        @DisplayName("dakikayı saate çevirmeli")
        void convertsMinutesToHours() {
            assertThat(new Duration(120).toHours()).isEqualTo(2);
            assertThat(new Duration(90).toHours()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getRemainingMinutes")
    class GetRemainingMinutes {

        @Test
        @DisplayName("kalan dakikaları döndürmeli")
        void returnsRemainingMinutes() {
            assertThat(new Duration(90).getRemainingMinutes()).isEqualTo(30);
            assertThat(new Duration(60).getRemainingMinutes()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("iki süreyi toplameli")
        void addsTwoDurations() {
            Duration result = new Duration(60).add(new Duration(30));
            assertThat(result.minutes()).isEqualTo(90);
        }
    }

    @Nested
    @DisplayName("format")
    class Format {

        @Test
        @DisplayName("saat ve dakika formatı döndürmeli")
        void formatsWithHoursAndMinutes() {
            assertThat(new Duration(90).format()).isEqualTo("1 hours 30 minutes");
        }

        @Test
        @DisplayName("sadece dakika formatı döndürmeli")
        void formatsMinutesOnly() {
            assertThat(new Duration(45).format()).isEqualTo("45 minutes");
        }
    }
}
