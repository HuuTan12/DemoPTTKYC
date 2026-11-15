package fastship.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity (Figure 5 - Class Diagram & Table 27).
 */
public class Order {
    private int orderId;
    private int customerId;
    private int pickupAddressId;
    private int deliveryAddressId;
    private int serviceTypeId;
    private double totalWeightKg;
    private long shippingFee;
    private long codAmount;
    private OrderStatus status = OrderStatus.NEW;
    private String note;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;

    // Associated objects for UI display
    private Address pickupAddress;
    private Address deliveryAddress;
    private ServiceType serviceType;
    private List<OrderItem> items = new ArrayList<>();
    private String assignedDriverName;

    public Order() {
    }

    public String getOrderCode() {
        return String.format("DH%04d", orderId);
    }

    public static int parseOrderCode(String codeOrId) {
        if (codeOrId == null) {
            return -1;
        }
        String cleaned = codeOrId.trim().toUpperCase()
                .replace("ORD-", "")
                .replace("ORD", "")
                .replace("DH-", "")
                .replace("DH", "")
                .trim();
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public long calculateFee() {
        if (serviceType == null) {
            return shippingFee;
        }
        return serviceType.calculateFee(totalWeightKg);
    }

    public void confirm() {
        if (this.status != OrderStatus.NEW) {
            throw new IllegalStateException("Only NEW orders can be confirmed.");
        }
        this.status = OrderStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public boolean canCancel() {
        return status == OrderStatus.NEW
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.ASSIGNED;
    }

    public void cancel(String reason) {
        if (!canCancel()) {
            throw new IllegalStateException("Cannot cancel an order that has already been picked up or completed.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required.");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason.trim();
    }

    public void changeStatus(OrderStatus s) {
        this.status = s;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getPickupAddressId() {
        return pickupAddressId;
    }

    public void setPickupAddressId(int pickupAddressId) {
        this.pickupAddressId = pickupAddressId;
    }

    public int getDeliveryAddressId() {
        return deliveryAddressId;
    }

    public void setDeliveryAddressId(int deliveryAddressId) {
        this.deliveryAddressId = deliveryAddressId;
    }

    public int getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(int serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public double getTotalWeightKg() {
        return totalWeightKg;
    }

    public void setTotalWeightKg(double totalWeightKg) {
        this.totalWeightKg = totalWeightKg;
    }

    public long getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(long shippingFee) {
        this.shippingFee = shippingFee;
    }

    public long getCodAmount() {
        return codAmount;
    }

    public void setCodAmount(long codAmount) {
        this.codAmount = codAmount;
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

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Address getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(Address pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getAssignedDriverName() {
        return assignedDriverName;
    }

    public void setAssignedDriverName(String assignedDriverName) {
        this.assignedDriverName = assignedDriverName;
    }
}

