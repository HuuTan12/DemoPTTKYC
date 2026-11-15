package fastship.service;

import fastship.dao.*;
import fastship.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderService (Figure 6 & Figure 7: Sequence Diagrams - UC1 Create Order & UC2 Confirm/Cancel Order).
 */
public class OrderService {

    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final AddressDAO addressDAO = new AddressDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final DriverDAO driverDAO = new DriverDAO();
    private final PricingService pricingService = new PricingService();
    private final TrackingService trackingService = new TrackingService();

    public Customer getCustomer(int customerId) {
        return customerDAO.findById(customerId);
    }

    public List<Address> getSavedAddresses(int customerId) {
        return addressDAO.findByCustomerId(customerId);
    }

    public long calculateFee(int serviceTypeId, double totalWeightKg) {
        ServiceType st = pricingService.getServiceType(serviceTypeId);
        return pricingService.getFee(st, totalWeightKg);
    }

    /**
     * UC1 Create Order (FR-01, FR-02, Table 17 & Figure 6).
     */
    public Order createOrder(int customerId,
                             Address pickupAddress,
                             Address deliveryAddress,
                             List<OrderItem> items,
                             int serviceTypeId,
                             long codAmount,
                             String note,
                             String recordedBy) {
        validateOrderInput(pickupAddress, deliveryAddress, items, codAmount);

        double totalWeight = items.stream()
                .mapToDouble(OrderItem::getWeightKg)
                .sum();
        ServiceType serviceType = pricingService.getServiceType(serviceTypeId);
        long shippingFee = pricingService.getFee(serviceType, totalWeight);

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setServiceTypeId(serviceTypeId);
        order.setServiceType(serviceType);
        order.setTotalWeightKg(totalWeight);
        order.setShippingFee(shippingFee);
        order.setCodAmount(codAmount);
        order.setStatus(OrderStatus.NEW);
        order.setNote(note);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(items);

        if (!DatabaseConnection.isMySqlAvailable()) {
            try {
                if (pickupAddress.getAddressId() <= 0) {
                    addressDAO.insert(null, pickupAddress);
                }
                addressDAO.insert(null, deliveryAddress);
                order.setPickupAddressId(pickupAddress.getAddressId());
                order.setDeliveryAddressId(deliveryAddress.getAddressId());
                order.setPickupAddress(pickupAddress);
                order.setDeliveryAddress(deliveryAddress);

                int orderId = orderDAO.insert(null, order);
                orderItemDAO.insertItems(null, orderId, items);
                trackingService.recordStatus(null, orderId, OrderStatus.NEW, "created by customer", recordedBy);
                return order;
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (pickupAddress.getAddressId() <= 0) {
                    addressDAO.insert(conn, pickupAddress);
                }
                addressDAO.insert(conn, deliveryAddress);
                order.setPickupAddressId(pickupAddress.getAddressId());
                order.setDeliveryAddressId(deliveryAddress.getAddressId());
                order.setPickupAddress(pickupAddress);
                order.setDeliveryAddress(deliveryAddress);

                int orderId = orderDAO.insert(conn, order);
                orderItemDAO.insertItems(conn, orderId, items);
                trackingService.recordStatus(conn, orderId, OrderStatus.NEW, "created by customer", recordedBy);
                conn.commit();
                return order;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save order: " + e.getMessage(), e);
        }
    }

    private void validateOrderInput(Address pickup, Address delivery, List<OrderItem> items, long codAmount) {
        if (pickup == null || isBlank(pickup.getContactName()) || isBlank(pickup.getPhone()) || isBlank(pickup.getStreet())) {
            throw new IllegalArgumentException("Sender name, phone, and pickup address are required (UC1 - E1).");
        }
        if (!isValidPhone(pickup.getPhone())) {
            throw new IllegalArgumentException("Sender phone must be 10 digits starting with 0 (UC1 - E1).");
        }
        if (delivery == null || isBlank(delivery.getContactName()) || isBlank(delivery.getPhone())
                || isBlank(delivery.getStreet()) || isBlank(delivery.getDistrict())) {
            throw new IllegalArgumentException("Receiver name, phone, street, and district are required (UC1 - E1).");
        }
        if (!isValidPhone(delivery.getPhone())) {
            throw new IllegalArgumentException("Receiver phone must be 10 digits starting with 0 (UC1 - E1).");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one parcel item is required (UC1 - E1).");
        }
        for (OrderItem item : items) {
            if (isBlank(item.getDescription())) {
                throw new IllegalArgumentException("Item description cannot be empty.");
            }
            if (item.getQuantity() <= 0 || item.getWeightKg() <= 0) {
                throw new IllegalArgumentException("Item quantity and weight must be greater than 0.");
            }
        }
        if (codAmount < 0) {
            throw new IllegalArgumentException("COD amount cannot be negative.");
        }
    }

    private boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String cleaned = phone.replaceAll("\\s+", "");
        return cleaned.matches("^0\\d{9}$");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public List<Order> getOrders(OrderStatus statusFilter, String areaFilter, Integer customerIdFilter, String searchKeyword) {
        List<Order> list = orderDAO.findAll(statusFilter, areaFilter, customerIdFilter, searchKeyword);
        for (Order o : list) {
            Assignment active = assignmentDAO.findActiveByOrderId(o.getOrderId());
            if (active != null && active.getDriver() != null) {
                o.setAssignedDriverName(active.getDriver().getFullName());
            }
        }
        return list;
    }

    public Order getOrderById(int orderId) {
        Order o = orderDAO.findById(orderId);
        if (o != null) {
            Assignment active = assignmentDAO.findActiveByOrderId(orderId);
            if (active != null && active.getDriver() != null) {
                o.setAssignedDriverName(active.getDriver().getFullName());
            }
        }
        return o;
    }

    /**
     * UC2 Confirm Order (FR-04, Table 18 & Figure 7).
     */
    public void confirmOrder(int orderId, UserAccount staff) {
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found.");
        }
        if (order.getStatus() != OrderStatus.NEW) {
            throw new IllegalStateException("Order was already confirmed or cancelled (UC2 - E1).");
        }
        order.confirm();

        if (!DatabaseConnection.isMySqlAvailable()) {
            try {
                orderDAO.updateStatus(null, orderId, OrderStatus.CONFIRMED, order.getConfirmedAt(), null);
                trackingService.recordStatus(null, orderId, OrderStatus.CONFIRMED,
                        "Confirmed by " + staff.getFullName(), staff.getFullName());
                return;
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                orderDAO.updateStatus(conn, orderId, OrderStatus.CONFIRMED, order.getConfirmedAt(), null);
                trackingService.recordStatus(conn, orderId, OrderStatus.CONFIRMED,
                        "Confirmed by " + staff.getFullName(), staff.getFullName());
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error confirming order: " + e.getMessage(), e);
        }
    }

    /**
     * UC2 Edit Order before assignment (FR-04, Table 18 - A1).
     */
    public void editOrderBeforeAssignment(int orderId, String receiverName, String receiverPhone,
                                          String deliveryStreet, String deliveryArea,
                                          int serviceTypeId, long codAmount, String note) {
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found.");
        }
        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order can only be edited before it is assigned to a driver (FR-04).");
        }
        if (!isValidPhone(receiverPhone)) {
            throw new IllegalArgumentException("Receiver phone must be 10 digits starting with 0.");
        }
        ServiceType st = pricingService.getServiceType(serviceTypeId);
        long newFee = pricingService.getFee(st, order.getTotalWeightKg());

