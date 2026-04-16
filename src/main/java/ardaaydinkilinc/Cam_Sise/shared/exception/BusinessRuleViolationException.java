package ardaaydinkilinc.Cam_Sise.shared.exception;

/**
 * Exception thrown when a business rule is violated
 */
public class BusinessRuleViolationException extends RuntimeException {

    private final String ruleCode;

    public BusinessRuleViolationException(String message) {
        super(message);
        this.ruleCode = null;
    }

    public BusinessRuleViolationException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
