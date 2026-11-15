package fastship.dao;

import fastship.model.Address;
import fastship.model.Order;
import fastship.model.OrderStatus;
import fastship.model.ServiceType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DAO for physical table `orders` (Table 27 & Table 40).
 */
public class OrderDAO {

    private final AddressDAO addressDAO = new AddressDAO();
    private final ServiceTypeDAO serviceTypeDAO = new ServiceTypeDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();

    public int insert(Connection conn, Order order) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            order.setOrderId(InMemoryStore.nextOrderId());
            if (order.getCreatedAt() == null) {
                order.setCreatedAt(LocalDateTime.now());
            }
            InMemoryStore.orders.add(order);
            return order.getOrderId();
        }
        String sql = "INSERT INTO orders (customer_id, pickup_address_id, delivery_address_id, service_type_id, " +
                     "total_weight_kg, shipping_fee, cod_amount, status, note, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getCustomerId());
            ps.setInt(2, order.getPickupAddressId());
            ps.setInt(3, order.getDeliveryAddressId());
            ps.setInt(4, order.getServiceTypeId());
            ps.setDouble(5, order.getTotalWeightKg());
            ps.setLong(6, order.getShippingFee());
            ps.setLong(7, order.getCodAmount());
            ps.setString(8, order.getStatus().name());
            ps.setString(9, order.getNote());
            ps.setTimestamp(10, Timestamp.valueOf(
                    order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    order.setOrderId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to insert Order");
    }

    public Order findById(int orderId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Order found = InMemoryStore.orders.stream()
                    .filter(o -> o.getOrderId() == orderId)
                    .findFirst()
                    .orElse(null);
            if (found != null) {
                enrichOrder(found);
            }
            return found;
        }
        String sql = "SELECT * FROM orders WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order o = mapRow(rs);
                    enrichOrder(o);
                    return o;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding Order: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Order> findAll(OrderStatus statusFilter, String areaFilter, Integer customerIdFilter, String searchKeyword) {
        List<Order> result = new ArrayList<>();
        if (!DatabaseConnection.isMySqlAvailable()) {
            for (Order o : InMemoryStore.orders) {
                enrichOrder(o);
                if (customerIdFilter != null && o.getCustomerId() != customerIdFilter) continue;
                if (statusFilter != null && o.getStatus() != statusFilter) continue;
                if (areaFilter != null && !areaFilter.isBlank() && !"ALL".equalsIgnoreCase(areaFilter)) {
                    String area = o.getDeliveryAddress() != null ? o.getDeliveryAddress().getArea() : "";
                    if (!areaFilter.equalsIgnoreCase(area)) continue;
                }
                if (searchKeyword != null && !searchKeyword.isBlank()) {
                    String kw = searchKeyword.trim().toLowerCase();
                    String code = o.getOrderCode().toLowerCase();
                    String recvPhone = o.getDeliveryAddress() != null && o.getDeliveryAddress().getPhone() != null
                            ? o.getDeliveryAddress().getPhone().toLowerCase() : "";
                    String recvName = o.getDeliveryAddress() != null && o.getDeliveryAddress().getContactName() != null
                            ? o.getDeliveryAddress().getContactName().toLowerCase() : "";
                    if (!code.contains(kw) && !recvPhone.contains(kw) && !recvName.contains(kw)
                            && !String.valueOf(o.getOrderId()).equals(kw)) {
                        continue;
                    }
                }
                result.add(o);
            }
            result.sort(Comparator.comparingInt(Order::getOrderId).reversed());
            return result;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT o.* FROM orders o " +
                "JOIN Address da ON o.delivery_address_id = da.address_id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (customerIdFilter != null) {
            sql.append("AND o.customer_id = ? ");
            params.add(customerIdFilter);
        }
        if (statusFilter != null) {
            sql.append("AND o.status = ? ");
            params.add(statusFilter.name());
        }
        if (areaFilter != null && !areaFilter.isBlank() && !"ALL".equalsIgnoreCase(areaFilter)) {
            sql.append("AND da.area = ? ");
            params.add(areaFilter);
        }
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            sql.append("AND (CAST(o.order_id AS CHAR) LIKE ? OR da.phone LIKE ? OR da.contact_name LIKE ?) ");
            String kw = "%" + searchKeyword.trim().replace("DH", "").replace("ORD-", "") + "%";
            params.add(kw);
            params.add("%" + searchKeyword.trim() + "%");
            params.add("%" + searchKeyword.trim() + "%");
        }
        sql.append("ORDER BY o.order_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = mapRow(rs);
                    enrichOrder(o);
                    result.add(o);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying orders: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Pending orders for Dispatcher (FR-08, UC3): confirmed orders without an active (PENDING or ACCEPTED) assignment.
     */
    public List<Order> findPendingForAssignment(String areaFilter) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            List<Order> result = new ArrayList<>();
            for (Order o : InMemoryStore.orders) {
                enrichOrder(o);
                if (o.getStatus() != OrderStatus.CONFIRMED) continue;
                boolean hasActive = InMemoryStore.assignments.stream()
                        .anyMatch(a -> a.getOrderId() == o.getOrderId()
                                && (a.getStatus() == fastship.model.AssignmentStatus.PENDING
                                || a.getStatus() == fastship.model.AssignmentStatus.ACCEPTED));
                if (hasActive) continue;
                if (areaFilter != null && !areaFilter.isBlank() && !"ALL".equalsIgnoreCase(areaFilter)) {
                    String area = o.getDeliveryAddress() != null ? o.getDeliveryAddress().getArea() : "";
                    if (!areaFilter.equalsIgnoreCase(area)) continue;
                }
                result.add(o);
            }
            return result;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT o.* FROM orders o " +
                "JOIN Address da ON o.delivery_address_id = da.address_id " +
                "WHERE o.status = 'CONFIRMED' " +
                "AND NOT EXISTS (" +
                "  SELECT 1 FROM Assignment a WHERE a.order_id = o.order_id AND a.status IN ('PENDING', 'ACCEPTED')" +
                ") ");
        if (areaFilter != null && !areaFilter.isBlank() && !"ALL".equalsIgnoreCase(areaFilter)) {
            sql.append("AND da.area = ? ");
        }
        sql.append("ORDER BY o.order_id ASC");

        List<Order> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (areaFilter != null && !areaFilter.isBlank() && !"ALL".equalsIgnoreCase(areaFilter)) {
                ps.setString(1, areaFilter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = mapRow(rs);
                    enrichOrder(o);
                    result.add(o);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying pending orders: " + e.getMessage(), e);
        }
        return result;
    }

    public void updateStatus(Connection conn, int orderId, OrderStatus status, LocalDateTime confirmedAt, String cancelReason) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Order o = findById(orderId);
            if (o != null) {
                o.setStatus(status);
                if (confirmedAt != null) o.setConfirmedAt(confirmedAt);
                if (cancelReason != null) o.setCancelReason(cancelReason);
            }
            return;
        }
        String sql = "UPDATE orders SET status = ?, confirmed_at = COALESCE(?, confirmed_at), cancel_reason = COALESCE(?, cancel_reason) WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            if (confirmedAt != null) {
                ps.setTimestamp(2, Timestamp.valueOf(confirmedAt));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            if (cancelReason != null) {
                ps.setString(3, cancelReason);
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setInt(4, orderId);
            ps.executeUpdate();
        }
    }

    public void updateOrderDetails(Order order) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Order existing = findById(order.getOrderId());
            if (existing != null) {
                existing.setNote(order.getNote());
                existing.setCodAmount(order.getCodAmount());
                existing.setServiceTypeId(order.getServiceTypeId());
                existing.setShippingFee(order.getShippingFee());
            }
            return;
        }
        String sql = "UPDATE orders SET service_type_id = ?, shipping_fee = ?, cod_amount = ?, note = ? WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, order.getServiceTypeId());
            ps.setLong(2, order.getShippingFee());
            ps.setLong(3, order.getCodAmount());
            ps.setString(4, order.getNote());
            ps.setInt(5, order.getOrderId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating order details: " + e.getMessage(), e);
        }
    }

    private void enrichOrder(Order o) {
        if (o.getPickupAddress() == null || DatabaseConnection.isMySqlAvailable()) {
            o.setPickupAddress(addressDAO.findById(o.getPickupAddressId()));
        }
        if (o.getDeliveryAddress() == null || DatabaseConnection.isMySqlAvailable()) {
            o.setDeliveryAddress(addressDAO.findById(o.getDeliveryAddressId()));
        }
        if (o.getServiceType() == null || DatabaseConnection.isMySqlAvailable()) {
            o.setServiceType(serviceTypeDAO.findById(o.getServiceTypeId()));
        }
        o.setItems(orderItemDAO.findByOrderId(o.getOrderId()));
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id"));
        o.setCustomerId(rs.getInt("customer_id"));
        o.setPickupAddressId(rs.getInt("pickup_address_id"));
        o.setDeliveryAddressId(rs.getInt("delivery_address_id"));
        o.setServiceTypeId(rs.getInt("service_type_id"));
        o.setTotalWeightKg(rs.getDouble("total_weight_kg"));
        o.setShippingFee(rs.getLong("shipping_fee"));
        o.setCodAmount(rs.getLong("cod_amount"));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setNote(rs.getString("note"));
        o.setCancelReason(rs.getString("cancel_reason"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) o.setCreatedAt(createdTs.toLocalDateTime());
        Timestamp confirmedTs = rs.getTimestamp("confirmed_at");
        if (confirmedTs != null) o.setConfirmedAt(confirmedTs.toLocalDateTime());
        return o;
    }
}
