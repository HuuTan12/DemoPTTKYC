package fastship.ui;

import fastship.model.*;
import fastship.service.OrderService;
import fastship.service.PricingService;
import fastship.service.TrackingService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Screens 6.1.2 (Order List - Figure 19) and 6.1.3 (Order Details - Figure 20).
 * Navigation matches Section 6.1.4:
 * Order List -> View -> Order Details -> Edit / Confirm / Cancel -> back to the list.
 */
public class OrderListAndDetailPanel extends JPanel {

    private final UserAccount currentUser;
    private final OrderService orderService = new OrderService();
    private final TrackingService trackingService = new TrackingService();
    private final PricingService pricingService = new PricingService();

    private final CardLayout cardLayout = new CardLayout();

    // Screen 6.1.2: Order List components (Figure 19 & Table 37)
    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;
    private JComboBox<String> areaFilterCombo;
    private DefaultTableModel orderTableModel;
    private JTable orderTable;
    private JLabel listCountFooterLabel;
    private List<Order> currentOrders;

    // Screen 6.1.3: Order Details components (Figure 20 & Table 37)
    private Order selectedOrder;
    private JLabel detailOrderCodeLabel;
    private JLabel detailStatusLabel;
    private JLabel senderVal;
    private JLabel pickupVal;
    private JLabel receiverVal;
    private JLabel deliveryVal;
    private JLabel serviceVal;
    private JLabel shippingFeeVal;
    private JLabel codAmountVal;
    private JLabel createdVal;
    private DefaultTableModel detailItemsModel;
    private DefaultListModel<String> statusHistoryModel;
    private JTextArea cancelReasonArea;
    private JButton detailEditBtn;
    private JButton detailConfirmBtn;
    private JButton detailCancelBtn;

    public OrderListAndDetailPanel(UserAccount currentUser) {
        this.currentUser = currentUser;
        setLayout(cardLayout);
        add(buildOrderListScreen(), "LIST");
        add(buildOrderDetailsScreen(), "DETAILS");
        refreshData();
        cardLayout.show(this, "LIST");
    }

    /**
     * Figure 19: Order List screen
     */
    private JPanel buildOrderListScreen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        boolean isCustomer = currentUser.getRole() == Role.CUSTOMER;

        // Header + Search & Filters (Figure 19)
        JPanel topBox = new JPanel(new BorderLayout(0, 8));
        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        JLabel title = new JLabel(isCustomer ? "My orders" : "Orders");
        title.setFont(UiTheme.FONT_TITLE);
        JLabel sub = new JLabel(isCustomer
                ? "Your orders. Select an order to view details or cancel before pickup."
                : "All orders. Filter by status to review new orders.");
        sub.setFont(UiTheme.FONT_SUBTITLE);
        titleBox.add(title);
        titleBox.add(sub);
        topBox.add(titleBox, BorderLayout.NORTH);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField = UiTheme.createTextField("");
        searchField.setPreferredSize(new Dimension(260, 28));
        searchField.setToolTipText("Search by order code or receiver phone");

        statusFilterCombo = new JComboBox<>(new String[]{
                "Status: All", "NEW", "CONFIRMED", "ASSIGNED", "PICKED_UP", "IN_TRANSIT", "DELIVERED", "FAILED", "CANCELLED"
        });
        statusFilterCombo.addActionListener(e -> refreshData());

        areaFilterCombo = new JComboBox<>(new String[]{"Area: All", "D7", "D1", "D3", "D5"});
        areaFilterCombo.addActionListener(e -> refreshData());

        JButton searchBtn = UiTheme.createOutlineButton("Search");
        searchBtn.addActionListener(e -> refreshData());

        filterBar.add(new JLabel("Search (code/phone):"));
        filterBar.add(searchField);
        filterBar.add(statusFilterCombo);
        filterBar.add(areaFilterCombo);
        filterBar.add(searchBtn);
        topBox.add(filterBar, BorderLayout.CENTER);

        panel.add(topBox, BorderLayout.NORTH);

