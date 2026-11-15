package fastship.ui;

import fastship.model.Address;
import fastship.model.Customer;
import fastship.model.Order;
import fastship.model.OrderItem;
import fastship.model.ServiceType;
import fastship.model.UserAccount;
import fastship.service.OrderService;
import fastship.service.PricingService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 6.1.1: Create Order (Figure 18 & Table 37 - FR-01, FR-02, UC1).
 * Components match Table 37:
 * - Sender and receiver fields (name, phone)
 * - Pickup and delivery address selectors/fields
 * - Item table (description, quantity, weight) with "+ Add item" and "Remove" actions
 * - Service type selector, COD field, note field
 * - Fee display, Cancel and Submit order buttons
 */
public class CreateOrderPanel extends JPanel {

    private final UserAccount currentUser;
    private final Runnable onOrderCreatedCallback;
    private final OrderService orderService = new OrderService();
    private final PricingService pricingService = new PricingService();

    private JTextField senderNameField;
    private JTextField senderPhoneField;
    private JComboBox<Address> pickupAddressCombo;

    private JTextField receiverNameField;
    private JTextField receiverPhoneField;
    private JTextField deliveryAddressField;
    private JComboBox<String> deliveryAreaCombo;

    private DefaultTableModel itemTableModel;
    private JTable itemTable;

    private JComboBox<ServiceType> serviceTypeCombo;
    private JTextField codAmountField;
    private JTextField noteField;

    private JLabel feeSummaryLabel;
    private JLabel feeAmountLabel;

