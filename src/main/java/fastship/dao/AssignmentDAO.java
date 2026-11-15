package fastship.dao;

import fastship.model.Assignment;
import fastship.model.AssignmentStatus;
import fastship.model.Driver;
import fastship.model.Order;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DAO for Assignment table (Table 31 & Table 40).
 */
public class AssignmentDAO {

    private final OrderDAO orderDAO = new OrderDAO();
    private final DriverDAO driverDAO = new DriverDAO();

    public int getNextAttemptNo(Connection conn, int orderId) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            return InMemoryStore.assignments.stream()
                    .filter(a -> a.getOrderId() == orderId)
                    .mapToInt(Assignment::getAttemptNo)
                    .max()
                    .orElse(0) + 1;
        }
        String sql = "SELECT COALESCE(MAX(attempt_no), 0) + 1 FROM Assignment WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 1;
    }

    public boolean hasActiveAssignment(Connection conn, int orderId) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            return InMemoryStore.assignments.stream()
                    .anyMatch(a -> a.getOrderId() == orderId
                            && (a.getStatus() == AssignmentStatus.PENDING || a.getStatus() == AssignmentStatus.ACCEPTED));
        }
        String sql = "SELECT COUNT(*) FROM Assignment WHERE order_id = ? AND status IN ('PENDING', 'ACCEPTED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public int insert(Connection conn, Assignment a) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            a.setAssignmentId(InMemoryStore.nextAssignmentId());
            if (a.getAssignedAt() == null) a.setAssignedAt(LocalDateTime.now());
            a.setOrder(orderDAO.findById(a.getOrderId()));
            a.setDriver(driverDAO.findById(a.getDriverId()));
            if (a.getOrder() != null && a.getDriver() != null) {
                a.getOrder().setAssignedDriverName(a.getDriver().getFullName());
            }
            InMemoryStore.assignments.add(a);
            return a.getAssignmentId();
        }
        String sql = "INSERT INTO Assignment (order_id, driver_id, assigned_by, attempt_no, status, assigned_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getOrderId());
            ps.setInt(2, a.getDriverId());
            ps.setInt(3, a.getAssignedBy());
            ps.setInt(4, a.getAttemptNo());
            ps.setString(5, a.getStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(
                    a.getAssignedAt() != null ? a.getAssignedAt() : LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    a.setAssignmentId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to insert Assignment");
    }

    public Assignment findById(int assignmentId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Assignment a = InMemoryStore.assignments.stream()
                    .filter(x -> x.getAssignmentId() == assignmentId)
                    .findFirst()
                    .orElse(null);
            if (a != null) enrich(a);
            return a;
        }
        String sql = "SELECT * FROM Assignment WHERE assignment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Assignment a = mapRow(rs);
                    enrich(a);
                    return a;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding Assignment: " + e.getMessage(), e);
        }
        return null;
    }

    public Assignment findActiveByOrderId(int orderId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Assignment a = InMemoryStore.assignments.stream()
                    .filter(x -> x.getOrderId() == orderId
                            && (x.getStatus() == AssignmentStatus.PENDING
                            || x.getStatus() == AssignmentStatus.ACCEPTED
                            || x.getStatus() == AssignmentStatus.COMPLETED))
                    .max(Comparator.comparingInt(Assignment::getAttemptNo))
                    .orElse(null);
            if (a != null) enrich(a);
            return a;
        }
        String sql = "SELECT * FROM Assignment WHERE order_id = ? AND status IN ('PENDING', 'ACCEPTED', 'COMPLETED') " +
                     "ORDER BY attempt_no DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Assignment a = mapRow(rs);
                    enrich(a);
                    return a;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding active Assignment: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Assignment> findByDriverId(int driverId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            List<Assignment> list = new ArrayList<>();
            for (Assignment a : InMemoryStore.assignments) {
                if (a.getDriverId() == driverId) {
                    enrich(a);
                    list.add(a);
                }
            }
            list.sort(Comparator.comparingInt(Assignment::getAssignmentId).reversed());
            return list;
        }
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT * FROM Assignment WHERE driver_id = ? ORDER BY assignment_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, driverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Assignment a = mapRow(rs);
                    enrich(a);
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying driver assignments: " + e.getMessage(), e);
        }
        return list;
    }

    public void updateStatus(Connection conn, int assignmentId, AssignmentStatus status, String rejectReason, LocalDateTime respondedAt) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Assignment a = findById(assignmentId);
            if (a != null) {
                a.setStatus(status);
                if (rejectReason != null) a.setRejectReason(rejectReason);
                if (respondedAt != null) a.setRespondedAt(respondedAt);
            }
            return;
        }
        String sql = "UPDATE Assignment SET status = ?, reject_reason = COALESCE(?, reject_reason), responded_at = COALESCE(?, responded_at) " +
                     "WHERE assignment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            if (rejectReason != null) {
                ps.setString(2, rejectReason);
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            if (respondedAt != null) {
                ps.setTimestamp(3, Timestamp.valueOf(respondedAt));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }
            ps.setInt(4, assignmentId);
            ps.executeUpdate();
        }
    }

    private void enrich(Assignment a) {
        Order o = orderDAO.findById(a.getOrderId());
        Driver d = driverDAO.findById(a.getDriverId());
        a.setOrder(o);
        a.setDriver(d);
        if (o != null && d != null) {
            o.setAssignedDriverName(d.getFullName());
        }
    }

    private Assignment mapRow(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setAssignmentId(rs.getInt("assignment_id"));
        a.setOrderId(rs.getInt("order_id"));
        a.setDriverId(rs.getInt("driver_id"));
        a.setAssignedBy(rs.getInt("assigned_by"));
        a.setAttemptNo(rs.getInt("attempt_no"));
        a.setStatus(AssignmentStatus.valueOf(rs.getString("status")));
        a.setRejectReason(rs.getString("reject_reason"));
        Timestamp assignedTs = rs.getTimestamp("assigned_at");
        if (assignedTs != null) a.setAssignedAt(assignedTs.toLocalDateTime());
        Timestamp respTs = rs.getTimestamp("responded_at");
        if (respTs != null) a.setRespondedAt(respTs.toLocalDateTime());
        return a;
    }
}