        Address deliv = order.getDeliveryAddress();
        if (deliv != null) {
            deliv.setContactName(receiverName.trim());
            deliv.setPhone(receiverPhone.trim().replaceAll("\\s+", ""));
            deliv.setStreet(deliveryStreet.trim());
            deliv.setArea(deliveryArea.trim());
            addressDAO.update(deliv);
        }
        order.setServiceTypeId(serviceTypeId);
        order.setServiceType(st);
        order.setShippingFee(newFee);
        order.setCodAmount(codAmount);
        order.setNote(note);
        orderDAO.updateOrderDetails(order);
    }

    /**
     * UC2 Cancel Order (FR-05, Table 18 - A2 & A3, Figure 7).
     * If the order is already assigned, the active assignment is cancelled and the driver is freed.
     */
    public void cancelOrder(int orderId, String reason, UserAccount actor) {
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found.");
        }
        if (!order.canCancel()) {
            throw new IllegalStateException("Cannot cancel an order that has already been picked up or delivered (UC2 - E2).");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required (UC2 - E3).");
        }
        order.cancel(reason);

        Assignment activeAssignment = assignmentDAO.findActiveByOrderId(orderId);

        if (!DatabaseConnection.isMySqlAvailable()) {
            try {
                orderDAO.updateStatus(null, orderId, OrderStatus.CANCELLED, null, reason.trim());
                if (activeAssignment != null && (activeAssignment.getStatus() == AssignmentStatus.PENDING
                        || activeAssignment.getStatus() == AssignmentStatus.ACCEPTED)) {
                    assignmentDAO.updateStatus(null, activeAssignment.getAssignmentId(),
                            AssignmentStatus.CANCELLED, "Order cancelled: " + reason.trim(), LocalDateTime.now());
                    driverDAO.updateStatus(null, activeAssignment.getDriverId(), DriverStatus.AVAILABLE);
                }
                trackingService.recordStatus(null, orderId, OrderStatus.CANCELLED,
                        "Cancelled (" + reason.trim() + ")", actor.getFullName());
                return;
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                orderDAO.updateStatus(conn, orderId, OrderStatus.CANCELLED, null, reason.trim());
                if (activeAssignment != null && (activeAssignment.getStatus() == AssignmentStatus.PENDING
                        || activeAssignment.getStatus() == AssignmentStatus.ACCEPTED)) {
                    assignmentDAO.updateStatus(conn, activeAssignment.getAssignmentId(),
                            AssignmentStatus.CANCELLED, "Order cancelled: " + reason.trim(), LocalDateTime.now());
                    driverDAO.updateStatus(conn, activeAssignment.getDriverId(), DriverStatus.AVAILABLE);
                }
                trackingService.recordStatus(conn, orderId, OrderStatus.CANCELLED,
                        "Cancelled (" + reason.trim() + ")", actor.getFullName());
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error cancelling order: " + e.getMessage(), e);
        }
    }
}
