package fastship.model;

import java.time.LocalDateTime;

/**
 * TrackingEvent entity (Figure 5 - Class Diagram & Table 32).
 */
public class TrackingEvent {
    private long eventId;
    private int orderId;
    private OrderStatus status;
    private String note;
    private String recordedBy;
    private LocalDateTime eventTime;

    public TrackingEvent() {
    }

    public TrackingEvent(long eventId, int orderId, OrderStatus status, String note, String recordedBy, LocalDateTime eventTime) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.status = status;
        this.note = note;
        this.recordedBy = recordedBy;
        this.eventTime = eventTime;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }
}

