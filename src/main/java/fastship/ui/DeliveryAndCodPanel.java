package fastship.ui;

import fastship.model.DeliveryProof;
import fastship.model.Driver;
import fastship.model.Payment;
import fastship.model.PaymentStatus;
import fastship.model.UserAccount;
import fastship.service.DeliveryService;
import fastship.service.DriverService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Screen 6.3.2: Delivery Confirmation and COD Reconciliation (Figure 25 & Table 39 - FR-13, FR-14, UC6).
 * Components match Table 39:
 * - Deliveries waiting for confirmation with driver and date filters (columns: ORDER, DRIVER, DELIVERED AT, COD (VND), STATUS)
 * - Delivery proof details (Received by, Delivered at, Note, Photo)
 * - Confirm delivery and Return to driver buttons
 * - COD amount and Cash received fields, match indicator, Reconcile button
 */
public class DeliveryAndCodPanel extends JPanel {

    private final UserAccount currentUser;
    private final DeliveryService deliveryService = new DeliveryService();
    private final DriverService driverService = new DriverService();

    private JComboBox<String> driverFilterCombo;
    private JTextField dateFilterField;
    private JPanel leftCard;
    private DefaultTableModel deliveryTableModel;
    private JTable deliveryTable;
    private List<DeliveryProof> proofs;
    private List<Driver> drivers;
    private DeliveryProof selectedProof;

    private JPanel proofCard;
    private JLabel receivedByLabel;
    private JLabel deliveredAtLabel;
    private JLabel proofNoteLabel;
    private JLabel proofPhotoLabel;
    private JButton confirmDeliveryBtn;
    private JButton returnToDriverBtn;

    private JTextField codAmountField;
    private JTextField cashReceivedField;
    private JTextField reconcileNoteField;
    private JLabel matchBadgeLabel;
    private JButton reconcileBtn;

    public DeliveryAndCodPanel(UserAccount currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Header + Driver & Date filters (Figure 25)
        JPanel header = new JPanel(new BorderLayout());
        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        JLabel title = new JLabel("Deliveries and COD");
        title.setFont(UiTheme.FONT_TITLE);
        JLabel sub = new JLabel("Check delivery proof, confirm the delivery, then reconcile the cash collected.");
        sub.setFont(UiTheme.FONT_SUBTITLE);
        titleBox.add(title);
        titleBox.add(sub);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        driverFilterCombo = new JComboBox<>();
        driverFilterCombo.addItem("Driver: All");
        drivers = driverService.getAllDrivers();
        for (Driver d : drivers) {
            driverFilterCombo.addItem(d.getDriverId() + " - " + d.getFullName());
        }
        driverFilterCombo.addActionListener(e -> refreshData());

        dateFilterField = UiTheme.createTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateFilterField.setPreferredSize(new Dimension(100, 26));

        filterPanel.add(driverFilterCombo);
        filterPanel.add(dateFilterField);

        header.add(titleBox, BorderLayout.WEST);
        header.add(filterPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Main Split (Figure 25)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.55);

        leftCard = UiTheme.createCardPanel("Waiting for confirmation (0)");
        leftCard.setLayout(new BorderLayout());
        deliveryTableModel = new DefaultTableModel(
                new Object[]{"ORDER", "DRIVER", "DELIVERED AT", "COD (VND)", "STATUS"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        deliveryTable = new JTable(deliveryTableModel);
        UiTheme.styleTable(deliveryTable);
        deliveryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int r = deliveryTable.getSelectedRow();
                if (r >= 0 && proofs != null && r < proofs.size()) {
                    showProofDetails(proofs.get(r));
                }
            }
        });
        leftCard.add(new JScrollPane(deliveryTable), BorderLayout.CENTER);

        // Right column: Delivery Proof Card + COD Reconciliation Card (Figure 25)
        JPanel rightCol = new JPanel(new GridLayout(2, 1, 0, 10));

