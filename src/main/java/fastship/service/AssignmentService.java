package fastship.service;

import fastship.dao.*;
import fastship.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AssignmentService (Figure 8 & Figure 9: Sequence Diagrams - UC3 Assign Driver & UC4 Driver Accepts and Delivers Order).
 */
public class AssignmentService {

    private final OrderDAO orderDAO = new OrderDAO();
    private final DriverDAO driverDAO = new DriverDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final DeliveryProofDAO deliveryProofDAO = new DeliveryProofDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final TrackingService trackingService = new TrackingService();

    /**
     * UC3 View Pending Orders (FR-08, Figure 8).
     */
    public List<Order> getPendingOrders(String area) {
        return orderDAO.findPendingForAssignment(area);
    }

    public int getNextAttemptNumber(int orderId) {
        try {
            return assignmentDAO.getNextAttemptNo(null, orderId);
        } catch (SQLException e) {
            return 1;
        }
    }

    /**
     * UC3 Assign Driver (FR-09, Table 19 & Figure 8).
     * Creates a PENDING assignment, sets Order to ASSIGNED, and adds a TrackingEvent in one transaction.
     */
    public Assignment assignOrder(int orderId, int driverId, UserAccount dispatcher) {
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found.");
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED orders can be assigned (UC3 - E2).");
        }
        Driver driver = driverDAO.findById(driverId);
        if (driver == null || !driver.isAvailable()) {
            throw new IllegalStateException("Selected driver is not available (UC3 - E1).");
        }

        if (!DatabaseConnection.isMySqlAvailable()) {
            try {
                if (assignmentDAO.hasActiveAssignment(null, orderId)) {
                    throw new IllegalStateException("Order already has an active assignment (UC3 - E2).");
                }
                int attemptNo = assignmentDAO.getNextAttemptNo(null, orderId);
                Assignment a = new Assignment();
                a.setOrderId(orderId);
                a.setDriverId(driverId);
                a.setAssignedBy(dispatcher.getUserId());
                a.setAttemptNo(attemptNo);
                a.setStatus(AssignmentStatus.PENDING);
                a.setAssignedAt(LocalDateTime.now());
                assignmentDAO.insert(null, a);

                orderDAO.updateStatus(null, orderId, OrderStatus.ASSIGNED, null, null);
                trackingService.recordStatus(null, orderId, OrderStatus.ASSIGNED,
                        "Assigned to " + driver.getFullName() + " (Attempt " + attemptNo + ")",
                        dispatcher.getFullName());
                return a;
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (assignmentDAO.hasActiveAssignment(conn, orderId)) {
                    throw new IllegalStateException("Order already has an active assignment (UC3 - E2).");
                }
                int attemptNo = assignmentDAO.getNextAttemptNo(conn, orderId);
                Assignment a = new Assignment();
                a.setOrderId(orderId);
                a.setDriverId(driverId);
                a.setAssignedBy(dispatcher.getUserId());
                a.setAttemptNo(attemptNo);
                a.setStatus(AssignmentStatus.PENDING);
                a.setAssignedAt(LocalDateTime.now());
                assignmentDAO.insert(conn, a);

                orderDAO.updateStatus(conn, orderId, OrderStatus.ASSIGNED, null, null);
                trackingService.recordStatus(conn, orderId, OrderStatus.ASSIGNED,
                        "Assigned to " + driver.getFullName() + " (Attempt " + attemptNo + ")",
                        dispatcher.getFullName());
                conn.commit();
                return a;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error assigning driver: " + e.getMessage(), e);
        }
    }

    public List<Assignment> getAssignmentsForDriver(int driverId) {
        return assignmentDAO.findByDriverId(driverId);
    }

    /**
     * UC4 Driver accepts order (FR-10, Table 20 & Figure 9):
     * Sets Assignment to ACCEPTED and Driver to BUSY.
     */
    public void accept(int assignmentId, UserAccount driverAccount) {
        Assignment a = assignmentDAO.findById(assignmentId);
        if (a == null) {
            throw new IllegalArgumentException("Assignment not found.");
        }
        if (a.getStatus() != AssignmentStatus.PENDING) {
            throw new IllegalStateException("Assignment is no longer pending (UC4 - E2).");
        }
        a.accept();

        if (!DatabaseConnection.isMySqlAvailable()) {
            try {
                assignmentDAO.updateStatus(null, assignmentId, AssignmentStatus.ACCEPTED, null, a.getRespondedAt());
                driverDAO.updateStatus(null, a.getDriverId(), DriverStatus.BUSY);
                return;
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                assignmentDAO.updateStatus(conn, assignmentId, AssignmentStatus.ACCEPTED, null, a.getRespondedAt());
                driverDAO.updateStatus(conn, a.getDriverId(), DriverStatus.BUSY);
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error accepting assignment: " + e.getMessage(), e);
        }
    }

    /**
     * UC4 Driver rejects order with reason (FR-10, Table 20 - A1 & E1, Figure 9):
     * Assignment becomes REJECTED, Order returns to CONFIRMED (pending-assignment list).
     */
    public void reject(int assignmentId, String reason, UserAccount driverAccount) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required (UC4 - E1).");
        }
        Assignment a = assignmentDAO.findById(assignmentId);
        if (a == null) {
            throw new IllegalArgumentException("Assignment not found.");
        }
        if (a.getStatus() != AssignmentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING assignments can be rejected.");
        }
        a.reject(reason);

