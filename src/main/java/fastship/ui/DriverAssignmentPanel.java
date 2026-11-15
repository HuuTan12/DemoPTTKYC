package fastship.ui;

import fastship.model.*;
import fastship.service.AssignmentService;
import fastship.service.DriverService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Screen 6.2.3: Driver App – Accept Order (Figure 23 & Table 38 - FR-10, FR-13, UC4).
 * Components match Table 38:
 * - Driver status label
 * - List of assigned orders with assignment status (columns: ORDER, RECEIVER, ADDRESS, COD (VND), ASSIGNMENT)
 * - Order details (pickup, receiver, delivery address, COD)
 * - Accept order button, Reject button with reason box
 * - Delivery progress buttons (Picked up, In transit, Delivered, Delivery failed)
 * - Delivery proof form (receiver name, delivery time, note, optional photo) with Submit proof button
 */
public class DriverAssignmentPanel extends JPanel {

    private final UserAccount currentUser;
    private final AssignmentService assignmentService = new AssignmentService();
    private final DriverService driverService = new DriverService();

    private JLabel driverStatusLabel;
    private DefaultTableModel assignmentTableModel;
    private JTable assignmentTable;
    private List<Assignment> assignments;
    private Assignment selectedAssignment;

    private JPanel orderDetailsCard;
    private JLabel pickupLabel;
    private JLabel receiverLabel;
    private JLabel deliveryLabel;
    private JLabel codLabel;
    private JTextField rejectReasonField;
    private JButton acceptBtn;
    private JButton rejectBtn;

    private JButton pickedUpBtn;
    private JButton inTransitBtn;
    private JButton deliveredBtn;
    private JButton failedBtn;

    private JTextField proofReceiverField;
    private JTextField proofTimeField;
    private JTextField proofNoteField;
    private JTextField proofPhotoField;

    public DriverAssignmentPanel(UserAccount currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Header + Driver status label (Figure 23)
        JPanel header = new JPanel(new BorderLayout());
        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        JLabel title = new JLabel("My assignments");
        title.setFont(UiTheme.FONT_TITLE);
        JLabel sub = new JLabel("Accept or reject each assigned order, then report pickup and delivery.");
        sub.setFont(UiTheme.FONT_SUBTITLE);
        titleBox.add(title);
        titleBox.add(sub);

        driverStatusLabel = new JLabel("Status: Available");
        driverStatusLabel.setFont(UiTheme.FONT_LABEL);

        header.add(titleBox, BorderLayout.WEST);
        header.add(driverStatusLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Main Split: Left table + Right 3 panels (Figure 23)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.55);

        assignmentTableModel = new DefaultTableModel(
                new Object[]{"ORDER", "RECEIVER", "ADDRESS", "COD (VND)", "ASSIGNMENT"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        assignmentTable = new JTable(assignmentTableModel);
        UiTheme.styleTable(assignmentTable);
        assignmentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int r = assignmentTable.getSelectedRow();
                if (r >= 0 && assignments != null && r < assignments.size()) {
                    showAssignmentDetails(assignments.get(r));
                }
            }
        });
        splitPane.setLeftComponent(new JScrollPane(assignmentTable));

