package cn.claresun.cpush.handler;

import java.util.Date;

/**
 * Created by claresun on 16-8-17.
 */
public class ErrorResponse {
    private final String reason;
    private final Date timestamp;

    public ErrorResponse(final String reason, final Date timestamp) {
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public String getReason() {
        return this.reason;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ErrorResponse [reason=");
        builder.append(this.reason);
        builder.append(", timestamp=");
        builder.append(this.timestamp);
        builder.append("]");
        return builder.toString();
    }
}