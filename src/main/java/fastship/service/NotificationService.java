package fastship.service;

import fastship.dao.NotificationDAO;
import fastship.model.Notification;
import fastship.model.Order;
import fastship.model.OrderStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * NotificationService (Figure 11 & Figure 13: Sequence Diagrams, FR-11).
 */
public class NotificationService {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    public void notify(Connection conn, int orderId, OrderStatus status, String detail) throws SQLException {
        Notification n = new Notification();
        n.setOrderId(orderId);
        n.setChannel("IN_APP");
        String code = String.format("DH%04d", orderId);
        String msg = "Order " + code + " status changed to " + status.name();
        if (detail != null && !detail.isBlank()) {
            msg += " (" + detail + ")";
        }
        n.setMessage(msg);
        n.setSentAt(LocalDateTime.now());
        n.setRead(false);
        notificationDAO.insert(conn, n);
    }

    public List<Notification> getCustomerNotifications(int customerId) {
        return notificationDAO.findByCustomerId(customerId);
    }
}