        // Right column: 3 panels matching Figure 23
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));

        // Panel 1: Order details + Accept / Reject + Reject reason
        orderDetailsCard = UiTheme.createCardPanel("Order details");
        orderDetailsCard.setLayout(new BorderLayout(0, 8));

        JPanel infoGrid = new JPanel(new GridLayout(4, 2, 6, 4));
        pickupLabel = new JLabel("—");
        receiverLabel = new JLabel("—");
        deliveryLabel = new JLabel("—");
        codLabel = new JLabel("—");
        codLabel.setFont(UiTheme.FONT_LABEL);

        infoGrid.add(UiTheme.createFieldLabel("Pickup"));
        infoGrid.add(pickupLabel);
        infoGrid.add(UiTheme.createFieldLabel("Receiver"));
        infoGrid.add(receiverLabel);
        infoGrid.add(UiTheme.createFieldLabel("Delivery"));
        infoGrid.add(deliveryLabel);
        infoGrid.add(UiTheme.createFieldLabel("COD"));
        infoGrid.add(codLabel);
        orderDetailsCard.add(infoGrid, BorderLayout.CENTER);

        JPanel acceptRejectBox = new JPanel(new BorderLayout(0, 6));
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        acceptBtn = UiTheme.createPrimaryButton("Accept order");
        acceptBtn.addActionListener(e -> handleAccept());
        rejectBtn = UiTheme.createDangerButton("Reject");
        rejectBtn.addActionListener(e -> handleReject());
        btnRow.add(acceptBtn);
        btnRow.add(rejectBtn);

        JPanel reasonPanel = new JPanel(new BorderLayout(0, 2));
        reasonPanel.add(UiTheme.createFieldLabel("Reject reason (required when rejecting)"), BorderLayout.NORTH);
        rejectReasonField = UiTheme.createTextField("");
        reasonPanel.add(rejectReasonField, BorderLayout.CENTER);

        acceptRejectBox.add(btnRow, BorderLayout.NORTH);
        acceptRejectBox.add(reasonPanel, BorderLayout.SOUTH);
        orderDetailsCard.add(acceptRejectBox, BorderLayout.SOUTH);

        // Panel 2: Delivery progress (after accepting)
        JPanel progressCard = UiTheme.createCardPanel("Delivery progress (after accepting)");
        progressCard.setLayout(new GridLayout(1, 4, 6, 0));
        pickedUpBtn = UiTheme.createOutlineButton("Picked up");
        pickedUpBtn.addActionListener(e -> handleProgressUpdate(OrderStatus.PICKED_UP));
        inTransitBtn = UiTheme.createOutlineButton("In transit");
        inTransitBtn.addActionListener(e -> handleProgressUpdate(OrderStatus.IN_TRANSIT));
        deliveredBtn = UiTheme.createOutlineButton("Delivered");
        deliveredBtn.addActionListener(e -> handleProgressUpdate(OrderStatus.DELIVERED));
        failedBtn = UiTheme.createDangerButton("Delivery failed");
        failedBtn.addActionListener(e -> handleProgressUpdate(OrderStatus.FAILED));

        progressCard.add(pickedUpBtn);
        progressCard.add(inTransitBtn);
        progressCard.add(deliveredBtn);
        progressCard.add(failedBtn);

        // Panel 3: Delivery proof (after delivering)
        JPanel proofCard = UiTheme.createCardPanel("Delivery proof (after delivering)");
        proofCard.setLayout(new BorderLayout(0, 8));
        JPanel proofForm = new JPanel(new GridLayout(2, 2, 8, 6));
        proofReceiverField = UiTheme.createTextField("");
        proofTimeField = UiTheme.createTextField(UiTheme.formatDateTime(LocalDateTime.now()));
        proofNoteField = UiTheme.createTextField("Left with receiver at door");
        proofPhotoField = UiTheme.createTextField("proof_photo.jpg");

        JPanel p1 = new JPanel(new BorderLayout(0, 2));
        p1.add(UiTheme.createFieldLabel("Receiver name"), BorderLayout.NORTH);
        p1.add(proofReceiverField, BorderLayout.CENTER);

        JPanel p2 = new JPanel(new BorderLayout(0, 2));
        p2.add(UiTheme.createFieldLabel("Delivered at"), BorderLayout.NORTH);
        p2.add(proofTimeField, BorderLayout.CENTER);

        JPanel p3 = new JPanel(new BorderLayout(0, 2));
        p3.add(UiTheme.createFieldLabel("Note"), BorderLayout.NORTH);
        p3.add(proofNoteField, BorderLayout.CENTER);

        JPanel p4 = new JPanel(new BorderLayout(0, 2));
        p4.add(UiTheme.createFieldLabel("Photo (optional)"), BorderLayout.NORTH);
        p4.add(proofPhotoField, BorderLayout.CENTER);

        proofForm.add(p1);
        proofForm.add(p2);
        proofForm.add(p3);
        proofForm.add(p4);
        proofCard.add(proofForm, BorderLayout.CENTER);

        JButton submitProofBtn = UiTheme.createPrimaryButton("Submit proof");
        submitProofBtn.addActionListener(e -> handleProgressUpdate(OrderStatus.DELIVERED));
        proofCard.add(submitProofBtn, BorderLayout.SOUTH);

        rightCol.add(orderDetailsCard);
        rightCol.add(Box.createVerticalStrut(8));
        rightCol.add(progressCard);
        rightCol.add(Box.createVerticalStrut(8));
        rightCol.add(proofCard);

        splitPane.setRightComponent(rightCol);
        add(splitPane, BorderLayout.CENTER);
    }

    public void refreshData() {
        int driverId = currentUser.getDriverId() != null ? currentUser.getDriverId() : 1;
        Driver d = driverService.getDriverById(driverId);
        if (d != null) {
            driverStatusLabel.setText("Status: " + d.getStatus().name());
        }
        assignments = assignmentService.getAssignmentsForDriver(driverId);
        assignmentTableModel.setRowCount(0);
        for (Assignment a : assignments) {
            Order o = a.getOrder();
            String code = o != null ? o.getOrderCode() : ("#" + a.getOrderId());
            String recv = (o != null && o.getDeliveryAddress() != null) ? o.getDeliveryAddress().getContactName() : "—";
            String addr = (o != null && o.getDeliveryAddress() != null)
                    ? o.getDeliveryAddress().getStreet() + ", " + o.getDeliveryAddress().getArea() : "—";
            long cod = o != null ? o.getCodAmount() : 0;
            assignmentTableModel.addRow(new Object[]{
                    code, recv, addr, UiTheme.formatNumber(cod), a.getStatus().name()
            });
        }
        if (!assignments.isEmpty()) {
            assignmentTable.setRowSelectionInterval(0, 0);
            showAssignmentDetails(assignments.get(0));
        } else {
            selectedAssignment = null;
        }
    }

    private void showAssignmentDetails(Assignment a) {
        this.selectedAssignment = a;
        Order o = a.getOrder();
        if (o == null) return;
        orderDetailsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Order " + o.getOrderCode() + " (Order status: " + o.getStatus() + ")"),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        Address p = o.getPickupAddress();
        Address d = o.getDeliveryAddress();
        pickupLabel.setText(p != null ? p.fullAddress() : "—");
        receiverLabel.setText(d != null ? d.getContactName() + " · " + d.getPhone() : "—");
        deliveryLabel.setText(d != null ? d.fullAddress() : "—");
        codLabel.setText(UiTheme.formatVnd(o.getCodAmount()));

        if (d != null) {
            proofReceiverField.setText(d.getContactName());
        }
        proofTimeField.setText(UiTheme.formatDateTime(LocalDateTime.now()));
        proofPhotoField.setText("proof_" + o.getOrderCode() + ".jpg");

        boolean isPending = a.getStatus() == AssignmentStatus.PENDING;
        acceptBtn.setEnabled(isPending);
        rejectBtn.setEnabled(isPending);
    }

    private void handleAccept() {
        if (selectedAssignment == null) return;
        try {
            assignmentService.accept(selectedAssignment.getAssignmentId(), currentUser);
            JOptionPane.showMessageDialog(this,
                    "Assignment accepted (Status: ACCEPTED, Driver: BUSY).",
                    "UC4 - Accept Order", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleReject() {
        if (selectedAssignment == null) return;
        try {
            assignmentService.reject(selectedAssignment.getAssignmentId(), rejectReasonField.getText(), currentUser);
            rejectReasonField.setText("");
            JOptionPane.showMessageDialog(this,
                    "Order rejected. Order has returned to Dispatcher's pending list (UC4 - A1).",
                    "UC4 - Reject Order", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "UC4 - E1", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleProgressUpdate(OrderStatus targetStatus) {
        if (selectedAssignment == null) return;
        try {
            assignmentService.updateDeliveryStatus(
                    selectedAssignment.getAssignmentId(),
                    targetStatus,
                    proofReceiverField.getText(),
                    proofNoteField.getText(),
                    proofPhotoField.getText(),
                    currentUser
            );
            JOptionPane.showMessageDialog(this,
                    "Order status updated to " + targetStatus.name() + ".",
                    "UC4 - Delivery Progress", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "UC4 - Exception", JOptionPane.ERROR_MESSAGE);
        }
    }
}
