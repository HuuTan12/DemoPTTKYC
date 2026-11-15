package fastship.dao;

import fastship.model.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DAO for Notification table (Table 35 & Table 40).
 */
public class NotificationDAO {

    public void insert(Connection conn, Notification n) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            n.setNotificationId(InMemoryStore.nextNotificationId());
            if (n.getSentAt() == null) n.setSentAt(LocalDateTime.now());
            InMemoryStore.notifications.add(n);
            return;
        }
        String sql = "INSERT INTO Notification (order_id, channel, message, sent_at, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, n.getOrderId());
            ps.setString(2, n.getChannel() != null ? n.getChannel() : "IN_APP");
            ps.setString(3, n.getMessage());
            ps.setTimestamp(4, Timestamp.valueOf(n.getSentAt() != null ? n.getSentAt() : LocalDateTime.now()));
            ps.setString(5, n.isRead() ? "READ" : "UNREAD");
            ps.executeUpdate();
        }
    }

    public List<Notification> findByCustomerId(int customerId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            List<Notification> list = new ArrayList<>();
            for (Notification n : InMemoryStore.notifications) {
                boolean belongs = InMemoryStore.orders.stream()
                        .anyMatch(o -> o.getOrderId() == n.getOrderId() && o.getCustomerId() == customerId);
                if (belongs) list.add(n);
            }
            list.sort(Comparator.comparingInt(Notification::getNotificationId).reversed());
            return list;
        }
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT n.* FROM Notification n JOIN orders o ON n.order_id = o.order_id " +
                     "WHERE o.customer_id = ? ORDER BY n.sent_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Notification(
                            rs.getInt("notification_id"),
                            rs.getInt("order_id"),
                            rs.getString("channel"),
                            rs.getString("message"),
                            rs.getTimestamp("sent_at").toLocalDateTime(),
                            "READ".equalsIgnoreCase(rs.getString("status"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying Notifications: " + e.getMessage(), e);
        }
        return list;
    }
}