        proofCard = UiTheme.createCardPanel("Delivery proof");
        proofCard.setLayout(new BorderLayout(0, 8));
        JPanel proofGrid = new JPanel(new GridLayout(4, 2, 8, 6));
        receivedByLabel = new JLabel("—");
        deliveredAtLabel = new JLabel("—");
        proofNoteLabel = new JLabel("—");
        proofPhotoLabel = new JLabel("—");

        proofGrid.add(UiTheme.createFieldLabel("Received by"));
        proofGrid.add(receivedByLabel);
        proofGrid.add(UiTheme.createFieldLabel("Delivered at"));
        proofGrid.add(deliveredAtLabel);
        proofGrid.add(UiTheme.createFieldLabel("Note"));
        proofGrid.add(proofNoteLabel);
        proofGrid.add(UiTheme.createFieldLabel("Photo"));
        proofGrid.add(proofPhotoLabel);
        proofCard.add(proofGrid, BorderLayout.CENTER);

        JPanel proofBtns = new JPanel(new GridLayout(1, 2, 8, 0));
        confirmDeliveryBtn = UiTheme.createPrimaryButton("Confirm delivery");
        confirmDeliveryBtn.addActionListener(e -> handleConfirmDelivery());
        returnToDriverBtn = UiTheme.createOutlineButton("Return to driver");
        returnToDriverBtn.addActionListener(e -> handleReturnToDriver());
        proofBtns.add(confirmDeliveryBtn);
        proofBtns.add(returnToDriverBtn);
        proofCard.add(proofBtns, BorderLayout.SOUTH);

