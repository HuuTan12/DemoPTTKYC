package fastship.dao;

import fastship.model.Payment;
import fastship.model.PaymentStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for Payment table (Table 34 & Table 40).
 */
public class PaymentDAO {

    public void saveOrUpdate(Connection conn, Payment payment) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Payment existing = findByOrderId(payment.getOrderId());
            if (existing == null) {
                payment.setPaymentId(InMemoryStore.nextPaymentId());
                InMemoryStore.payments.add(payment);
            } else {
                existing.setReceivedAmount(payment.getReceivedAmount());
                existing.setCollectedAt(payment.getCollectedAt());
                existing.setStatus(payment.getStatus());
                existing.setReconciledBy(payment.getReconciledBy());
                existing.setReconciledAt(payment.getReconciledAt());
                existing.setNote(payment.getNote());
            }
            return;
        }
        Payment existing = findByOrderId(payment.getOrderId());
        if (existing == null) {
            String sql = "INSERT INTO Payment (order_id, driver_id, cod_amount, received_amount, collected_at, status, reconciled_by, reconciled_at, note) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, payment.getOrderId());
                ps.setInt(2, payment.getDriverId());
                ps.setLong(3, payment.getCodAmount());
                if (payment.getReceivedAmount() != null) ps.setLong(4, payment.getReceivedAmount());
                else ps.setNull(4, Types.DECIMAL);
                if (payment.getCollectedAt() != null) ps.setTimestamp(5, Timestamp.valueOf(payment.getCollectedAt()));
                else ps.setNull(5, Types.TIMESTAMP);
                ps.setString(6, payment.getStatus().name());
                if (payment.getReconciledBy() != null) ps.setInt(7, payment.getReconciledBy());
                else ps.setNull(7, Types.INTEGER);
                if (payment.getReconciledAt() != null) ps.setTimestamp(8, Timestamp.valueOf(payment.getReconciledAt()));
                else ps.setNull(8, Types.TIMESTAMP);
                ps.setString(9, payment.getNote());
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE Payment SET received_amount=?, collected_at=?, status=?, reconciled_by=?, reconciled_at=?, note=? WHERE order_id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (payment.getReceivedAmount() != null) ps.setLong(1, payment.getReceivedAmount());
                else ps.setNull(1, Types.DECIMAL);
                if (payment.getCollectedAt() != null) ps.setTimestamp(2, Timestamp.valueOf(payment.getCollectedAt()));
                else ps.setNull(2, Types.TIMESTAMP);
                ps.setString(3, payment.getStatus().name());
                if (payment.getReconciledBy() != null) ps.setInt(4, payment.getReconciledBy());
                else ps.setNull(4, Types.INTEGER);
                if (payment.getReconciledAt() != null) ps.setTimestamp(5, Timestamp.valueOf(payment.getReconciledAt()));
                else ps.setNull(5, Types.TIMESTAMP);
                ps.setString(6, payment.getNote());
                ps.setInt(7, payment.getOrderId());
                ps.executeUpdate();
            }
        }
    }

    public Payment findByOrderId(int orderId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            return InMemoryStore.payments.stream()
                    .filter(p -> p.getOrderId() == orderId)
                    .findFirst()
                    .orElse(null);
        }
        String sql = "SELECT * FROM Payment WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding Payment: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Payment> findAll() {
        if (!DatabaseConnection.isMySqlAvailable()) {
            return new ArrayList<>(InMemoryStore.payments);
        }
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM Payment ORDER BY payment_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading Payments: " + e.getMessage(), e);
        }
        return list;
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("payment_id"));
        p.setOrderId(rs.getInt("order_id"));
        p.setDriverId(rs.getInt("driver_id"));
        p.setCodAmount(rs.getLong("cod_amount"));
        long recv = rs.getLong("received_amount");
        p.setReceivedAmount(rs.wasNull() ? null : recv);
        Timestamp colTs = rs.getTimestamp("collected_at");
        if (colTs != null) p.setCollectedAt(colTs.toLocalDateTime());
        p.setStatus(PaymentStatus.valueOf(rs.getString("status")));
        int recBy = rs.getInt("reconciled_by");
        p.setReconciledBy(rs.wasNull() ? null : recBy);
        Timestamp recTs = rs.getTimestamp("reconciled_at");
        if (recTs != null) p.setReconciledAt(recTs.toLocalDateTime());
        p.setNote(rs.getString("note"));
        return p;
    }
}
