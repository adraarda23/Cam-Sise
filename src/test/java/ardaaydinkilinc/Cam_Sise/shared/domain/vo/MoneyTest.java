package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money Tests")
class MoneyTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("geçerli tutarı kabul etmeli")
        void acceptsValidAmount() {
            Money money = new Money(new BigDecimal("100.00"), Money.Currency.TRY);
            assertThat(money.amount()).isEqualByComparingTo("100.00");
            assertThat(money.currency()).isEqualTo(Money.Currency.TRY);
        }

        @Test
        @DisplayName("sıfır tutarı kabul etmeli")
        void acceptsZeroAmount() {
            assertThatCode(() -> new Money(BigDecimal.ZERO, Money.Currency.USD)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("negatif tutarda exception fırlatmalı")
        void throwsOnNegativeAmount() {
            assertThatThrownBy(() -> new Money(new BigDecimal("-1.00"), Money.Currency.TRY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("null tutarda exception fırlatmalı")
        void throwsOnNullAmount() {
            assertThatThrownBy(() -> new Money(null, Money.Currency.TRY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null para biriminde exception fırlatmalı")
        void throwsOnNullCurrency() {
            assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("aynı para birimindeki tutarları toplamalı")
        void addsSameCurrency() {
            Money m1 = new Money(new BigDecimal("100.00"), Money.Currency.TRY);
            Money m2 = new Money(new BigDecimal("50.00"), Money.Currency.TRY);
            assertThat(m1.add(m2).amount()).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("farklı para biriminde exception fırlatmalı")
        void throwsOnDifferentCurrency() {
            Money m1 = new Money(new BigDecimal("100.00"), Money.Currency.TRY);
            Money m2 = new Money(new BigDecimal("50.00"), Money.Currency.USD);
            assertThatThrownBy(() -> m1.add(m2))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("subtract")
    class Subtract {

        @Test
        @DisplayName("tutarları çıkarmalı")
        void subtractsAmount() {
            Money m1 = new Money(new BigDecimal("100.00"), Money.Currency.TRY);
            Money m2 = new Money(new BigDecimal("30.00"), Money.Currency.TRY);
            assertThat(m1.subtract(m2).amount()).isEqualByComparingTo("70.00");
        }

        @Test
        @DisplayName("negatif sonuçta exception fırlatmalı")
        void throwsOnNegativeResult() {
            Money m1 = new Money(new BigDecimal("10.00"), Money.Currency.TRY);
            Money m2 = new Money(new BigDecimal("20.00"), Money.Currency.TRY);
            assertThatThrownBy(() -> m1.subtract(m2))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Currency enum")
    class CurrencyEnum {

        @Test
        @DisplayName("para birimi displayName ve symbol döndürmeli")
        void returnsCurrencyDetails() {
            assertThat(Money.Currency.TRY.getDisplayName()).isEqualTo("Turkish Lira");
            assertThat(Money.Currency.TRY.getSymbol()).isEqualTo("₺");
            assertThat(Money.Currency.USD.getSymbol()).isEqualTo("$");
            assertThat(Money.Currency.EUR.getSymbol()).isEqualTo("€");
        }
    }
}