        JPanel codCard = UiTheme.createCardPanel("COD reconciliation");
        codCard.setLayout(new BorderLayout(0, 8));
        JPanel codForm = new JPanel(new GridLayout(3, 2, 8, 6));
        codAmountField = UiTheme.createTextField("0");
        codAmountField.setEditable(false);
        cashReceivedField = UiTheme.createTextField("0");
        cashReceivedField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateMatchIndicator(); }
            @Override public void removeUpdate(DocumentEvent e) { updateMatchIndicator(); }
            @Override public void changedUpdate(DocumentEvent e) { updateMatchIndicator(); }
        });
        reconcileNoteField = UiTheme.createTextField("");

        codForm.add(UiTheme.createFieldLabel("COD amount"));
        codForm.add(UiTheme.createFieldLabel("Cash received"));
        codForm.add(codAmountField);
        codForm.add(cashReceivedField);
        codForm.add(UiTheme.createFieldLabel("Note (required if Difference)"));
        codForm.add(reconcileNoteField);
        codCard.add(codForm, BorderLayout.CENTER);

        JPanel codFooter = new JPanel(new BorderLayout());
        matchBadgeLabel = new JLabel("Matches");
        matchBadgeLabel.setFont(UiTheme.FONT_LABEL);
        reconcileBtn = UiTheme.createPrimaryButton("Reconcile");
        reconcileBtn.addActionListener(e -> handleReconcileCod());
        codFooter.add(matchBadgeLabel, BorderLayout.WEST);
        codFooter.add(reconcileBtn, BorderLayout.EAST);
        codCard.add(codFooter, BorderLayout.SOUTH);

        rightCol.add(proofCard);
        rightCol.add(codCard);

        splitPane.setLeftComponent(leftCard);
        splitPane.setRightComponent(rightCol);
        add(splitPane, BorderLayout.CENTER);
    }

    public void refreshData() {
        Integer driverFilter = null;
        String selDrv = (String) driverFilterCombo.getSelectedItem();
        if (selDrv != null && !selDrv.startsWith("Driver:")) {
            driverFilter = Integer.parseInt(selDrv.split(" - ")[0].trim());
        }

        proofs = deliveryService.getDeliveriesForStaff(driverFilter);
        leftCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Waiting for confirmation (" + proofs.size() + ")"),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        deliveryTableModel.setRowCount(0);
        for (DeliveryProof dp : proofs) {
            String code = String.format("DH%04d", dp.getOrderId());
            Payment p = dp.getPayment();
            String statusText;
            if (dp.getConfirmedBy() == null) {
                statusText = "Waiting";
            } else if (p != null) {
                statusText = p.getStatus().name();
            } else {
                statusText = "Confirmed";
            }
            deliveryTableModel.addRow(new Object[]{
                    code,
                    dp.getDriverName() != null ? dp.getDriverName() : ("Driver #" + dp.getDriverId()),
                    UiTheme.formatDateTime(dp.getDeliveredAt()),
                    UiTheme.formatNumber(dp.getCodAmount()),
                    statusText
            });
        }
        if (!proofs.isEmpty()) {
            deliveryTable.setRowSelectionInterval(0, 0);
            showProofDetails(proofs.get(0));
        }
    }

    private void showProofDetails(DeliveryProof dp) {
        this.selectedProof = dp;
        String code = String.format("DH%04d", dp.getOrderId());
        proofCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Delivery proof · " + code),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        receivedByLabel.setText(dp.getReceivedBy());
        deliveredAtLabel.setText(UiTheme.formatDateTime(dp.getDeliveredAt()));
        proofNoteLabel.setText(dp.getNote() != null ? dp.getNote() : "—");
        proofPhotoLabel.setText(dp.getPhotoRef() != null ? dp.getPhotoRef() : "—");

        confirmDeliveryBtn.setEnabled(dp.getConfirmedBy() == null);

        codAmountField.setText(String.valueOf(dp.getCodAmount()));
        Payment p = dp.getPayment();
        if (p != null && p.getReceivedAmount() != null) {
            cashReceivedField.setText(String.valueOf(p.getReceivedAmount()));
            reconcileNoteField.setText(p.getNote() != null ? p.getNote() : "");
        } else {
            cashReceivedField.setText(String.valueOf(dp.getCodAmount()));
            reconcileNoteField.setText("");
        }
        reconcileBtn.setEnabled(dp.getCodAmount() > 0 && (p == null || p.getStatus() != PaymentStatus.RECONCILED));
        updateMatchIndicator();
    }

    private void updateMatchIndicator() {
        if (selectedProof == null) return;
        try {
            long expected = selectedProof.getCodAmount();
            long received = Long.parseLong(cashReceivedField.getText().trim().replace(",", ""));
            long diff = received - expected;
            if (diff == 0) {
                matchBadgeLabel.setText("Matches");
            } else {
                matchBadgeLabel.setText(String.format("Difference (%+,d VND)", diff));
            }
        } catch (NumberFormatException ex) {
            matchBadgeLabel.setText("Invalid amount");
        }
    }

    private void handleConfirmDelivery() {
        if (selectedProof == null) return;
        try {
            deliveryService.confirmDelivery(selectedProof.getProofId(), currentUser);
            JOptionPane.showMessageDialog(this,
                    "Delivery confirmed and customer notified (UC6 - FR-13).",
                    "UC6 - Confirm Delivery", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleReturnToDriver() {
        if (selectedProof == null) return;
        String note = JOptionPane.showInputDialog(this, "Enter reason to return proof to driver (UC6 - E1):",
                "Incomplete delivery proof");
        if (note != null) {
            try {
                deliveryService.returnToDriver(selectedProof.getProofId(), note, currentUser);
                JOptionPane.showMessageDialog(this, "Delivery proof returned to driver for correction (UC6 - E1).",
                        "Return to Driver", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleReconcileCod() {
        if (selectedProof == null) return;
        try {
            long received = Long.parseLong(cashReceivedField.getText().trim().replace(",", ""));
            Payment result = deliveryService.reconcileCod(
                    selectedProof.getOrderId(),
                    received,
                    reconcileNoteField.getText(),
                    currentUser
            );
            JOptionPane.showMessageDialog(this,
                    String.format("COD Reconciled!\nStatus: %s\nCOD Amount: %s\nCash Received: %s",
                            result.getStatus().name(),
                            UiTheme.formatVnd(result.getCodAmount()),
                            UiTheme.formatVnd(result.getReceivedAmount())),
                    "UC6 - Reconcile COD", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for Cash received.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "UC6 - Reconciliation Rule", JOptionPane.ERROR_MESSAGE);
        }
    }
}
