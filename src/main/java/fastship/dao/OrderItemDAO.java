package fastship.dao;

import fastship.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for OrderItem table (Table 28 & Table 40).
 */
public class OrderItemDAO {

    public void insertItems(Connection conn, int orderId, List<OrderItem> items) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            for (OrderItem item : items) {
                item.setOrderItemId(InMemoryStore.nextOrderItemId());
                item.setOrderId(orderId);
                InMemoryStore.orderItems.add(item);
            }
            return;
        }
        String sql = "INSERT INTO OrderItem (order_id, description, quantity, weight_kg) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (OrderItem item : items) {
                ps.setInt(1, orderId);
                ps.setString(2, item.getDescription());
                ps.setInt(3, item.getQuantity());
                ps.setDouble(4, item.getWeightKg());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<OrderItem> findByOrderId(int orderId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            List<OrderItem> result = new ArrayList<>();
            for (OrderItem item : InMemoryStore.orderItems) {
                if (item.getOrderId() == orderId) {
                    result.add(item);
                }
            }
            return result;
        }
        List<OrderItem> list = new ArrayList<>();
        String sql = "SELECT order_item_id, order_id, description, quantity, weight_kg FROM OrderItem WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OrderItem(
                            rs.getInt("order_item_id"),
                            rs.getInt("order_id"),
                            rs.getString("description"),
                            rs.getInt("quantity"),
                            rs.getDouble("weight_kg")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading OrderItems: " + e.getMessage(), e);
        }
        return list;
    }
}
