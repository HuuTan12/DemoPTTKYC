package fastship.dao;

import fastship.model.OrderStatus;
import fastship.model.TrackingEvent;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DAO for TrackingEvent table (Table 32 & Table 40).
 */
public class TrackingEventDAO {

    public void insert(Connection conn, TrackingEvent event) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            event.setEventId(InMemoryStore.nextEventId());
            if (event.getEventTime() == null) {
                event.setEventTime(LocalDateTime.now());
            }
            InMemoryStore.trackingEvents.add(event);
            return;
        }
        String sql = "INSERT INTO TrackingEvent (order_id, status, note, recorded_by, event_time) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, event.getOrderId());
            ps.setString(2, event.getStatus().name());
            ps.setString(3, event.getNote());
            ps.setString(4, event.getRecordedBy());
            ps.setTimestamp(5, Timestamp.valueOf(
                    event.getEventTime() != null ? event.getEventTime() : LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    event.setEventId(rs.getLong(1));
                }
            }
        }
    }

    public List<TrackingEvent> findByOrderId(int orderId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            List<TrackingEvent> list = new ArrayList<>();
            for (TrackingEvent te : InMemoryStore.trackingEvents) {
                if (te.getOrderId() == orderId) {
                    list.add(te);
                }
            }
            list.sort(Comparator.comparing(TrackingEvent::getEventTime).reversed());
            return list;
        }
        List<TrackingEvent> list = new ArrayList<>();
        String sql = "SELECT event_id, order_id, status, note, recorded_by, event_time " +
                     "FROM TrackingEvent WHERE order_id = ? ORDER BY event_time DESC, event_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TrackingEvent(
                            rs.getLong("event_id"),
                            rs.getInt("order_id"),
                            OrderStatus.valueOf(rs.getString("status")),
                            rs.getString("note"),
                            rs.getString("recorded_by"),
                            rs.getTimestamp("event_time").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying TrackingEvents: " + e.getMessage(), e);
        }
        return list;
    }
}
