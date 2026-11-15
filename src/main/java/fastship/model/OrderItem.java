package fastship.model;

/**
 * OrderItem entity (Figure 5 - Class Diagram & Table 28).
 */
public class OrderItem {
    private int orderItemId;
    private int orderId;
    private String description;
    private int quantity;
    private double weightKg;

    public OrderItem() {
    }

    public OrderItem(int orderItemId, int orderId, String description, int quantity, double weightKg) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.description = description;
        this.quantity = quantity;
        this.weightKg = weightKg;
    }

    public OrderItem(String description, int quantity, double weightKg) {
        this(0, 0, description, quantity, weightKg);
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }
}

