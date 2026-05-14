package ardaaydinkilinc.Cam_Sise.notification.domain.vo;

/**
 * Categorises notifications so the UI can filter / icon them, and so
 * the email template engine can pick the right copy.
 */
public enum NotificationType {
    STOCK_ANOMALY,
    LOW_STOCK,
    STOCK_THRESHOLD_EXCEEDED,
    COLLECTION_REQUEST_CREATED,
    COLLECTION_REQUEST_APPROVED,
    COLLECTION_REQUEST_REJECTED,
    COLLECTION_REQUEST_COMPLETED,
    COLLECTION_PLAN_GENERATED,
    COLLECTION_PLAN_ASSIGNED,
    COLLECTION_PLAN_COMPLETED,
    SYSTEM_INFO
}
