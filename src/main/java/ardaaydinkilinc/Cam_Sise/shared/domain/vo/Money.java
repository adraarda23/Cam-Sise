package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

import java.math.BigDecimal;

/**
 * Money value object
 */
public record Money(
        BigDecimal amount,
        Currency currency
) implements ValueObject {

    public enum Currency {
        TRY("Turkish Lira", "₺"),
        USD("US Dollar", "$"),
        EUR("Euro", "€");

        private final String displayName;
        private final String symbol;

        Currency(String displayName, String symbol) {
            this.displayName = displayName;
            this.symbol = symbol;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract different currencies");
        }
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Resulting amount cannot be negative");
        }
        return new Money(result, this.currency);
    }

    public String formatted() {
        return String.format("%s %s", currency.getSymbol(), amount);
    }
}
