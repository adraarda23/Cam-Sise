package ardaaydinkilinc.Cam_Sise.notification.domain.vo;

/**
 * UI/email severity for a notification.
 * Maps naturally onto banner color and email subject prefix.
 */
public enum NotificationSeverity {
    INFO,       // informational, no action needed
    WARNING,    // action recommended
    CRITICAL    // action required immediately
}
