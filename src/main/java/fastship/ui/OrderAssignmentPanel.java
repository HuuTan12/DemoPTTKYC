package fastship.ui;

import fastship.model.Driver;
import fastship.model.Order;
import fastship.model.UserAccount;
import fastship.service.AssignmentService;
import fastship.service.DriverService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Screen 6.2.2: Order Assignment (Figure 22 & Table 38 - FR-08, FR-09, UC3).
 * Components match Table 38:
 * - Pending order list with area filter (columns: ORDER, RECEIVER, DELIVERY ADDRESS, WEIGHT, COD (VND))
 * - Available-driver table with area and current order count (columns: DRIVER, AREA, CURRENT ORDERS)
 * - "Suggest driver" button with a "Suggested" label
 * - Assignment summary (order, driver, attempt number)
 * - Clear and Confirm assignment buttons
 * - Message when no driver is available
 */
public class OrderAssignmentPanel extends JPanel {

    private final UserAccount currentUser;
    private final AssignmentService assignmentService = new AssignmentService();
    private final DriverService driverService = new DriverService();

    private JComboBox<String> areaFilterCombo;
    private JPanel pendingCard;
    private DefaultTableModel pendingTableModel;
    private JTable pendingTable;
    private List<Order> pendingOrders;

    private DefaultTableModel driverTableModel;
    private JTable driverTable;
    private List<Driver> availableDrivers;
    private Integer suggestedDriverId = null;

    private JLabel summaryTitleLabel;
    private JLabel summaryAttemptLabel;