    public CreateOrderPanel(UserAccount currentUser, Runnable onOrderCreatedCallback) {
        this.currentUser = currentUser;
        this.onOrderCreatedCallback = onOrderCreatedCallback;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        initComponents();
        loadCustomerDefaults();
        recalculateFeePreview();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        JLabel title = new JLabel("Create delivery order");
        title.setFont(UiTheme.FONT_TITLE);
        JLabel sub = new JLabel("Fill in sender, receiver and parcel details. The shipping fee is calculated before you submit.");
        sub.setFont(UiTheme.FONT_SUBTITLE);
        headerPanel.add(title);
        headerPanel.add(sub);
        add(headerPanel, BorderLayout.NORTH);

        // Center content
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // 1. Sender and pickup + Receiver and delivery (Figure 18)
        JPanel topRow = new JPanel(new GridLayout(1, 2, 12, 0));

        // Sender panel
        JPanel senderPanel = UiTheme.createCardPanel("Sender and pickup");
        senderPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        senderNameField = UiTheme.createTextField("");
        senderPhoneField = UiTheme.createTextField("");
        pickupAddressCombo = new JComboBox<>();

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5;
        senderPanel.add(UiTheme.createFieldLabel("Sender name"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.5;
        senderPanel.add(UiTheme.createFieldLabel("Phone"), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        senderPanel.add(senderNameField, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        senderPanel.add(senderPhoneField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        senderPanel.add(UiTheme.createFieldLabel("Pickup address"), gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        senderPanel.add(pickupAddressCombo, gbc);

        // Receiver panel
        JPanel receiverPanel = UiTheme.createCardPanel("Receiver and delivery");
        receiverPanel.setLayout(new GridBagLayout());
        receiverNameField = UiTheme.createTextField("Tran Thi Dung");
        receiverPhoneField = UiTheme.createTextField("0912345678");
        deliveryAddressField = UiTheme.createTextField("120 Nguyen Thi Thap, District 7, HCMC");
        deliveryAreaCombo = new JComboBox<>(new String[]{"D7", "D1", "D3", "D5"});

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5;
        receiverPanel.add(UiTheme.createFieldLabel("Receiver name"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.5;
        receiverPanel.add(UiTheme.createFieldLabel("Phone"), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        receiverPanel.add(receiverNameField, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        receiverPanel.add(receiverPhoneField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        receiverPanel.add(UiTheme.createFieldLabel("Delivery address"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        receiverPanel.add(UiTheme.createFieldLabel("Area"), gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        receiverPanel.add(deliveryAddressField, gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        receiverPanel.add(deliveryAreaCombo, gbc);

        topRow.add(senderPanel);
        topRow.add(receiverPanel);
        centerPanel.add(topRow);
        centerPanel.add(Box.createVerticalStrut(10));

        // 2. Parcel items panel (Figure 18)
        JPanel itemsPanel = UiTheme.createCardPanel("Parcel items");
        itemsPanel.setLayout(new BorderLayout(6, 6));

        JPanel itemActionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton addItemBtn = UiTheme.createOutlineButton("+ Add item");
        addItemBtn.addActionListener(e -> promptAddItem());
        JButton removeItemBtn = UiTheme.createOutlineButton("Remove");
        removeItemBtn.addActionListener(e -> removeSelectedItem());
        itemActionRow.add(addItemBtn);
        itemActionRow.add(removeItemBtn);
        itemsPanel.add(itemActionRow, BorderLayout.NORTH);

        itemTableModel = new DefaultTableModel(new Object[]{"DESCRIPTION", "QUANTITY", "WEIGHT (KG)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        itemTable = new JTable(itemTableModel);
        UiTheme.styleTable(itemTable);
        JScrollPane itemScroll = new JScrollPane(itemTable);
        itemScroll.setPreferredSize(new Dimension(700, 140));
        itemsPanel.add(itemScroll, BorderLayout.CENTER);

        centerPanel.add(itemsPanel);
        centerPanel.add(Box.createVerticalStrut(10));

        // 3. Service type, COD amount (VND), Note (Figure 18)
        JPanel optionsRow = new JPanel(new GridLayout(1, 3, 12, 0));

        JPanel serviceBox = new JPanel(new BorderLayout(0, 4));
        serviceBox.add(UiTheme.createFieldLabel("Service type"), BorderLayout.NORTH);
        serviceTypeCombo = new JComboBox<>();
        for (ServiceType st : pricingService.getActiveServiceTypes()) {
            serviceTypeCombo.addItem(st);
        }
        serviceTypeCombo.addActionListener(e -> recalculateFeePreview());
        serviceBox.add(serviceTypeCombo, BorderLayout.CENTER);

        JPanel codBox = new JPanel(new BorderLayout(0, 4));
        codBox.add(UiTheme.createFieldLabel("COD amount (VND)"), BorderLayout.NORTH);
        codAmountField = UiTheme.createTextField("450000");
        codBox.add(codAmountField, BorderLayout.CENTER);

        JPanel noteBox = new JPanel(new BorderLayout(0, 4));
        noteBox.add(UiTheme.createFieldLabel("Note"), BorderLayout.NORTH);
        noteField = UiTheme.createTextField("Call before delivery");
        noteBox.add(noteField, BorderLayout.CENTER);

        optionsRow.add(serviceBox);
        optionsRow.add(codBox);
        optionsRow.add(noteBox);
        centerPanel.add(optionsRow);

        add(centerPanel, BorderLayout.CENTER);

        // 4. Bottom bar: Shipping fee display + Cancel + Submit order (Figure 18)
        JPanel bottomBar = UiTheme.createCardPanel();
        bottomBar.setLayout(new BorderLayout());

        JPanel feeBox = new JPanel(new GridLayout(2, 1, 0, 2));
        feeSummaryLabel = new JLabel("Shipping fee · Standard · total 2.5 kg");
        feeSummaryLabel.setFont(UiTheme.FONT_SUBTITLE);
        feeAmountLabel = new JLabel("32,500 VND");
        feeAmountLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        feeBox.add(feeSummaryLabel);
        feeBox.add(feeAmountLabel);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton cancelBtn = UiTheme.createOutlineButton("Cancel");
        cancelBtn.addActionListener(e -> resetForm());
        JButton submitBtn = UiTheme.createPrimaryButton("Submit order");
        submitBtn.addActionListener(e -> handleSubmitOrder());
        btnPanel.add(cancelBtn);
        btnPanel.add(submitBtn);

        bottomBar.add(feeBox, BorderLayout.WEST);
        bottomBar.add(btnPanel, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private void loadCustomerDefaults() {
        int custId = currentUser.getCustomerId() != null ? currentUser.getCustomerId() : 1;
        Customer cust = orderService.getCustomer(custId);
        if (cust != null) {
            senderNameField.setText(cust.getFullName());
            senderPhoneField.setText(cust.getPhone());
        }
        pickupAddressCombo.removeAllItems();
        for (Address a : orderService.getSavedAddresses(custId)) {
            pickupAddressCombo.addItem(a);
        }
        resetForm();
    }

    private void resetForm() {
        receiverNameField.setText("Tran Thi Dung");
        receiverPhoneField.setText("0912345678");
        deliveryAddressField.setText("120 Nguyen Thi Thap, District 7, HCMC");
        deliveryAreaCombo.setSelectedItem("D7");

        itemTableModel.setRowCount(0);
        itemTableModel.addRow(new Object[]{"Book box", 1, 1.5});
        itemTableModel.addRow(new Object[]{"Phone case", 1, 1.0});

        if (serviceTypeCombo.getItemCount() > 0) {
            serviceTypeCombo.setSelectedIndex(0);
        }
        codAmountField.setText("450000");
        noteField.setText("Call before delivery");
        recalculateFeePreview();
    }

    private void promptAddItem() {
        JTextField descField = new JTextField();
        JTextField qtyField = new JTextField("1");
        JTextField weightField = new JTextField("1.0");

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Description:"));
        form.add(descField);
        form.add(new JLabel("Quantity:"));
        form.add(qtyField);
        form.add(new JLabel("Weight (kg):"));
        form.add(weightField);

        int res = JOptionPane.showConfirmDialog(this, form, "Add Parcel Item",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String desc = descField.getText().trim();
                int qty = Integer.parseInt(qtyField.getText().trim());
                double weight = Double.parseDouble(weightField.getText().trim());
                if (desc.isEmpty() || qty <= 0 || weight <= 0) {
                    throw new IllegalArgumentException();
                }
                itemTableModel.addRow(new Object[]{desc, qty, weight});
                recalculateFeePreview();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a non-empty description, positive integer quantity, and weight > 0.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeSelectedItem() {
        int row = itemTable.getSelectedRow();
        if (row >= 0) {
            itemTableModel.removeRow(row);
            recalculateFeePreview();
        } else {
            JOptionPane.showMessageDialog(this, "Select an item row in the table to remove.");
        }
    }

    private double getTotalWeightFromTable() {
        double total = 0;
        for (int i = 0; i < itemTableModel.getRowCount(); i++) {
            total += Double.parseDouble(itemTableModel.getValueAt(i, 2).toString());
        }
        return total;
    }

    private void recalculateFeePreview() {
        ServiceType st = (ServiceType) serviceTypeCombo.getSelectedItem();
        double totalWeight = getTotalWeightFromTable();
        if (st == null) return;
        feeSummaryLabel.setText(String.format("Shipping fee · %s · total %.1f kg", st.getName(), totalWeight));
        try {
            long fee = pricingService.getFee(st, totalWeight);
            feeAmountLabel.setText(UiTheme.formatVnd(fee));
        } catch (IllegalArgumentException ex) {
            feeAmountLabel.setText(ex.getMessage());
        }
    }

    private void handleSubmitOrder() {
        try {
            int custId = currentUser.getCustomerId() != null ? currentUser.getCustomerId() : 1;
            Address savedPickup = (Address) pickupAddressCombo.getSelectedItem();
            Address pickup;
            if (savedPickup != null) {
                pickup = new Address(
                        savedPickup.getAddressId(),
                        custId,
                        senderNameField.getText().trim(),
                        senderPhoneField.getText().trim().replaceAll("\\s+", ""),
                        savedPickup.getStreet(),
                        savedPickup.getWard(),
                        savedPickup.getDistrict(),
                        savedPickup.getCity(),
                        savedPickup.getArea()
                );
            } else {
                pickup = new Address(0, custId,
                        senderNameField.getText().trim(),
                        senderPhoneField.getText().trim().replaceAll("\\s+", ""),
                        "19 Nguyen Huu Tho", "Tan Phong Ward", "District 7", "Ho Chi Minh City", "D7");
            }

            String rawDeliv = deliveryAddressField.getText().trim();
            String area = (String) deliveryAreaCombo.getSelectedItem();
            Address delivery = new Address(
                    0,
                    null,
                    receiverNameField.getText().trim(),
                    receiverPhoneField.getText().trim().replaceAll("\\s+", ""),
                    rawDeliv,
                    "",
                    "District (" + area + ")",
                    "Ho Chi Minh City",
                    area
            );

            List<OrderItem> items = new ArrayList<>();
            for (int i = 0; i < itemTableModel.getRowCount(); i++) {
                String desc = itemTableModel.getValueAt(i, 0).toString();
                int qty = Integer.parseInt(itemTableModel.getValueAt(i, 1).toString());
                double weight = Double.parseDouble(itemTableModel.getValueAt(i, 2).toString());
                items.add(new OrderItem(desc, qty, weight));
            }

            ServiceType st = (ServiceType) serviceTypeCombo.getSelectedItem();
            long cod = 0;
            String codText = codAmountField.getText().trim().replace(",", "");
            if (!codText.isEmpty()) {
                cod = Long.parseLong(codText);
            }

            Order created = orderService.createOrder(
                    custId,
                    pickup,
                    delivery,
                    items,
                    st != null ? st.getServiceTypeId() : 1,
                    cod,
                    noteField.getText().trim(),
                    currentUser.getUsername()
            );

            JOptionPane.showMessageDialog(this,
                    String.format("Order created successfully!\nOrder Code: %s\nStatus: %s\nShipping Fee: %s",
                            created.getOrderCode(),
                            created.getStatus(),
                            UiTheme.formatVnd(created.getShippingFee())),
                    "UC1 - Create Order",
                    JOptionPane.INFORMATION_MESSAGE);

            if (onOrderCreatedCallback != null) {
                onOrderCreatedCallback.run();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "COD amount must be a valid number.",
                    "Validation Error (UC1 - E1)", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Validation Error (UC1 - E1 / E2)", JOptionPane.ERROR_MESSAGE);
        }
    }
}
