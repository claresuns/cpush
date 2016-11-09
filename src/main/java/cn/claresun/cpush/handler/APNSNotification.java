package cn.claresun.cpush.handler;

import java.util.Date;

/**
 * Created by claresun on 16-8-11.
 */
public class APNSNotification {
    private final String token;
    private final String payload;
    private final Date invalidationTime;
    private final DeliveryPriority priority;
    private final String topic;

    public APNSNotification(String token, String topic, String payload) {
        this(token, topic, payload, null, DeliveryPriority.IMMEDIATE);
    }

    public APNSNotification(final String token, final String topic, final String payload, final Date invalidationTime) {
        this(token, topic, payload, invalidationTime, DeliveryPriority.IMMEDIATE);
    }

    public APNSNotification(String token, String topic, String payload, Date invalidationTime, DeliveryPriority priority) {
        this.token = token;
        this.payload = payload;
        this.invalidationTime = invalidationTime;
        this.priority = priority;
        this.topic = topic;
    }

    public String getToken() {
        return token;
    }

    public String getPayload() {
        return payload;
    }

    public Date getInvalidationTime() {
        return invalidationTime;
    }

    public DeliveryPriority getPriority() {
        return priority;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.invalidationTime == null) ? 0 : this.invalidationTime.hashCode());
        result = prime * result + ((this.payload == null) ? 0 : this.payload.hashCode());
        result = prime * result + ((this.priority == null) ? 0 : this.priority.hashCode());
        result = prime * result + ((this.token == null) ? 0 : this.token.hashCode());
        result = prime * result + ((this.topic == null) ? 0 : this.topic.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof APNSNotification)) {
            return false;
        }
        final APNSNotification other = (APNSNotification) obj;
        if (this.invalidationTime == null) {
            if (other.invalidationTime != null) {
                return false;
            }
        } else if (!this.invalidationTime.equals(other.invalidationTime)) {
            return false;
        }
        if (this.payload == null) {
            if (other.payload != null) {
                return false;
            }
        } else if (!this.payload.equals(other.payload)) {
            return false;
        }
        if (this.priority != other.priority) {
            return false;
        }
        if (this.token == null) {
            if (other.token != null) {
                return false;
            }
        } else if (!this.token.equals(other.token)) {
            return false;
        }
        if (this.topic == null) {
            if (other.topic != null) {
                return false;
            }
        } else if (!this.topic.equals(other.topic)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[token=");
        builder.append(this.token);
        builder.append(", payload=");
        builder.append(this.payload);
        builder.append(", invalidationTime=");
        builder.append(this.invalidationTime);
        builder.append(", priority=");
        builder.append(this.priority);
        builder.append(", topic=");
        builder.append(this.topic);
        builder.append("]");
        return builder.toString();
    }

}
