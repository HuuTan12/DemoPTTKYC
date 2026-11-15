package fastship.model;

import java.time.LocalDateTime;

/**
 * Assignment entity (Figure 5 - Class Diagram & Table 31).
 */
public class Assignment {
    private int assignmentId;
    private int orderId;
    private int driverId;
    private int assignedBy;
    private int attemptNo = 1;
    private AssignmentStatus status = AssignmentStatus.PENDING;
    private String rejectReason;
    private LocalDateTime assignedAt;
    private LocalDateTime respondedAt;

    // Optional joined object for UI display
    private Order order;
    private Driver driver;

    public Assignment() {
    }

    public void accept() {
        if (status != AssignmentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING assignments can be accepted.");
        }
        this.status = AssignmentStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (status != AssignmentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING assignments can be rejected.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required.");
        }
        this.status = AssignmentStatus.REJECTED;
        this.rejectReason = reason.trim();
        this.respondedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = AssignmentStatus.CANCELLED;
        this.respondedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = AssignmentStatus.COMPLETED;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
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

    public int getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(int assignedBy) {
        this.assignedBy = assignedBy;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(int attemptNo) {
        this.attemptNo = attemptNo;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }
}

