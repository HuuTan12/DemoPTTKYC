package fastship.model;

import java.time.LocalDateTime;

/**
 * DeliveryProof entity (Figure 5 - Class Diagram & Table 33).
 */
public class DeliveryProof {
    private int proofId;
    private int orderId;
    private String receivedBy;
    private LocalDateTime deliveredAt;
    private String note;
    private String photoRef;
    private Integer confirmedBy;
    private LocalDateTime confirmedAt;

    // Optional joined info for Staff UI (Figure 25)
    private String driverName;
    private int driverId;
    private long codAmount;
    private Payment payment;

    public DeliveryProof() {
    }

    public void confirm(UserAccount staff) {
        this.confirmedBy = staff.getUserId();
        this.confirmedAt = LocalDateTime.now();
    }

    public void returnToDriver(String note) {
        this.confirmedBy = null;
        this.confirmedAt = null;
        this.note = note;
    }

    public int getProofId() {
        return proofId;
    }

    public void setProofId(int proofId) {
        this.proofId = proofId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPhotoRef() {
        return photoRef;
    }

    public void setPhotoRef(String photoRef) {
        this.photoRef = photoRef;
    }

    public Integer getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(Integer confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
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

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }
}

