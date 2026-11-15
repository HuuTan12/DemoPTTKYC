package fastship.dao;

import fastship.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory mirror of the 13 database tables pre-populated with the exact sample data
 * from Chapter 7.3 (Table 41 & Table 42). Used automatically when MySQL 8 is not running locally.
 */
public class InMemoryStore {

    public static final List<Customer> customers = new ArrayList<>();
    public static final List<Address> addresses = new ArrayList<>();
    public static final List<ServiceType> serviceTypes = new ArrayList<>();
    public static final List<Order> orders = new ArrayList<>();
    public static final List<OrderItem> orderItems = new ArrayList<>();
    public static final List<Driver> drivers = new ArrayList<>();
    public static final List<Vehicle> vehicles = new ArrayList<>();
    public static final List<UserAccount> userAccounts = new ArrayList<>();
    public static final List<Assignment> assignments = new ArrayList<>();
    public static final List<TrackingEvent> trackingEvents = new ArrayList<>();
    public static final List<DeliveryProof> deliveryProofs = new ArrayList<>();
    public static final List<Payment> payments = new ArrayList<>();
    public static final List<Notification> notifications = new ArrayList<>();

    private static int addressSeq = 10;
    private static int orderSeq = 1;
    private static int orderItemSeq = 10;
    private static int driverSeq = 10;
    private static int vehicleSeq = 10;
    private static int assignmentSeq = 10;
    private static long eventSeq = 10;
    private static int proofSeq = 10;
    private static int paymentSeq = 10;
    private static int notificationSeq = 10;

    static {
        resetSampleData();
    }

