package ardaaydinkilinc.Cam_Sise.logistics.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Capacity Tests")
class CapacityTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("geçerli kapasite oluşturulabilmeli")
        void acceptsValidCapacity() {
            Capacity capacity = new Capacity(100, 50);
            assertThat(capacity.pallets()).isEqualTo(100);
            assertThat(capacity.separators()).isEqualTo(50);
        }

        @Test
        @DisplayName("sıfır kapasite geçerlidir")
        void acceptsZeroCapacity() {
            assertThatCode(() -> new Capacity(0, 0)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("negatif palet sayısında exception fırlatmalı")
        void throwsOnNegativePallets() {
            assertThatThrownBy(() -> new Capacity(-1, 50))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("negatif ayırıcı sayısında exception fırlatmalı")
        void throwsOnNegativeSeparators() {
            assertThatThrownBy(() -> new Capacity(100, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }
    }

    @Nested
    @DisplayName("canAccommodate")
    class CanAccommodate {

        @Test
        @DisplayName("yeterli kapasite varsa true döndürmeli")
        void returnsTrueWhenSufficient() {
            Capacity available = new Capacity(100, 80);
            Capacity required = new Capacity(50, 40);
            assertThat(available.canAccommodate(required)).isTrue();
        }

        @Test
        @DisplayName("eşit kapasite olduğunda true döndürmeli")
        void returnsTrueWhenExact() {
            Capacity available = new Capacity(100, 80);
            assertThat(available.canAccommodate(new Capacity(100, 80))).isTrue();
        }

        @Test
        @DisplayName("palet yetersizse false döndürmeli")
        void returnsFalseWhenPalletsInsufficient() {
            Capacity available = new Capacity(10, 80);
            Capacity required = new Capacity(50, 40);
            assertThat(available.canAccommodate(required)).isFalse();
        }

        @Test
        @DisplayName("ayırıcı yetersizse false döndürmeli")
        void returnsFalseWhenSeparatorsInsufficient() {
            Capacity available = new Capacity(100, 10);
            Capacity required = new Capacity(50, 40);
            assertThat(available.canAccommodate(required)).isFalse();
        }
    }

    @Nested
    @DisplayName("canRouteWith")
    class CanRouteWith {

        @Test
        @DisplayName("her iki boyut da yeterliyse true döndürmeli")
        void returnsTrueWhenBothSufficient() {
            Capacity capacity = new Capacity(100, 50);
            Capacity demand = new Capacity(80, 30);
            assertThat(capacity.canRouteWith(demand)).isTrue();
        }

        @Test
        @DisplayName("palet=0 (unconstrained) yüksek talebe rağmen true döndürmeli")
        void returnsTrueWhenPalletsUnconstrained() {
            Capacity capacity = new Capacity(0, 50);
            Capacity demand = new Capacity(999, 30);
            assertThat(capacity.canRouteWith(demand)).isTrue();
        }

        @Test
        @DisplayName("ayırıcı=0 (unconstrained) yüksek talebe rağmen true döndürmeli")
        void returnsTrueWhenSeparatorsUnconstrained() {
            Capacity capacity = new Capacity(100, 0);
            Capacity demand = new Capacity(80, 999);
            assertThat(capacity.canRouteWith(demand)).isTrue();
        }

        @Test
        @DisplayName("talep kapasiteyi aşıyorsa false döndürmeli")
        void returnsFalseWhenDemandExceedsCapacity() {
            Capacity capacity = new Capacity(100, 50);
            Capacity demand = new Capacity(200, 30);
            assertThat(capacity.canRouteWith(demand)).isFalse();
        }
    }

    @Nested
    @DisplayName("subtract")
    class Subtract {

        @Test
        @DisplayName("geçerli çıkarma sonuç döndürmeli")
        void subtractsSuccessfully() {
            Capacity capacity = new Capacity(100, 80);
            Capacity used = new Capacity(30, 20);
            Capacity result = capacity.subtract(used);
            assertThat(result.pallets()).isEqualTo(70);
            assertThat(result.separators()).isEqualTo(60);
        }

        @Test
        @DisplayName("sıfıra kadar çıkarma geçerlidir")
        void subtractsToZero() {
            Capacity capacity = new Capacity(50, 30);
            Capacity result = capacity.subtract(new Capacity(50, 30));
            assertThat(result.pallets()).isEqualTo(0);
            assertThat(result.separators()).isEqualTo(0);
        }

        @Test
        @DisplayName("palet negatif olursa exception fırlatmalı")
        void throwsWhenPalletsWouldGoNegative() {
            Capacity capacity = new Capacity(10, 80);
            assertThatThrownBy(() -> capacity.subtract(new Capacity(50, 20)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("ayırıcı negatif olursa exception fırlatmalı")
        void throwsWhenSeparatorsWouldGoNegative() {
            Capacity capacity = new Capacity(100, 10);
            assertThatThrownBy(() -> capacity.subtract(new Capacity(50, 50)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("iki kapasiteyi toplamalı")
        void addsTwoCapacities() {
            Capacity a = new Capacity(50, 30);
            Capacity b = new Capacity(20, 10);
            Capacity result = a.add(b);
            assertThat(result.pallets()).isEqualTo(70);
            assertThat(result.separators()).isEqualTo(40);
        }
    }

    @Nested
    @DisplayName("isEmpty")
    class IsEmpty {

        @Test
        @DisplayName("her ikisi de sıfırsa true döndürmeli")
        void returnsTrueWhenBothZero() {
            assertThat(new Capacity(0, 0).isEmpty()).isTrue();
        }

        @Test
        @DisplayName("palet > 0 ise false döndürmeli")
        void returnsFalseWhenPalletsNonZero() {
            assertThat(new Capacity(1, 0).isEmpty()).isFalse();
        }

        @Test
        @DisplayName("ayırıcı > 0 ise false döndürmeli")
        void returnsFalseWhenSeparatorsNonZero() {
            assertThat(new Capacity(0, 1).isEmpty()).isFalse();
        }

        @Test
        @DisplayName("her ikisi > 0 ise false döndürmeli")
        void returnsFalseWhenBothNonZero() {
            assertThat(new Capacity(5, 3).isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("formatted")
    class Formatted {

        @Test
        @DisplayName("formatlanmış string döndürmeli")
        void returnsFormattedString() {
            assertThat(new Capacity(100, 50).formatted()).isEqualTo("100 pallets, 50 separators");
        }
    }
}
