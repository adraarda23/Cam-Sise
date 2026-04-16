package ardaaydinkilinc.Cam_Sise.inventory.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Loss rate percentage value object
 */
public record LossRate(double percentage) implements ValueObject {

    public LossRate {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Loss rate must be between 0 and 100");
        }
    }

    /**
     * Calculate the number of lost items from a total quantity
     */
    public int calculateLoss(int totalQuantity) {
        return (int) Math.round(totalQuantity * (percentage / 100.0));
    }

    /**
     * Calculate remaining quantity after loss
     */
    public int calculateRemaining(int totalQuantity) {
        return totalQuantity - calculateLoss(totalQuantity);
    }

    public String formatted() {
        return String.format("%.2f%%", percentage);
    }
}
