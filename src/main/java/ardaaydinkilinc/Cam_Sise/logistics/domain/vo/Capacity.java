package ardaaydinkilinc.Cam_Sise.logistics.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Vehicle/Plan capacity value object
 */
public record Capacity(
        int pallets,
        int separators
) implements ValueObject {

    public Capacity {
        if (pallets < 0 || separators < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
    }

    /**
     * Check if this capacity can accommodate the required capacity
     */
    public boolean canAccommodate(Capacity required) {
        return this.pallets >= required.pallets &&
                this.separators >= required.separators;
    }

    /**
     * Subtract used capacity
     */
    public Capacity subtract(Capacity used) {
        int newPallets = this.pallets - used.pallets;
        int newSeparators = this.separators - used.separators;

        if (newPallets < 0 || newSeparators < 0) {
            throw new IllegalArgumentException("Resulting capacity cannot be negative");
        }

        return new Capacity(newPallets, newSeparators);
    }

    /**
     * Add capacities
     */
    public Capacity add(Capacity other) {
        return new Capacity(
                this.pallets + other.pallets,
                this.separators + other.separators
        );
    }

    /**
     * Check if this capacity is zero
     */
    public boolean isEmpty() {
        return pallets == 0 && separators == 0;
    }

    public String formatted() {
        return String.format("%d pallets, %d separators", pallets, separators);
    }
}