    public static synchronized void resetSampleData() {
        customers.clear();
        addresses.clear();
        serviceTypes.clear();
        orders.clear();
        orderItems.clear();
        drivers.clear();
        vehicles.clear();
        userAccounts.clear();
        assignments.clear();
        trackingEvents.clear();
        deliveryProofs.clear();
        payments.clear();
        notifications.clear();

        // 1. ServiceTypes (Table 41)
        serviceTypes.add(new ServiceType(1, "Standard", 20000, 5000, 30.0, true));
        serviceTypes.add(new ServiceType(2, "Express", 35000, 8000, 20.0, true));

        // 2. Customer (Table 41)
        customers.add(new Customer(1, "Nguyen Van An", "0901234567", "nguyenvanan@email.com"));

        // 3. Addresses (Table 41)
        addresses.add(new Address(1, 1, "Nguyen Van An", "0901234567",
                "19 Nguyen Huu Tho", "Tan Phong Ward", "District 7", "Ho Chi Minh City", "D7"));
        addresses.add(new Address(2, null, "Pham Thu Dung", "0912000111",
                "45 Le Loi", "Ben Thanh Ward", "District 7", "Ho Chi Minh City", "D7"));

        // 4. Drivers & Vehicles (Table 41)
        Driver d1 = new Driver(1, "Le Van Binh", "0903111222", "B2-0451", "D7", DriverStatus.AVAILABLE);
        Vehicle v1 = new Vehicle(1, 1, "59X1-123.45", "Motorbike", 50.0);
        d1.setVehicle(v1);

        Driver d2 = new Driver(2, "Pham Thi Cuc", "0903222333", "B2-0932", "D7", DriverStatus.AVAILABLE);
        Vehicle v2 = new Vehicle(2, 2, "59X2-678.90", "Motorbike", 50.0);
        d2.setVehicle(v2);

        Driver d3 = new Driver(3, "Dang Gia Bao", "0903333444", "C-1187", "D1", DriverStatus.AVAILABLE);
        Vehicle v3 = new Vehicle(3, 3, "51C-456.78", "Van", 500.0);
        d3.setVehicle(v3);

        drivers.add(d1);
        drivers.add(d2);
        drivers.add(d3);
        vehicles.add(v1);
        vehicles.add(v2);
        vehicles.add(v3);

        // 5. UserAccounts (Table 41 - all with demo password 123456)
        userAccounts.add(new UserAccount(1, "customer01", "123456", "Nguyen Van An", Role.CUSTOMER, 1, null, true));
        userAccounts.add(new UserAccount(2, "staff01", "123456", "Tran Thi Lan", Role.STAFF, null, null, true));
        userAccounts.add(new UserAccount(3, "dispatch01", "123456", "Pham Minh Hoang", Role.DISPATCHER, null, null, true));
        userAccounts.add(new UserAccount(4, "driver01", "123456", "Le Van Binh", Role.DRIVER, null, 1, true));
        userAccounts.add(new UserAccount(5, "driver02", "123456", "Pham Thi Cuc", Role.DRIVER, null, 2, true));
        userAccounts.add(new UserAccount(6, "manager01", "123456", "Le Quang", Role.MANAGER, null, null, true));

        // 6. Sample pre-delivered order #1 (for Demo Step 10 - COD Difference check)
        Order sampleOrder = new Order();
        sampleOrder.setOrderId(1);
        sampleOrder.setCustomerId(1);
        sampleOrder.setPickupAddressId(1);
        sampleOrder.setDeliveryAddressId(2);
        sampleOrder.setServiceTypeId(1);
        sampleOrder.setTotalWeightKg(2.0);
        sampleOrder.setShippingFee(30000);
        sampleOrder.setCodAmount(450000);
        sampleOrder.setStatus(OrderStatus.DELIVERED);
        sampleOrder.setNote("Sample delivered order for Step 10 COD Difference test");
        sampleOrder.setCreatedAt(LocalDateTime.now().minusHours(2));
        sampleOrder.setConfirmedAt(LocalDateTime.now().minusMinutes(110));
        sampleOrder.setPickupAddress(addresses.get(0));
        sampleOrder.setDeliveryAddress(addresses.get(1));
        sampleOrder.setServiceType(serviceTypes.get(0));
        sampleOrder.setAssignedDriverName("Le Van Binh");

        OrderItem item1 = new OrderItem(1, 1, "Headphones Box", 1, 2.0);
        sampleOrder.getItems().add(item1);
        orderItems.add(item1);
        orders.add(sampleOrder);

        Assignment a1 = new Assignment();
        a1.setAssignmentId(1);
        a1.setOrderId(1);
        a1.setDriverId(1);
        a1.setAssignedBy(3);
        a1.setAttemptNo(1);
        a1.setStatus(AssignmentStatus.COMPLETED);
        a1.setAssignedAt(LocalDateTime.now().minusMinutes(100));
        a1.setRespondedAt(LocalDateTime.now().minusMinutes(95));
        a1.setOrder(sampleOrder);
        a1.setDriver(d1);
        assignments.add(a1);

        trackingEvents.add(new TrackingEvent(1, 1, OrderStatus.NEW, "Order created by customer", "customer01", LocalDateTime.now().minusMinutes(120)));
        trackingEvents.add(new TrackingEvent(2, 1, OrderStatus.CONFIRMED, "Order confirmed by staff", "staff01", LocalDateTime.now().minusMinutes(110)));
        trackingEvents.add(new TrackingEvent(3, 1, OrderStatus.ASSIGNED, "Assigned to driver Le Van Binh", "dispatch01", LocalDateTime.now().minusMinutes(100)));
        trackingEvents.add(new TrackingEvent(4, 1, OrderStatus.PICKED_UP, "Driver picked up parcel", "driver01", LocalDateTime.now().minusMinutes(80)));
        trackingEvents.add(new TrackingEvent(5, 1, OrderStatus.IN_TRANSIT, "Parcel is in transit", "driver01", LocalDateTime.now().minusMinutes(50)));
        trackingEvents.add(new TrackingEvent(6, 1, OrderStatus.DELIVERED, "Driver reported parcel delivered", "driver01", LocalDateTime.now().minusMinutes(20)));

        DeliveryProof dp = new DeliveryProof();
        dp.setProofId(1);
        dp.setOrderId(1);
        dp.setReceivedBy("Pham Thu Dung");
        dp.setDeliveredAt(LocalDateTime.now().minusMinutes(20));
        dp.setNote("Left with receiver at front desk");
        dp.setPhotoRef("proof_DH0001.jpg");
        dp.setDriverId(1);
        dp.setDriverName("Le Van Binh");
        dp.setCodAmount(450000);

        Payment p1 = new Payment();
        p1.setPaymentId(1);
        p1.setOrderId(1);
        p1.setDriverId(1);
        p1.setCodAmount(450000);
        p1.setStatus(PaymentStatus.PENDING);
        dp.setPayment(p1);

        deliveryProofs.add(dp);
        payments.add(p1);

        orderSeq = 2;
    }

    public static synchronized int nextAddressId() { return ++addressSeq; }
    public static synchronized int nextOrderId() { return orderSeq++; }
    public static synchronized int nextOrderItemId() { return ++orderItemSeq; }
    public static synchronized int nextDriverId() { return ++driverSeq; }
    public static synchronized int nextVehicleId() { return ++vehicleSeq; }
    public static synchronized int nextAssignmentId() { return ++assignmentSeq; }
    public static synchronized long nextEventId() { return ++eventSeq; }
    public static synchronized int nextProofId() { return ++proofSeq; }
    public static synchronized int nextPaymentId() { return ++paymentSeq; }
    public static synchronized int nextNotificationId() { return ++notificationSeq; }
}
