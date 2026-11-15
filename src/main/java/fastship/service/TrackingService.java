package fastship.service;

import fastship.dao.AssignmentDAO;
import fastship.dao.OrderDAO;
import fastship.dao.TrackingEventDAO;
import fastship.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TrackingService (Figure 7, 9, 10, 13: Sequence Diagrams, FR-11 & FR-12, UC5).
 */
public class TrackingService {

    private final TrackingEventDAO trackingEventDAO = new TrackingEventDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final NotificationService notificationService = new NotificationService();

    public void recordStatus(Connection conn, int orderId, OrderStatus status, String note, String recordedBy) throws SQLException {
        TrackingEvent event = new TrackingEvent();
        event.setOrderId(orderId);
        event.setStatus(status);
        event.setNote(note);
        event.setRecordedBy(recordedBy);
        event.setEventTime(LocalDateTime.now());
        trackingEventDAO.insert(conn, event);
        notificationService.notify(conn, orderId, status, note);
    }

    /**
     * UC5 Track Order Status (Figure 10):
     * Customers can only look up their own orders; unknown codes and other customers' orders
     * both throw "Order not found" (UC5 - E1 & E2).
     */
    public Order getOrderForTracking(String orderCode, UserAccount actor) {
        int orderId = Order.parseOrderCode(orderCode);
        if (orderId <= 0) {
            throw new IllegalArgumentException("Order not found.");
        }
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found.");
        }
        if (actor != null && actor.getRole() == Role.CUSTOMER) {
            if (actor.getCustomerId() == null || order.getCustomerId() != actor.getCustomerId()) {
                // UC5 - E2: show same message as E1
                throw new IllegalArgumentException("Order not found.");
            }
        }
        Assignment activeAssign = assignmentDAO.findActiveByOrderId(orderId);
        if (activeAssign != null && activeAssign.getDriver() != null) {
            order.setAssignedDriverName(activeAssign.getDriver().getFullName());
        }
        return order;
    }

    public List<TrackingEvent> getTimeline(int orderId) {
        return trackingEventDAO.findByOrderId(orderId);
    }
}
