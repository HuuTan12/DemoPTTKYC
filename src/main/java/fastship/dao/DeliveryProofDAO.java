package fastship.dao;

import fastship.model.DeliveryProof;
import fastship.model.Payment;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for DeliveryProof table (Table 33 & Table 40).
 */
public class DeliveryProofDAO {

    private final PaymentDAO paymentDAO = new PaymentDAO();

    public void saveOrUpdate(Connection conn, DeliveryProof proof) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            DeliveryProof existing = findByOrderId(proof.getOrderId());
            if (existing == null) {
                proof.setProofId(InMemoryStore.nextProofId());
                InMemoryStore.deliveryProofs.add(proof);
            } else {
                existing.setReceivedBy(proof.getReceivedBy());
                existing.setDeliveredAt(proof.getDeliveredAt());
                existing.setNote(proof.getNote());
                existing.setPhotoRef(proof.getPhotoRef());
                existing.setConfirmedBy(proof.getConfirmedBy());
                existing.setConfirmedAt(proof.getConfirmedAt());
            }
            return;
        }
        DeliveryProof existing = findByOrderId(proof.getOrderId());
        if (existing == null) {
            String sql = "INSERT INTO DeliveryProof (order_id, received_by, delivered_at, note, photo_ref, confirmed_by, confirmed_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, proof.getOrderId());
                ps.setString(2, proof.getReceivedBy());
                ps.setTimestamp(3, Timestamp.valueOf(proof.getDeliveredAt()));
                ps.setString(4, proof.getNote());
                ps.setString(5, proof.getPhotoRef());
                if (proof.getConfirmedBy() != null) ps.setInt(6, proof.getConfirmedBy());
                else ps.setNull(6, Types.INTEGER);
                if (proof.getConfirmedAt() != null) ps.setTimestamp(7, Timestamp.valueOf(proof.getConfirmedAt()));
                else ps.setNull(7, Types.TIMESTAMP);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) proof.setProofId(rs.getInt(1));
                }
            }
        } else {
            String sql = "UPDATE DeliveryProof SET received_by=?, delivered_at=?, note=?, photo_ref=?, confirmed_by=?, confirmed_at=? WHERE order_id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, proof.getReceivedBy());
                ps.setTimestamp(2, Timestamp.valueOf(proof.getDeliveredAt()));
                ps.setString(3, proof.getNote());
                ps.setString(4, proof.getPhotoRef());
                if (proof.getConfirmedBy() != null) ps.setInt(5, proof.getConfirmedBy());
                else ps.setNull(5, Types.INTEGER);
                if (proof.getConfirmedAt() != null) ps.setTimestamp(6, Timestamp.valueOf(proof.getConfirmedAt()));
                else ps.setNull(6, Types.TIMESTAMP);
                ps.setInt(7, proof.getOrderId());
                ps.executeUpdate();
            }
        }
    }

    public DeliveryProof findByOrderId(int orderId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            DeliveryProof dp = InMemoryStore.deliveryProofs.stream()
                    .filter(p -> p.getOrderId() == orderId)
                    .findFirst()
                    .orElse(null);
            if (dp != null) {
                dp.setPayment(paymentDAO.findByOrderId(orderId));
            }
            return dp;
        }
        String sql = "SELECT dp.*, o.cod_amount, d.driver_id, d.full_name AS driver_name " +
                     "FROM DeliveryProof dp " +
                     "JOIN orders o ON dp.order_id = o.order_id " +
                     "LEFT JOIN Assignment a ON a.order_id = o.order_id AND a.status IN ('ACCEPTED', 'COMPLETED') " +
                     "LEFT JOIN Driver d ON a.driver_id = d.driver_id " +
                     "WHERE dp.order_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DeliveryProof dp = mapRow(rs);
                    dp.setPayment(paymentDAO.findByOrderId(orderId));
                    return dp;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding DeliveryProof: " + e.getMessage(), e);
        }
        return null;
    }

    public DeliveryProof findById(int proofId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            DeliveryProof dp = InMemoryStore.deliveryProofs.stream()
                    .filter(p -> p.getProofId() == proofId)
                    .findFirst()
                    .orElse(null);
            if (dp != null) {
                dp.setPayment(paymentDAO.findByOrderId(dp.getOrderId()));
            }
            return dp;
        }
        String sql = "SELECT dp.*, o.cod_amount, d.driver_id, d.full_name AS driver_name " +
                     "FROM DeliveryProof dp " +
                     "JOIN orders o ON dp.order_id = o.order_id " +
                     "LEFT JOIN Assignment a ON a.order_id = o.order_id AND a.status IN ('ACCEPTED', 'COMPLETED') " +
                     "LEFT JOIN Driver d ON a.driver_id = d.driver_id " +
                     "WHERE dp.proof_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, proofId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DeliveryProof dp = mapRow(rs);
                    dp.setPayment(paymentDAO.findByOrderId(dp.getOrderId()));
                    return dp;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding DeliveryProof by id: " + e.getMessage(), e);
        }
        return null;
    }

    public List<DeliveryProof> findAllForStaff(Integer driverIdFilter) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            List<DeliveryProof> list = new ArrayList<>();
            for (DeliveryProof dp : InMemoryStore.deliveryProofs) {
                dp.setPayment(paymentDAO.findByOrderId(dp.getOrderId()));
                if (driverIdFilter != null && driverIdFilter > 0 && dp.getDriverId() != driverIdFilter) {
                    continue;
                }
                list.add(dp);
            }
            return list;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT dp.*, o.cod_amount, d.driver_id, d.full_name AS driver_name " +
                "FROM DeliveryProof dp " +
                "JOIN orders o ON dp.order_id = o.order_id " +
                "LEFT JOIN Assignment a ON a.order_id = o.order_id AND a.status IN ('ACCEPTED', 'COMPLETED') " +
                "LEFT JOIN Driver d ON a.driver_id = d.driver_id WHERE 1=1 ");
        if (driverIdFilter != null && driverIdFilter > 0) {
            sql.append("AND d.driver_id = ? ");
        }
        sql.append("ORDER BY dp.delivered_at DESC");

        List<DeliveryProof> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (driverIdFilter != null && driverIdFilter > 0) {
                ps.setInt(1, driverIdFilter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DeliveryProof dp = mapRow(rs);
                    dp.setPayment(paymentDAO.findByOrderId(dp.getOrderId()));
                    list.add(dp);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading DeliveryProofs: " + e.getMessage(), e);
        }
        return list;
    }

    private DeliveryProof mapRow(ResultSet rs) throws SQLException {
        DeliveryProof dp = new DeliveryProof();
        dp.setProofId(rs.getInt("proof_id"));
        dp.setOrderId(rs.getInt("order_id"));
        dp.setReceivedBy(rs.getString("received_by"));
        Timestamp delTs = rs.getTimestamp("delivered_at");
        if (delTs != null) dp.setDeliveredAt(delTs.toLocalDateTime());
        dp.setNote(rs.getString("note"));
        dp.setPhotoRef(rs.getString("photo_ref"));
        int confBy = rs.getInt("confirmed_by");
        dp.setConfirmedBy(rs.wasNull() ? null : confBy);
        Timestamp confTs = rs.getTimestamp("confirmed_at");
        if (confTs != null) dp.setConfirmedAt(confTs.toLocalDateTime());
        dp.setCodAmount(rs.getLong("cod_amount"));
        dp.setDriverId(rs.getInt("driver_id"));
        dp.setDriverName(rs.getString("driver_name"));
        return dp;
    }
}
