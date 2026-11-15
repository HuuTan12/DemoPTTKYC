package fastship.model;

import java.time.LocalDateTime;

/**
 * Notification entity (Figure 5 - Class Diagram & Table 35).
 */
public class Notification {
    private int notificationId;
    private int orderId;
    private String channel = "IN_APP";
    private String message;
    private LocalDateTime sentAt;
    private boolean read;

    public Notification() {
    }

    public Notification(int notificationId, int orderId, String channel, String message, LocalDateTime sentAt, boolean read) {
        this.notificationId = notificationId;
        this.orderId = orderId;
        this.channel = channel;
        this.message = message;
        this.sentAt = sentAt;
        this.read = read;
    }

    public void markRead() {
        this.read = true;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}