        // Table matching Figure 19 columns: ORDER CODE | RECEIVER | AREA | SERVICE | STATUS
        orderTableModel = new DefaultTableModel(
                new Object[]{"ORDER CODE", "RECEIVER", "AREA", "SERVICE", "STATUS"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        orderTable = new JTable(orderTableModel);
        UiTheme.styleTable(orderTable);
        panel.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        // Bottom actions matching Figure 19 & Table 37: View button; Confirm and Cancel buttons for staff only
        JPanel bottomBar = new JPanel(new BorderLayout());
        listCountFooterLabel = new JLabel("Showing 0 orders");
        listCountFooterLabel.setFont(UiTheme.FONT_SUBTITLE);
        bottomBar.add(listCountFooterLabel, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton viewBtn = UiTheme.createOutlineButton("View");
        viewBtn.addActionListener(e -> openSelectedOrderDetails());
        actionBtns.add(viewBtn);

        if (!isCustomer) {
            JButton confirmBtn = UiTheme.createPrimaryButton("Confirm");
            confirmBtn.addActionListener(e -> confirmSelectedOrderFromList());
            JButton cancelBtn = UiTheme.createDangerButton("Cancel");
            cancelBtn.addActionListener(e -> openSelectedOrderDetails());
            actionBtns.add(confirmBtn);
            actionBtns.add(cancelBtn);
        }
        bottomBar.add(actionBtns, BorderLayout.EAST);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Figure 20: Order Details screen
     */
    private JPanel buildOrderDetailsScreen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        boolean isCustomer = currentUser.getRole() == Role.CUSTOMER;

        // Top bar: Order code + status on left; Back, Edit, Cancel order, Confirm order on right (Figure 20)
        JPanel topBar = new JPanel(new BorderLayout());
        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton backBtn = UiTheme.createOutlineButton("<- Back to Orders");
        backBtn.addActionListener(e -> {
            refreshData();
            cardLayout.show(this, "LIST");
        });
        detailOrderCodeLabel = new JLabel("Order DH0001");
        detailOrderCodeLabel.setFont(UiTheme.FONT_TITLE);
        detailStatusLabel = new JLabel("[NEW]");
        detailStatusLabel.setFont(UiTheme.FONT_CARD_TITLE);
        leftHeader.add(backBtn);
        leftHeader.add(detailOrderCodeLabel);
        leftHeader.add(detailStatusLabel);
        topBar.add(leftHeader, BorderLayout.WEST);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        detailEditBtn = UiTheme.createOutlineButton("Edit");
        detailEditBtn.addActionListener(e -> handleEditOrder());
        detailCancelBtn = UiTheme.createDangerButton("Cancel order");
        detailCancelBtn.addActionListener(e -> handleCancelOrder());
        detailConfirmBtn = UiTheme.createPrimaryButton("Confirm order");
        detailConfirmBtn.addActionListener(e -> handleConfirmOrder());

        if (isCustomer) {
            detailEditBtn.setVisible(false);
            detailConfirmBtn.setVisible(false);
        }
        rightActions.add(detailEditBtn);
        rightActions.add(detailCancelBtn);
        rightActions.add(detailConfirmBtn);
        topBar.add(rightActions, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        // Main 2-column layout matching Figure 20
        JPanel mainGrid = new JPanel(new GridLayout(1, 2, 14, 0));

        // Left column: Order information + Items table
        JPanel leftCol = new JPanel(new BorderLayout(0, 10));

        JPanel infoPanel = UiTheme.createCardPanel("Order information");
        infoPanel.setLayout(new GridLayout(8, 2, 8, 6));
        senderVal = new JLabel("—");
        pickupVal = new JLabel("—");
        receiverVal = new JLabel("—");
        deliveryVal = new JLabel("—");
        serviceVal = new JLabel("—");
        shippingFeeVal = new JLabel("—");
        shippingFeeVal.setFont(UiTheme.FONT_LABEL);
        codAmountVal = new JLabel("—");
        codAmountVal.setFont(UiTheme.FONT_LABEL);
        createdVal = new JLabel("—");

        infoPanel.add(UiTheme.createFieldLabel("Sender"));
        infoPanel.add(senderVal);
        infoPanel.add(UiTheme.createFieldLabel("Pickup"));
        infoPanel.add(pickupVal);
        infoPanel.add(UiTheme.createFieldLabel("Receiver"));
        infoPanel.add(receiverVal);
        infoPanel.add(UiTheme.createFieldLabel("Delivery"));
        infoPanel.add(deliveryVal);
        infoPanel.add(UiTheme.createFieldLabel("Service"));
        infoPanel.add(serviceVal);
        infoPanel.add(UiTheme.createFieldLabel("Shipping fee"));
        infoPanel.add(shippingFeeVal);
        infoPanel.add(UiTheme.createFieldLabel("COD amount"));
        infoPanel.add(codAmountVal);
        infoPanel.add(UiTheme.createFieldLabel("Created"));
        infoPanel.add(createdVal);

        JPanel itemsPanel = UiTheme.createCardPanel("Items");
        itemsPanel.setLayout(new BorderLayout());
        detailItemsModel = new DefaultTableModel(new Object[]{"DESCRIPTION", "QUANTITY", "WEIGHT (KG)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable itemsTable = new JTable(detailItemsModel);
        UiTheme.styleTable(itemsTable);
        itemsPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        leftCol.add(infoPanel, BorderLayout.NORTH);
        leftCol.add(itemsPanel, BorderLayout.CENTER);

        // Right column: Status history + Cancel order box (Figure 20)
        JPanel rightCol = new JPanel(new GridLayout(2, 1, 0, 10));

        JPanel historyPanel = UiTheme.createCardPanel("Status history");
        historyPanel.setLayout(new BorderLayout());
        statusHistoryModel = new DefaultListModel<>();
        JList<String> historyList = new JList<>(statusHistoryModel);
        historyList.setFont(UiTheme.FONT_BODY);
        historyPanel.add(new JScrollPane(historyList), BorderLayout.CENTER);

        JPanel cancelPanel = UiTheme.createCardPanel("Cancel order");
        cancelPanel.setLayout(new BorderLayout(0, 6));
        cancelPanel.add(UiTheme.createFieldLabel("Reason (required)"), BorderLayout.NORTH);
        cancelReasonArea = new JTextArea(4, 20);
        cancelReasonArea.setLineWrap(true);
        cancelReasonArea.setWrapStyleWord(true);
        cancelPanel.add(new JScrollPane(cancelReasonArea), BorderLayout.CENTER);
        JLabel cancelWarning = new JLabel("If the order is already assigned, the assignment is cancelled and the driver is informed.");
        cancelWarning.setFont(UiTheme.FONT_SUBTITLE);
        cancelPanel.add(cancelWarning, BorderLayout.SOUTH);

        rightCol.add(historyPanel);
        rightCol.add(cancelPanel);

        mainGrid.add(leftCol);
        mainGrid.add(rightCol);
        panel.add(mainGrid, BorderLayout.CENTER);

        return panel;
    }

    public void refreshData() {
        OrderStatus statusFilter = null;
        String selStatus = (String) statusFilterCombo.getSelectedItem();
        if (selStatus != null && !selStatus.startsWith("Status:")) {
            statusFilter = OrderStatus.valueOf(selStatus);
        }
        String selArea = (String) areaFilterCombo.getSelectedItem();
        String areaFilter = (selArea != null && !selArea.startsWith("Area:")) ? selArea : null;
        Integer customerFilter = currentUser.getRole() == Role.CUSTOMER ? currentUser.getCustomerId() : null;

        currentOrders = orderService.getOrders(statusFilter, areaFilter, customerFilter, searchField.getText());
        orderTableModel.setRowCount(0);
        for (Order o : currentOrders) {
            String recv = o.getDeliveryAddress() != null ? o.getDeliveryAddress().getContactName() : "—";
            String area = o.getDeliveryAddress() != null ? o.getDeliveryAddress().getArea() : "—";
            String srv = o.getServiceType() != null ? o.getServiceType().getName() : "—";
            orderTableModel.addRow(new Object[]{
                    o.getOrderCode(),
                    recv,
                    area,
                    srv,
                    o.getStatus().name()
            });
        }
        listCountFooterLabel.setText("Showing " + currentOrders.size() + " orders · Customers see only their own orders and no Confirm button.");
        if (!currentOrders.isEmpty()) {
            orderTable.setRowSelectionInterval(0, 0);
        }
    }

    private void openSelectedOrderDetails() {
        int row = orderTable.getSelectedRow();
        if (row < 0 || currentOrders == null || row >= currentOrders.size()) {
            JOptionPane.showMessageDialog(this, "Please select an order row first.");
            return;
        }
        populateOrderDetails(currentOrders.get(row));
        cardLayout.show(this, "DETAILS");
    }

    private void confirmSelectedOrderFromList() {
        int row = orderTable.getSelectedRow();
        if (row < 0 || currentOrders == null || row >= currentOrders.size()) {
            JOptionPane.showMessageDialog(this, "Please select an order row first.");
            return;
        }
        this.selectedOrder = currentOrders.get(row);
        handleConfirmOrder();
    }

    private void populateOrderDetails(Order o) {
        this.selectedOrder = orderService.getOrderById(o.getOrderId());
        if (this.selectedOrder == null) this.selectedOrder = o;

        detailOrderCodeLabel.setText("Order " + selectedOrder.getOrderCode());
        detailStatusLabel.setText("[" + selectedOrder.getStatus().name() + "]");

        Address p = selectedOrder.getPickupAddress();
        Address d = selectedOrder.getDeliveryAddress();
        senderVal.setText(p != null ? p.getContactName() + " · " + p.getPhone() : "—");
        pickupVal.setText(p != null ? p.fullAddress() : "—");
        receiverVal.setText(d != null ? d.getContactName() + " · " + d.getPhone() : "—");
        deliveryVal.setText(d != null ? d.fullAddress() + " (" + d.getArea() + ")" : "—");

        String srvName = selectedOrder.getServiceType() != null ? selectedOrder.getServiceType().getName() : "Standard";
        serviceVal.setText(String.format("%s · %.1f kg", srvName, selectedOrder.getTotalWeightKg()));
        shippingFeeVal.setText(UiTheme.formatVnd(selectedOrder.getShippingFee()));
        codAmountVal.setText(UiTheme.formatVnd(selectedOrder.getCodAmount()));
        createdVal.setText(UiTheme.formatDateTime(selectedOrder.getCreatedAt()));

        detailItemsModel.setRowCount(0);
        for (OrderItem item : selectedOrder.getItems()) {
            detailItemsModel.addRow(new Object[]{item.getDescription(), item.getQuantity(), item.getWeightKg()});
        }

        statusHistoryModel.clear();
        for (TrackingEvent ev : trackingService.getTimeline(selectedOrder.getOrderId())) {
            statusHistoryModel.addElement(String.format("%s - %s · %s (%s)",
                    ev.getStatus().name(),
                    UiTheme.formatDateTime(ev.getEventTime()),
                    ev.getRecordedBy(),
                    ev.getNote() != null ? ev.getNote() : ""));
        }

        boolean isStaff = currentUser.getRole() != Role.CUSTOMER;
        detailConfirmBtn.setEnabled(isStaff && selectedOrder.getStatus() == OrderStatus.NEW);
        detailEditBtn.setEnabled(isStaff && (selectedOrder.getStatus() == OrderStatus.NEW || selectedOrder.getStatus() == OrderStatus.CONFIRMED));
        detailCancelBtn.setEnabled(selectedOrder.canCancel());
    }

    private void handleConfirmOrder() {
        if (selectedOrder == null) return;
        try {
            orderService.confirmOrder(selectedOrder.getOrderId(), currentUser);
            JOptionPane.showMessageDialog(this,
                    "Order " + selectedOrder.getOrderCode() + " confirmed (Status: CONFIRMED).",
                    "UC2 - Confirm Order", JOptionPane.INFORMATION_MESSAGE);
            populateOrderDetails(selectedOrder);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "UC2 - Exception", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEditOrder() {
        if (selectedOrder == null) return;
        Address d = selectedOrder.getDeliveryAddress();
        JTextField recvNameField = new JTextField(d != null ? d.getContactName() : "");
        JTextField recvPhoneField = new JTextField(d != null ? d.getPhone() : "");
        JTextField streetField = new JTextField(d != null ? d.getStreet() : "");
        JComboBox<String> areaCombo = new JComboBox<>(new String[]{"D7", "D1", "D3", "D5"});
        if (d != null) areaCombo.setSelectedItem(d.getArea());

        JComboBox<ServiceType> srvCombo = new JComboBox<>();
        for (ServiceType st : pricingService.getActiveServiceTypes()) {
            srvCombo.addItem(st);
            if (st.getServiceTypeId() == selectedOrder.getServiceTypeId()) {
                srvCombo.setSelectedItem(st);
            }
        }
        JTextField codField = new JTextField(String.valueOf(selectedOrder.getCodAmount()));
        JTextField noteField = new JTextField(selectedOrder.getNote() != null ? selectedOrder.getNote() : "");

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Receiver name:"));
        form.add(recvNameField);
        form.add(new JLabel("Receiver phone:"));
        form.add(recvPhoneField);
        form.add(new JLabel("Delivery address:"));
        form.add(streetField);
        form.add(new JLabel("Area:"));
        form.add(areaCombo);
        form.add(new JLabel("Service type:"));
        form.add(srvCombo);
        form.add(new JLabel("COD amount (VND):"));
        form.add(codField);
        form.add(new JLabel("Note:"));
        form.add(noteField);

        int res = JOptionPane.showConfirmDialog(this, form,
                "Edit Order " + selectedOrder.getOrderCode() + " (UC2 - A1)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                ServiceType selSrv = (ServiceType) srvCombo.getSelectedItem();
                orderService.editOrderBeforeAssignment(
                        selectedOrder.getOrderId(),
                        recvNameField.getText(),
                        recvPhoneField.getText(),
                        streetField.getText(),
                        (String) areaCombo.getSelectedItem(),
                        selSrv != null ? selSrv.getServiceTypeId() : selectedOrder.getServiceTypeId(),
                        Long.parseLong(codField.getText().trim()),
                        noteField.getText()
                );
                populateOrderDetails(selectedOrder);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Edit Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleCancelOrder() {
        if (selectedOrder == null) return;
        try {
            orderService.cancelOrder(selectedOrder.getOrderId(), cancelReasonArea.getText(), currentUser);
            cancelReasonArea.setText("");
            JOptionPane.showMessageDialog(this,
                    "Order " + selectedOrder.getOrderCode() + " has been CANCELLED.",
                    "UC2 - Cancel Order", JOptionPane.INFORMATION_MESSAGE);
            populateOrderDetails(selectedOrder);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "UC2 - Cancel Refused", JOptionPane.ERROR_MESSAGE);
        }
    }
}
