package fastship.ui;

import fastship.model.Address;
import fastship.model.Order;
import fastship.model.TrackingEvent;
import fastship.model.UserAccount;
import fastship.service.TrackingService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Screen 6.3.1: Order Tracking (Figure 24 & Table 39 - FR-11, FR-12, UC5).
 * Components match Table 39:
 * - Order code box
 * - Search button
 * - Status badge, driver name, delivery information
 * - Timeline of events
 */
public class OrderTrackingPanel extends JPanel {

    private final UserAccount currentUser;
    private final TrackingService trackingService = new TrackingService();

    private JTextField orderCodeField;
    private JLabel orderTitleLabel;
    private JLabel statusBadgeLabel;
    private JLabel receiverLabel;
    private JLabel deliveryAddressLabel;
    private JLabel serviceLabel;
    private JLabel driverLabel;
    private JLabel codLabel;
    private DefaultTableModel timelineModel;

    public OrderTrackingPanel(UserAccount currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        initComponents();
    }

    private void initComponents() {
        // Top Header + Order code box + Search button (Figure 24)
        JPanel topPanel = new JPanel(new BorderLayout(0, 8));
        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        JLabel title = new JLabel("Track your order");
        title.setFont(UiTheme.FONT_TITLE);
        JLabel sub = new JLabel("Enter an order code to see its current status and full history.");
        sub.setFont(UiTheme.FONT_SUBTITLE);
        titleBox.add(title);
        titleBox.add(sub);
        topPanel.add(titleBox, BorderLayout.NORTH);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        orderCodeField = UiTheme.createTextField("DH0002");
        orderCodeField.setPreferredSize(new Dimension(260, 28));
        JButton searchBtn = UiTheme.createPrimaryButton("Search");
        searchBtn.addActionListener(e -> handleLookup());

        searchRow.add(orderCodeField);
        searchRow.add(searchBtn);
        topPanel.add(searchRow, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Two panels side by side: Order info & Timeline (Figure 24)
        JPanel cardsGrid = new JPanel(new GridLayout(1, 2, 12, 0));

        // Left: Order summary
        JPanel summaryCard = UiTheme.createCardPanel("Order information");
        summaryCard.setLayout(new BorderLayout(0, 10));

        JPanel summaryTop = new JPanel(new BorderLayout());
        orderTitleLabel = new JLabel("Order —");
        orderTitleLabel.setFont(UiTheme.FONT_CARD_TITLE);
        statusBadgeLabel = new JLabel("—");
        statusBadgeLabel.setFont(UiTheme.FONT_LABEL);
        summaryTop.add(orderTitleLabel, BorderLayout.WEST);
        summaryTop.add(statusBadgeLabel, BorderLayout.EAST);
        summaryCard.add(summaryTop, BorderLayout.NORTH);

        JPanel detailsGrid = new JPanel(new GridLayout(5, 2, 8, 8));
        receiverLabel = new JLabel("—");
        deliveryAddressLabel = new JLabel("—");
        serviceLabel = new JLabel("—");
        driverLabel = new JLabel("—");
        driverLabel.setFont(UiTheme.FONT_LABEL);
        codLabel = new JLabel("—");

        detailsGrid.add(UiTheme.createFieldLabel("Receiver"));
        detailsGrid.add(receiverLabel);
        detailsGrid.add(UiTheme.createFieldLabel("Delivery address"));
        detailsGrid.add(deliveryAddressLabel);
        detailsGrid.add(UiTheme.createFieldLabel("Service"));
        detailsGrid.add(serviceLabel);
        detailsGrid.add(UiTheme.createFieldLabel("Driver"));
        detailsGrid.add(driverLabel);
        detailsGrid.add(UiTheme.createFieldLabel("COD"));
        detailsGrid.add(codLabel);

        JPanel wrapGrid = new JPanel(new BorderLayout());
        wrapGrid.add(detailsGrid, BorderLayout.NORTH);
        summaryCard.add(wrapGrid, BorderLayout.CENTER);

        JLabel footerHint = new JLabel("Unknown codes and other customers' orders both show \"Order not found\".");
        footerHint.setFont(UiTheme.FONT_SUBTITLE);
        summaryCard.add(footerHint, BorderLayout.SOUTH);

        // Right: Timeline table
        JPanel timelineCard = UiTheme.createCardPanel("Timeline");
        timelineCard.setLayout(new BorderLayout());
        timelineModel = new DefaultTableModel(
                new Object[]{"STATUS", "DATE & TIME", "RECORDED BY", "NOTE"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable timelineTable = new JTable(timelineModel);
        UiTheme.styleTable(timelineTable);
        timelineCard.add(new JScrollPane(timelineTable), BorderLayout.CENTER);

        cardsGrid.add(summaryCard);
        cardsGrid.add(timelineCard);
        add(cardsGrid, BorderLayout.CENTER);
    }

    public void handleLookup() {
        try {
            Order o = trackingService.getOrderForTracking(orderCodeField.getText(), currentUser);
            orderTitleLabel.setText("Order " + o.getOrderCode());
            statusBadgeLabel.setText("[" + o.getStatus().name() + "]");
            Address d = o.getDeliveryAddress();
            receiverLabel.setText(d != null ? d.getContactName() : "—");
            deliveryAddressLabel.setText(d != null ? d.fullAddress() : "—");
            serviceLabel.setText(o.getServiceType() != null ? o.getServiceType().getName() : "Standard");
            driverLabel.setText(o.getAssignedDriverName() != null ? o.getAssignedDriverName() : "—");
            codLabel.setText(UiTheme.formatVnd(o.getCodAmount()));

            List<TrackingEvent> events = trackingService.getTimeline(o.getOrderId());
            timelineModel.setRowCount(0);
            for (TrackingEvent ev : events) {
                timelineModel.addRow(new Object[]{
                        ev.getStatus().name(),
                        UiTheme.formatDateTime(ev.getEventTime()),
                        ev.getRecordedBy(),
                        ev.getNote()
                });
            }
        } catch (Exception ex) {
            orderTitleLabel.setText("Order not found");
            statusBadgeLabel.setText("—");
            receiverLabel.setText("—");
            deliveryAddressLabel.setText("—");
            serviceLabel.setText("—");
            driverLabel.setText("—");
            codLabel.setText("—");
            timelineModel.setRowCount(0);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "UC5 - Track Order Status", JOptionPane.WARNING_MESSAGE);
        }
    }
}