    public OrderAssignmentPanel(UserAccount currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Header + Area filter (Figure 22)
        JPanel headerPanel = new JPanel(new BorderLayout());
        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        JLabel title = new JLabel("Order assignment");
        title.setFont(UiTheme.FONT_TITLE);
        JLabel subtitle = new JLabel("Confirmed orders without an active assignment. Pick an order, then a driver.");
        subtitle.setFont(UiTheme.FONT_SUBTITLE);
        titleBox.add(title);
        titleBox.add(subtitle);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        areaFilterCombo = new JComboBox<>(new String[]{"Area: D7", "Area: All", "Area: D1", "Area: D3", "Area: D5"});
        areaFilterCombo.addActionListener(e -> refreshData());
        filterPanel.add(areaFilterCombo);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(filterPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Two tables side by side (Figure 22)
        JPanel middleGrid = new JPanel(new GridLayout(1, 2, 12, 0));

        // Left: Pending orders table
        pendingCard = UiTheme.createCardPanel("Pending orders (0)");
        pendingCard.setLayout(new BorderLayout());
        pendingTableModel = new DefaultTableModel(
                new Object[]{"ORDER", "RECEIVER", "DELIVERY ADDRESS", "WEIGHT", "COD (VND)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pendingTable = new JTable(pendingTableModel);
        UiTheme.styleTable(pendingTable);
        pendingTable.getSelectionModel().addListSelectionListener(e -> updateSelectionSummary());
        pendingCard.add(new JScrollPane(pendingTable), BorderLayout.CENTER);

        // Right: Available drivers table + Suggest driver button
        JPanel driverCard = UiTheme.createCardPanel("Available drivers");
        driverCard.setLayout(new BorderLayout(0, 6));
        JPanel suggestRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton suggestBtn = UiTheme.createOutlineButton("Suggest driver");
        suggestBtn.addActionListener(e -> handleSuggestDriver());
        suggestRow.add(suggestBtn);
        driverCard.add(suggestRow, BorderLayout.NORTH);

        driverTableModel = new DefaultTableModel(
                new Object[]{"DRIVER", "AREA", "CURRENT ORDERS"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        driverTable = new JTable(driverTableModel);
        UiTheme.styleTable(driverTable);
        driverTable.getSelectionModel().addListSelectionListener(e -> updateSelectionSummary());
        driverCard.add(new JScrollPane(driverTable), BorderLayout.CENTER);

        middleGrid.add(pendingCard);
        middleGrid.add(driverCard);
        add(middleGrid, BorderLayout.CENTER);

        // Bottom Assignment summary + Clear + Confirm assignment + info message (Figure 22)
        JPanel bottomContainer = new JPanel(new BorderLayout(0, 6));

        JPanel assignBanner = UiTheme.createCardPanel("Assignment");
        assignBanner.setLayout(new BorderLayout());

        JPanel summaryText = new JPanel(new GridLayout(2, 1, 0, 2));
        summaryTitleLabel = new JLabel("Select an order and a driver");
        summaryTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        summaryAttemptLabel = new JLabel("Attempt 1 · driver will see the order in the Driver portal");
        summaryAttemptLabel.setFont(UiTheme.FONT_SUBTITLE);
        summaryText.add(summaryTitleLabel);
        summaryText.add(summaryAttemptLabel);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        JButton clearBtn = UiTheme.createOutlineButton("Clear");
        clearBtn.addActionListener(e -> {
            pendingTable.clearSelection();
            driverTable.clearSelection();
            updateSelectionSummary();
        });
        JButton confirmAssignBtn = UiTheme.createPrimaryButton("Confirm assignment");
        confirmAssignBtn.addActionListener(e -> handleConfirmAssignment());
        btns.add(clearBtn);
        btns.add(confirmAssignBtn);

        assignBanner.add(summaryText, BorderLayout.WEST);
        assignBanner.add(btns, BorderLayout.EAST);
        bottomContainer.add(assignBanner, BorderLayout.CENTER);

        JLabel infoMsg = new JLabel("If no driver is available, the order stays in the pending list. If another dispatcher assigns it first, the list reloads.");
        infoMsg.setFont(UiTheme.FONT_SUBTITLE);
        bottomContainer.add(infoMsg, BorderLayout.SOUTH);

        add(bottomContainer, BorderLayout.SOUTH);
    }

    private String getSelectedAreaCode() {
        String raw = (String) areaFilterCombo.getSelectedItem();
        if (raw == null || raw.contains("All")) return "ALL";
        return raw.replace("Area:", "").trim();
    }

    public void refreshData() {
        String area = getSelectedAreaCode();
        pendingOrders = assignmentService.getPendingOrders(area);
        pendingCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Pending orders (" + pendingOrders.size() + ")"),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        pendingTableModel.setRowCount(0);
        for (Order o : pendingOrders) {
            String recv = o.getDeliveryAddress() != null ? o.getDeliveryAddress().getContactName() : "—";
            String addr = o.getDeliveryAddress() != null
                    ? o.getDeliveryAddress().getStreet() + ", " + o.getDeliveryAddress().getArea() : "—";
            pendingTableModel.addRow(new Object[]{
                    o.getOrderCode(),
                    recv,
                    addr,
                    String.format("%.1f kg", o.getTotalWeightKg()),
                    UiTheme.formatNumber(o.getCodAmount())
            });
        }

        availableDrivers = driverService.getAvailableDrivers(area);
        Driver suggested = driverService.suggestDriver(area);
        suggestedDriverId = suggested != null ? suggested.getDriverId() : null;

        driverTableModel.setRowCount(0);
        for (Driver d : availableDrivers) {
            String nameDisplay = d.getFullName();
            if (suggestedDriverId != null && d.getDriverId() == suggestedDriverId) {
                nameDisplay += " (Suggested)";
            }
            driverTableModel.addRow(new Object[]{
                    nameDisplay,
                    d.getServiceArea(),
                    d.getCurrentOrdersCount()
            });
        }

        if (!pendingOrders.isEmpty()) {
            pendingTable.setRowSelectionInterval(0, 0);
        }
        if (!availableDrivers.isEmpty()) {
            driverTable.setRowSelectionInterval(0, 0);
        }
        updateSelectionSummary();
    }

    private void handleSuggestDriver() {
        String area = getSelectedAreaCode();
        int orderRow = pendingTable.getSelectedRow();
        if (orderRow >= 0 && orderRow < pendingOrders.size()) {
            Order selOrder = pendingOrders.get(orderRow);
            if (selOrder.getDeliveryAddress() != null) {
                area = selOrder.getDeliveryAddress().getArea();
            }
        }
        Driver suggested = driverService.suggestDriver(area);
        if (suggested == null) {
            JOptionPane.showMessageDialog(this, "No driver is available in area " + area + " (UC3 - E1).",
                    "No Driver Available", JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (int i = 0; i < availableDrivers.size(); i++) {
            if (availableDrivers.get(i).getDriverId() == suggested.getDriverId()) {
                driverTable.setRowSelectionInterval(i, i);
                break;
            }
        }
        updateSelectionSummary();
    }

    private void updateSelectionSummary() {
        int oRow = pendingTable.getSelectedRow();
        int dRow = driverTable.getSelectedRow();
        if (oRow >= 0 && oRow < pendingOrders.size() && dRow >= 0 && dRow < availableDrivers.size()) {
            Order o = pendingOrders.get(oRow);
            Driver d = availableDrivers.get(dRow);
            int attemptNo = assignmentService.getNextAttemptNumber(o.getOrderId());
            summaryTitleLabel.setText(o.getOrderCode() + " -> " + d.getFullName());
            summaryAttemptLabel.setText("Attempt " + attemptNo + " · driver will see the order in the Driver portal");
        } else {
            summaryTitleLabel.setText("Select an order and a driver");
            summaryAttemptLabel.setText("Attempt —");
        }
    }

    private void handleConfirmAssignment() {
        int oRow = pendingTable.getSelectedRow();
        int dRow = driverTable.getSelectedRow();
        if (oRow < 0 || oRow >= pendingOrders.size()) {
            JOptionPane.showMessageDialog(this, "Please select a pending order.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (dRow < 0 || dRow >= availableDrivers.size()) {
            JOptionPane.showMessageDialog(this, "No driver is available (UC3 - E1).", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Order o = pendingOrders.get(oRow);
        Driver d = availableDrivers.get(dRow);
        try {
            var assignment = assignmentService.assignOrder(o.getOrderId(), d.getDriverId(), currentUser);
            JOptionPane.showMessageDialog(this,
                    String.format("Order %s assigned to %s (Attempt %d).",
                            o.getOrderCode(), d.getFullName(), assignment.getAttemptNo()),
                    "UC3 - Assign Driver", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Assignment Error", JOptionPane.ERROR_MESSAGE);
            refreshData();
        }
    }
}
