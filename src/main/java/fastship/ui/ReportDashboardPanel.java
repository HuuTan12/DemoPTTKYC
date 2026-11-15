package fastship.ui;

import fastship.model.Driver;
import fastship.model.Order;
import fastship.model.OrderStatus;
import fastship.model.Payment;
import fastship.model.PaymentStatus;
import fastship.service.DeliveryService;
import fastship.service.DriverService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Screen 6.3.3: Report Dashboard (Figure 26 & Table 39 - FR-15).
 * Components match Table 39:
 * - Period selector ("Period: November 2026")
 * - Export button
 * - Summary cards (Total orders, Success rate, COD collected, COD differences)
 * - Orders by status chart/table
 * - COD by driver table (columns: DRIVER, ORDERS, COD (VND), DIFFERENCE)
 */
public class ReportDashboardPanel extends JPanel {

    private final DeliveryService deliveryService = new DeliveryService();
    private final DriverService driverService = new DriverService();

    private JComboBox<String> periodCombo;
    private JLabel totalOrdersVal;
    private JLabel successRateVal;
    private JLabel codCollectedVal;
    private JLabel codDiffCountVal;
    private DefaultTableModel statusTableModel;
    private DefaultTableModel driverCodTableModel;

    public ReportDashboardPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Header + Period selector + Export button (Figure 26)
        JPanel header = new JPanel(new BorderLayout());
        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        JLabel title = new JLabel("Reports");
        title.setFont(UiTheme.FONT_TITLE);
        JLabel sub = new JLabel("Order status, delivery success rate and COD totals for the selected period.");
        sub.setFont(UiTheme.FONT_SUBTITLE);
        titleBox.add(title);
        titleBox.add(sub);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        periodCombo = new JComboBox<>(new String[]{"Period: November 2026", "Period: October 2026", "Period: All time"});
        periodCombo.addActionListener(e -> refreshData());
        JButton exportBtn = UiTheme.createOutlineButton("Export");
        exportBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Report summary exported.", "Export Report", JOptionPane.INFORMATION_MESSAGE));
        topRight.add(periodCombo);
        topRight.add(exportBtn);

        header.add(titleBox, BorderLayout.WEST);
        header.add(topRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel centerCol = new JPanel(new BorderLayout(0, 12));

        // 4 Summary Cards (Figure 26)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        totalOrdersVal = new JLabel("0");
        successRateVal = new JLabel("0%");
        codCollectedVal = new JLabel("0 VND");
        codDiffCountVal = new JLabel("0");

        kpiGrid.add(buildKpiCard("Total orders", totalOrdersVal));
        kpiGrid.add(buildKpiCard("Success rate", successRateVal));
        kpiGrid.add(buildKpiCard("COD collected", codCollectedVal));
        kpiGrid.add(buildKpiCard("COD differences", codDiffCountVal));
        centerCol.add(kpiGrid, BorderLayout.NORTH);

        // Two panels side by side: Orders by status & COD by driver (Figure 26)
        JPanel bottomGrid = new JPanel(new GridLayout(1, 2, 12, 0));

        JPanel statusCard = UiTheme.createCardPanel("Orders by status");
        statusCard.setLayout(new BorderLayout());
        statusTableModel = new DefaultTableModel(new Object[]{"STATUS", "COUNT"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable stTable = new JTable(statusTableModel);
        UiTheme.styleTable(stTable);
        statusCard.add(new JScrollPane(stTable), BorderLayout.CENTER);

        JPanel drvCard = UiTheme.createCardPanel("COD by driver");
        drvCard.setLayout(new BorderLayout());
        driverCodTableModel = new DefaultTableModel(
                new Object[]{"DRIVER", "ORDERS", "COD (VND)", "DIFFERENCE"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable drvTable = new JTable(driverCodTableModel);
        UiTheme.styleTable(drvTable);
        drvCard.add(new JScrollPane(drvTable), BorderLayout.CENTER);

        bottomGrid.add(statusCard);
        bottomGrid.add(drvCard);
        centerCol.add(bottomGrid, BorderLayout.CENTER);

        add(centerCol, BorderLayout.CENTER);
    }

    private JPanel buildKpiCard(String title, JLabel valueLabel) {
        JPanel card = UiTheme.createCardPanel(title);
        card.setLayout(new BorderLayout());
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void refreshData() {
        List<Order> orders = deliveryService.getAllOrdersForReport();
        List<Payment> payments = deliveryService.getAllPaymentsForReport();
        List<Driver> drivers = driverService.getAllDrivers();

        int total = orders.size();
        long deliveredCount = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        double rate = total > 0 ? (deliveredCount * 100.0 / total) : 0.0;

        long totalCollected = payments.stream()
                .filter(p -> p.getReceivedAmount() != null)
                .mapToLong(Payment::getReceivedAmount)
                .sum();
        long diffCount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.DIFFERENCE)
                .count();

        totalOrdersVal.setText(String.valueOf(total));
        successRateVal.setText(String.format("%.1f%%", rate));
        codCollectedVal.setText(UiTheme.formatVnd(totalCollected));
        codDiffCountVal.setText(String.valueOf(diffCount));

        Map<OrderStatus, Integer> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) counts.put(s, 0);
        for (Order o : orders) counts.put(o.getStatus(), counts.get(o.getStatus()) + 1);

        statusTableModel.setRowCount(0);
        for (OrderStatus s : OrderStatus.values()) {
            statusTableModel.addRow(new Object[]{s.name(), counts.get(s)});
        }

        driverCodTableModel.setRowCount(0);
        for (Driver d : drivers) {
            int codOrderCount = 0;
            long expected = 0;
            long received = 0;
            for (Payment p : payments) {
                if (p.getDriverId() == d.getDriverId()) {
                    codOrderCount++;
                    expected += p.getCodAmount();
                    received += (p.getReceivedAmount() != null ? p.getReceivedAmount() : 0);
                }
            }
            long diff = received - expected;
            driverCodTableModel.addRow(new Object[]{
                    d.getFullName(),
                    codOrderCount,
                    UiTheme.formatNumber(received),
                    diff == 0 ? "0" : String.format("%+,d", diff)
            });
        }
    }
}
