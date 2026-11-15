package fastship.service;

import fastship.dao.DatabaseConnection;
import fastship.dao.DeliveryProofDAO;
import fastship.dao.OrderDAO;
import fastship.dao.PaymentDAO;
import fastship.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DeliveryService (Figure 11: Sequence Diagram - UC6 Confirm Delivery and Reconcile COD, FR-13, FR-14, FR-15).
 */
public class DeliveryService {

    private final DeliveryProofDAO deliveryProofDAO = new DeliveryProofDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final TrackingService trackingService = new TrackingService();

    public List<DeliveryProof> getDeliveriesForStaff(Integer driverIdFilter) {
        return deliveryProofDAO.findAllForStaff(driverIdFilter);
    }

    /**
     * UC6 Confirm Delivery (FR-13, Table 22 & Figure 11).
     */
    public void confirmDelivery(int proofId, UserAccount staff) {
        DeliveryProof proof = deliveryProofDAO.findById(proofId);
        if (proof == null) {
            throw new IllegalArgumentException("Delivery proof not found (UC6 - E2).");
        }
        if (proof.getConfirmedBy() != null) {
            throw new IllegalStateException("This delivery proof has already been confirmed.");
        }
        proof.confirm(staff);

        if (!DatabaseConnection.isMySqlAvailable()) {
            try {
                deliveryProofDAO.saveOrUpdate(null, proof);
                trackingService.recordStatus(null, proof.getOrderId(), OrderStatus.DELIVERED,
                        "Delivery proof confirmed by staff " + staff.getFullName(), staff.getFullName());
                return;
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                deliveryProofDAO.saveOrUpdate(conn, proof);
                trackingService.recordStatus(conn, proof.getOrderId(), OrderStatus.DELIVERED,
                        "Delivery proof confirmed by staff " + staff.getFullName(), staff.getFullName());
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error confirming delivery: " + e.getMessage(), e);
        }
    }

    /**
     * UC6 - E1: Staff returns incomplete proof to driver.
     */
    public void returnToDriver(int proofId, String reasonNote, UserAccount staff) {
        DeliveryProof proof = deliveryProofDAO.findById(proofId);
        if (proof == null) {
            throw new IllegalArgumentException("Delivery proof not found.");
        }
        if (reasonNote == null || reasonNote.isBlank()) {
            throw new IllegalArgumentException("Please enter a note explaining why the proof is returned to the driver.");
        }
        proof.returnToDriver("RETURNED TO DRIVER: " + reasonNote.trim());
        try {
            deliveryProofDAO.saveOrUpdate(null, proof);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * UC6 Reconcile COD (FR-14, Table 22 - Main flow steps 4-5, A2 & E3, Figure 11).
     */
    public Payment reconcileCod(int orderId, long cashReceived, String note, UserAccount staff) {
        DeliveryProof proof = deliveryProofDAO.findByOrderId(orderId);
        if (proof == null) {
            throw new IllegalStateException("Order has not been reported as delivered yet (UC6 - E2).");
        }
        if (proof.getConfirmedBy() == null) {
            confirmDelivery(proof.getProofId(), staff);
        }

        Payment payment = paymentDAO.findByOrderId(orderId);
        if (payment == null) {
            if (proof.getCodAmount() <= 0) {
                throw new IllegalStateException("This order has no COD amount to reconcile (UC6 - A1).");
            }
            payment = new Payment();
            payment.setOrderId(orderId);
            payment.setDriverId(proof.getDriverId());
            payment.setCodAmount(proof.getCodAmount());
        }

        if (payment.getStatus() == PaymentStatus.RECONCILED) {
            throw new IllegalStateException("This payment is already reconciled (UC6 - E3).");
        }
        if (cashReceived < 0) {
            throw new IllegalArgumentException("Cash received cannot be negative.");
        }

        boolean isDifference = (cashReceived != payment.getCodAmount());
        if (isDifference && (note == null || note.isBlank())) {
            throw new IllegalArgumentException(String.format(
                    "Cash received (%,d VND) differs from COD amount (%,d VND). A note explaining the difference is required (UC6 - A2)!",
                    cashReceived, payment.getCodAmount()));
        }

        payment.collect(cashReceived);
        payment.reconcile(staff);
        payment.setNote(isDifference ? note.trim() : (note != null && !note.isBlank() ? note.trim() : "Matched"));

        if (!DatabaseConnection.isMySqlAvailable()) {
            try {
                paymentDAO.saveOrUpdate(null, payment);
                return payment;
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                paymentDAO.saveOrUpdate(conn, payment);
                conn.commit();
                return payment;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reconciling COD: " + e.getMessage(), e);
        }
    }

    public List<Order> getAllOrdersForReport() {
        return orderDAO.findAll(null, null, null, null);
    }

    public List<Payment> getAllPaymentsForReport() {
        return paymentDAO.findAll();
    }
}
