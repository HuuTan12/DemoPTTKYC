package fastship.model;

import java.time.LocalDateTime;

/**
 * Payment entity for COD reconciliation (Figure 5 - Class Diagram & Table 34).
 */
public class Payment {
    private int paymentId;
    private int orderId;
    private int driverId;
    private long codAmount;
    private Long receivedAmount;
    private LocalDateTime collectedAt;
    private PaymentStatus status = PaymentStatus.PENDING;
    private Integer reconciledBy;
    private LocalDateTime reconciledAt;
    private String note;

    public Payment() {
    }

    public void collect(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Collected amount cannot be negative.");
        }
        this.receivedAmount = amount;
        this.collectedAt = LocalDateTime.now();
        this.status = PaymentStatus.COLLECTED;
    }

    public void reconcile(UserAccount staff) {
        if (this.receivedAmount == null) {
            throw new IllegalStateException("Cannot reconcile before recording received cash.");
        }
        this.reconciledBy = staff.getUserId();
        this.reconciledAt = LocalDateTime.now();
        if (this.receivedAmount.longValue() == this.codAmount) {
            this.status = PaymentStatus.RECONCILED;
        } else {
            this.status = PaymentStatus.DIFFERENCE;
        }
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getDriverId() {
        return driverId;
    }

    public void setDriverId(int driverId) {
        this.driverId = driverId;
    }

    public long getCodAmount() {
        return codAmount;
    }

    public void setCodAmount(long codAmount) {
        this.codAmount = codAmount;
    }

    public Long getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(Long receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Integer getReconciledBy() {
        return reconciledBy;
    }

    public void setReconciledBy(Integer reconciledBy) {
        this.reconciledBy = reconciledBy;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