        if (!DatabaseConnection.isMySqlAvailable()) {
            try {
                assignmentDAO.updateStatus(null, assignmentId, AssignmentStatus.REJECTED, reason.trim(), a.getRespondedAt());
                orderDAO.updateStatus(null, a.getOrderId(), OrderStatus.CONFIRMED, null, null);
                trackingService.recordStatus(null, a.getOrderId(), OrderStatus.CONFIRMED,
                        "Driver rejected (" + reason.trim() + "); returned to pending list",
                        driverAccount.getFullName());
                return;
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                assignmentDAO.updateStatus(conn, assignmentId, AssignmentStatus.REJECTED, reason.trim(), a.getRespondedAt());
                orderDAO.updateStatus(conn, a.getOrderId(), OrderStatus.CONFIRMED, null, null);
                trackingService.recordStatus(conn, a.getOrderId(), OrderStatus.CONFIRMED,
                        "Driver rejected (" + reason.trim() + "); returned to pending list",
                        driverAccount.getFullName());
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error rejecting assignment: " + e.getMessage(), e);
        }
    }

    /**
     * UC4 Driver updates delivery progress (FR-10, Table 20 - Main flow steps 4-6 & E3).
     * Enforces strict sequence: ASSIGNED (accepted) -> PICKED_UP -> IN_TRANSIT -> DELIVERED / FAILED.
     */
    public void updateDeliveryStatus(int assignmentId, OrderStatus targetStatus,
                                     String receivedBy, String note, String photoRef,
                                     UserAccount driverAccount) {
        Assignment a = assignmentDAO.findById(assignmentId);
        if (a == null) {
            throw new IllegalArgumentException("Assignment not found.");
        }
        if (a.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new IllegalStateException("You must accept the assignment before updating delivery progress (UC4 - E3).");
        }
        Order order = orderDAO.findById(a.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("Order not found.");
        }

        OrderStatus current = order.getStatus();
        validateStatusTransition(current, targetStatus);

        if (targetStatus == OrderStatus.DELIVERED && (receivedBy == null || receivedBy.isBlank())) {
            throw new IllegalArgumentException("Please enter Receiver Name in Delivery Proof before marking Delivered (FR-13).");
        }
        if (targetStatus == OrderStatus.FAILED && (note == null || note.isBlank())) {
            throw new IllegalArgumentException("Please enter the failure reason in Note when reporting Delivery Failed (UC4 - A2).");
        }

        if (!DatabaseConnection.isMySqlAvailable()) {
            try {
                applyDeliveryStatusUpdate(null, a, order, targetStatus, receivedBy, note, photoRef, driverAccount);
                return;
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                applyDeliveryStatusUpdate(conn, a, order, targetStatus, receivedBy, note, photoRef, driverAccount);
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating delivery status: " + e.getMessage(), e);
        }
    }

    private void applyDeliveryStatusUpdate(Connection conn, Assignment a, Order order, OrderStatus targetStatus,
                                           String receivedBy, String note, String photoRef,
                                           UserAccount driverAccount) throws SQLException {
        orderDAO.updateStatus(conn, order.getOrderId(), targetStatus, null, null);
        String eventNote = (note != null && !note.isBlank()) ? note.trim() : targetStatus.name();
        trackingService.recordStatus(conn, order.getOrderId(), targetStatus, eventNote, driverAccount.getFullName());

        if (targetStatus == OrderStatus.DELIVERED) {
            assignmentDAO.updateStatus(conn, a.getAssignmentId(), AssignmentStatus.COMPLETED, null, null);
            driverDAO.updateStatus(conn, a.getDriverId(), DriverStatus.AVAILABLE);

            DeliveryProof proof = new DeliveryProof();
            proof.setOrderId(order.getOrderId());
            proof.setReceivedBy(receivedBy.trim());
            proof.setDeliveredAt(LocalDateTime.now());
            proof.setNote(note != null ? note.trim() : "Delivered to receiver");
            proof.setPhotoRef(photoRef != null && !photoRef.isBlank()
                    ? photoRef.trim() : "proof_" + order.getOrderCode() + ".jpg");
            proof.setDriverId(a.getDriverId());
            proof.setDriverName(driverAccount.getFullName());
            proof.setCodAmount(order.getCodAmount());
            deliveryProofDAO.saveOrUpdate(conn, proof);

            if (order.getCodAmount() > 0) {
                Payment payment = new Payment();
                payment.setOrderId(order.getOrderId());
                payment.setDriverId(a.getDriverId());
                payment.setCodAmount(order.getCodAmount());
                payment.setStatus(PaymentStatus.PENDING);
                paymentDAO.saveOrUpdate(conn, payment);
                proof.setPayment(payment);
            }
        } else if (targetStatus == OrderStatus.FAILED) {
            assignmentDAO.updateStatus(conn, a.getAssignmentId(), AssignmentStatus.COMPLETED, null, null);
            driverDAO.updateStatus(conn, a.getDriverId(), DriverStatus.AVAILABLE);
        }
    }

    /**
     * UC4 - E3: A status change out of sequence (for example skipping "Picked up") is refused.
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        if (target == OrderStatus.PICKED_UP && current != OrderStatus.ASSIGNED) {
            throw new IllegalStateException("Invalid status sequence: Order must be ASSIGNED before marking PICKED_UP (UC4 - E3).");
        }
        if (target == OrderStatus.IN_TRANSIT && current != OrderStatus.PICKED_UP) {
            throw new IllegalStateException("Invalid status sequence: Cannot skip PICKED_UP! Mark PICKED_UP before IN_TRANSIT (UC4 - E3).");
        }
        if ((target == OrderStatus.DELIVERED || target == OrderStatus.FAILED) && current != OrderStatus.IN_TRANSIT) {
            throw new IllegalStateException("Invalid status sequence: Order must be IN_TRANSIT before marking " + target + " (UC4 - E3).");
        }
    }
}
