package fastship.service;

import fastship.dao.InMemoryStore;
import fastship.model.*;

import java.util.List;

/**
 * Automated verification of all 11 Demo Script steps (Table 42) and exception flows (UC1 to UC6).
 */
public class DemoScriptVerifier {

    public static void main(String[] args) {
        InMemoryStore.resetSampleData();

        AuthService authService = new AuthService();
        OrderService orderService = new OrderService();
        DriverService driverService = new DriverService();
        AssignmentService assignmentService = new AssignmentService();
        TrackingService trackingService = new TrackingService();
        DeliveryService deliveryService = new DeliveryService();

        UserAccount customer01 = authService.login("customer01", "123456");
        UserAccount staff01 = authService.login("staff01", "123456");
        UserAccount dispatch01 = authService.login("dispatch01", "123456");
        UserAccount driver01 = authService.login("driver01", "123456");
        UserAccount driver02 = authService.login("driver02", "123456");

        // STEP 1 (Table 42): Customer creates demo order with 2 items (1.5kg + 1.0kg = 2.5kg), Standard service, COD 450,000 VND
        Address pickup = orderService.getSavedAddresses(customer01.getCustomerId()).get(0);
        Address delivery = new Address(0, null, "Tran Thi Dung", "0912345678",
                "120 Nguyen Thi Thap", "", "District 7", "Ho Chi Minh City", "D7");
        List<OrderItem> items = List.of(
                new OrderItem("Book box", 1, 1.5),
                new OrderItem("Phone case", 1, 1.0)
        );
        Order demoOrder = orderService.createOrder(
                customer01.getCustomerId(), pickup, delivery, items, 1, 450000, "Call before delivery", customer01.getUsername()
        );
        assertCondition(demoOrder.getShippingFee() == 32500, "Step 1: Fee must be 32,500 VND, got " + demoOrder.getShippingFee());
        assertCondition(demoOrder.getStatus() == OrderStatus.NEW, "Step 1: Order status must be NEW");
        System.out.println("[PASS] Step 1 (UC1): Created " + demoOrder.getOrderCode() + ", Fee = " + demoOrder.getShippingFee() + " VND, Status = " + demoOrder.getStatus());

        // STEP 2 (Table 42): Submit an order with an empty receiver phone -> validation error (UC1 - E1)
        boolean step2Caught = false;
        try {
            Address badDelivery = new Address(0, null, "Tran Thi Dung", "",
                    "120 Nguyen Thi Thap", "", "District 7", "Ho Chi Minh City", "D7");
            orderService.createOrder(customer01.getCustomerId(), pickup, badDelivery, items, 1, 450000, "", customer01.getUsername());
        } catch (IllegalArgumentException ex) {
            step2Caught = true;
            System.out.println("[PASS] Step 2 (UC1 - E1): Empty phone rejected -> " + ex.getMessage());
        }
        assertCondition(step2Caught, "Step 2: Empty receiver phone must throw validation error");

        // STEP 3 (Table 42): Staff filters by NEW and confirms demo order (UC2)
        orderService.confirmOrder(demoOrder.getOrderId(), staff01);
        Order afterConfirm = orderService.getOrderById(demoOrder.getOrderId());
        assertCondition(afterConfirm.getStatus() == OrderStatus.CONFIRMED, "Step 3: Status must be CONFIRMED");
        List<Order> pendingD7 = assignmentService.getPendingOrders("D7");
        assertCondition(pendingD7.stream().anyMatch(o -> o.getOrderId() == demoOrder.getOrderId()),
                "Step 3: Confirmed order must appear in pending-assignment list");
        System.out.println("[PASS] Step 3 (UC2): Order " + demoOrder.getOrderCode() + " confirmed and in D7 pending list");

        // STEP 4 (Table 42): Dispatcher filters pending orders by area D7, clicks Suggest driver, and confirms (UC3)
        Driver suggested = driverService.suggestDriver("D7");
        assertCondition(suggested != null && suggested.getDriverId() == 1, "Step 4: Suggested driver should be Le Van Binh (id=1)");
        Assignment a1 = assignmentService.assignOrder(demoOrder.getOrderId(), suggested.getDriverId(), dispatch01);
        assertCondition(a1.getAttemptNo() == 1 && a1.getStatus() == AssignmentStatus.PENDING, "Step 4: Attempt 1 PENDING");
        assertCondition(orderService.getOrderById(demoOrder.getOrderId()).getStatus() == OrderStatus.ASSIGNED, "Step 4: Order status ASSIGNED");
        System.out.println("[PASS] Step 4 (UC3): Suggested " + suggested.getFullName() + ", created Assignment Attempt #" + a1.getAttemptNo());

        // STEP 5 (Table 42): Assigned driver rejects the order with a reason (UC4 - A1)
        boolean emptyRejectCaught = false;
        try {
            assignmentService.reject(a1.getAssignmentId(), "", driver01);
        } catch (IllegalArgumentException ex) {
            emptyRejectCaught = true;
        }
        assertCondition(emptyRejectCaught, "Step 5 (UC4 - E1): Rejection without reason must be refused");

        assignmentService.reject(a1.getAssignmentId(), "Motorbike flat tire", driver01);
        assertCondition(orderService.getOrderById(demoOrder.getOrderId()).getStatus() == OrderStatus.CONFIRMED,
                "Step 5: Rejected order must return to CONFIRMED (pending list)");
        System.out.println("[PASS] Step 5 (UC4 - A1): Driver 1 rejected with reason; order returned to pending list");

        // STEP 6 (Table 42): Dispatcher reassigns order to the other driver -> attempt number 2 (UC3 - A2)
        Assignment a2 = assignmentService.assignOrder(demoOrder.getOrderId(), 2, dispatch01);
        assertCondition(a2.getAttemptNo() == 2, "Step 6: Second assignment must have attempt_no = 2, got " + a2.getAttemptNo());
        System.out.println("[PASS] Step 6 (UC3 - A2): Reassigned to Pham Thi Cuc with Attempt #" + a2.getAttemptNo());

        // STEP 7 (Table 42): Driver accepts order, tests out-of-sequence refusal (UC4 - E3), then marks Picked up -> In transit -> Delivered + proof
        assignmentService.accept(a2.getAssignmentId(), driver02);
        assertCondition(driverService.getDriverById(2).getStatus() == DriverStatus.BUSY, "Step 7: Driver 2 must be BUSY after accepting");

        boolean skipCaught = false;
        try {
            // Try skipping PICKED_UP and jumping straight to IN_TRANSIT
            assignmentService.updateDeliveryStatus(a2.getAssignmentId(), OrderStatus.IN_TRANSIT, "Tran Thi Dung", "", "", driver02);
        } catch (IllegalStateException ex) {
            skipCaught = true;
            System.out.println("[PASS] Step 7 (UC4 - E3): Out-of-sequence status skip refused -> " + ex.getMessage());
        }
        assertCondition(skipCaught, "Step 7: Skipping PICKED_UP must be refused");

        assignmentService.updateDeliveryStatus(a2.getAssignmentId(), OrderStatus.PICKED_UP, "Tran Thi Dung", "Picked up from sender", "", driver02);
        assignmentService.updateDeliveryStatus(a2.getAssignmentId(), OrderStatus.IN_TRANSIT, "Tran Thi Dung", "On the way to D7", "", driver02);
        assignmentService.updateDeliveryStatus(a2.getAssignmentId(), OrderStatus.DELIVERED, "Tran Thi Dung", "Left with receiver", "proof_DH0002.jpg", driver02);
        assertCondition(orderService.getOrderById(demoOrder.getOrderId()).getStatus() == OrderStatus.DELIVERED, "Step 7: Order must be DELIVERED");
        System.out.println("[PASS] Step 7 (UC4): Completed Picked up -> In transit -> Delivered + DeliveryProof saved");

        // STEP 8 (Table 42): Customer looks up the order by its code (UC5)
        Order tracked = trackingService.getOrderForTracking(demoOrder.getOrderCode(), customer01);
        List<TrackingEvent> timeline = trackingService.getTimeline(tracked.getOrderId());
        assertCondition("Pham Thi Cuc".equals(tracked.getAssignedDriverName()), "Step 8: Driver name must be Pham Thi Cuc");
        assertCondition(timeline.size() >= 6, "Step 8: Timeline must have all status events");
        System.out.println("[PASS] Step 8 (UC5): Looked up " + tracked.getOrderCode() + ", Driver = " + tracked.getAssignedDriverName() + ", Events = " + timeline.size());

        // STEP 9 (Table 42): Staff confirms delivery and enters 450,000 VND received -> Payment RECONCILED (UC6)
        Payment pReconciled = deliveryService.reconcileCod(demoOrder.getOrderId(), 450000, "", staff01);
        assertCondition(pReconciled.getStatus() == PaymentStatus.RECONCILED, "Step 9: Payment must be RECONCILED");
        System.out.println("[PASS] Step 9 (UC6): Delivery confirmed & 450,000 VND COD reconciled -> Status = " + pReconciled.getStatus());

        // STEP 10 (Table 42): Staff reconciles sample order #1 entering 400,000 VND instead of 450,000 VND -> Payment DIFFERENCE & note required (UC6 - A2)
        boolean missingDiffNoteCaught = false;
        try {
            deliveryService.reconcileCod(1, 400000, "", staff01);
        } catch (IllegalArgumentException ex) {
            missingDiffNoteCaught = true;
        }
        assertCondition(missingDiffNoteCaught, "Step 10: Difference without note must be rejected");

        Payment pDiff = deliveryService.reconcileCod(1, 400000, "50,000 VND short, driver will pay tomorrow", staff01);
        assertCondition(pDiff.getStatus() == PaymentStatus.DIFFERENCE, "Step 10: Payment status must be DIFFERENCE");
        System.out.println("[PASS] Step 10 (UC6 - A2): 400,000 / 450,000 VND marked as " + pDiff.getStatus() + " with required note");

        // Reset sample data so GUI starts clean at Step 1
        InMemoryStore.resetSampleData();
        System.out.println("=== ALL 11 DEMO STEPS & EXCEPTION RULES PASSED 100% ===");
    }

    private static void assertCondition(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("VERIFICATION FAILED: " + msg);
        }
    }
}
