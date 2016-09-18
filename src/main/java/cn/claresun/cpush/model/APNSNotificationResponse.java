package cn.claresun.cpush.model;

import java.util.Date;

/**
 * Created by claresun on 16-8-17.
 */
public class APNSNotificationResponse {
    private final APNSNotification notification;
    private final boolean success;
    private final String rejectionReason;
    private final Date tokenExpirationTimestamp;

    public APNSNotificationResponse(APNSNotification notification, boolean success, String rejectionReason, Date tokenExpirationTimestamp) {
        this.notification = notification;
        this.success = success;
        this.rejectionReason = rejectionReason;
        this.tokenExpirationTimestamp = tokenExpirationTimestamp;
    }

    public APNSNotification getToken() {
        return notification;
    }

    public boolean isAccepted() {
        return this.success;
    }

    public String getRejectionReason() {
        return this.rejectionReason;
    }

    public Date getTokenInvalidationTimestamp() {
        return this.tokenExpirationTimestamp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("APNSNotificationResponse{");
        sb.append("notification='").append(notification).append('\'');
        sb.append(", success=").append(success);
        sb.append(", rejectionReason='").append(rejectionReason).append('\'');
        sb.append(", tokenExpirationTimestamp=").append(tokenExpirationTimestamp);
        sb.append('}');
        return sb.toString();
    }
}
