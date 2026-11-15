package fastship.ui;

import fastship.model.Driver;
import fastship.model.DriverStatus;
import fastship.model.Vehicle;
import fastship.service.DriverService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 6.2.1: Driver Management (Figure 21 & Table 38 - FR-06, FR-07).
 * Components match Table 38:
 * - Driver table (name, phone, license, area, status, vehicle)
 * - Search box ("Search by name or phone")
 * - Status filter ("Status: All")
 * - "+ Add driver" button
 * - Profile form (name, phone, license, service area), status selector
 * - Vehicle form (plate number, type, capacity)
 * - Cancel and Save driver buttons
 */
public class DriverManagementPanel extends JPanel {

    private final DriverService driverService = new DriverService();

    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;
    private DefaultTableModel driverTableModel;
    private JTable driverTable;
    private List<Driver> displayedDrivers = new ArrayList<>();
    private Driver selectedDriver;

    private JLabel formTitleLabel;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField licenseField;
    private JComboBox<String> areaCombo;
    private JComboBox<DriverStatus> statusCombo;
    private JTextField plateField;
    private JComboBox<String> vehicleTypeCombo;
    private JTextField capacityField;

    public DriverManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Header + Search & Status Filter + "+ Add driver" button (Figure 21)
        JPanel topContainer = new JPanel(new BorderLayout(0, 8));

        JPanel headerRow = new JPanel(new BorderLayout());
        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        JLabel title = new JLabel("Drivers and vehicles");
        title.setFont(UiTheme.FONT_TITLE);
        JLabel sub = new JLabel("Maintain driver profiles, operating status and assigned vehicles.");
        sub.setFont(UiTheme.FONT_SUBTITLE);
        titleBox.add(title);
        titleBox.add(sub);

        JButton addDriverBtn = UiTheme.createPrimaryButton("+ Add driver");
        addDriverBtn.addActionListener(e -> clearFormForNewDriver());
        headerRow.add(titleBox, BorderLayout.WEST);
        headerRow.add(addDriverBtn, BorderLayout.EAST);
        topContainer.add(headerRow, BorderLayout.NORTH);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField = UiTheme.createTextField("");
        searchField.setPreferredSize(new Dimension(240, 28));
        statusFilterCombo = new JComboBox<>(new String[]{"Status: All", "AVAILABLE", "BUSY", "OFF"});
        statusFilterCombo.addActionListener(e -> refreshData());
        JButton searchBtn = UiTheme.createOutlineButton("Search");
        searchBtn.addActionListener(e -> refreshData());

        filterBar.add(new JLabel("Search by name or phone:"));
        filterBar.add(searchField);
        filterBar.add(statusFilterCombo);
        filterBar.add(searchBtn);
        topContainer.add(filterBar, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);

        // Main Split: Left Driver Table & Right Driver + Vehicle Form (Figure 21)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        driverTableModel = new DefaultTableModel(
                new Object[]{"NAME", "PHONE", "LICENSE", "AREA", "STATUS", "VEHICLE"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        driverTable = new JTable(driverTableModel);
        UiTheme.styleTable(driverTable);
        driverTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int r = driverTable.getSelectedRow();
                if (r >= 0 && r < displayedDrivers.size()) {
                    populateForm(displayedDrivers.get(r));
                }
            }
        });
        splitPane.setLeftComponent(new JScrollPane(driverTable));

        // Right panel: Driver profile & Vehicle form (Figure 21)
        JPanel formCard = UiTheme.createCardPanel();
        formCard.setLayout(new BorderLayout(0, 10));
        formTitleLabel = new JLabel("Driver profile");
        formTitleLabel.setFont(UiTheme.FONT_CARD_TITLE);
        formCard.add(formTitleLabel, BorderLayout.NORTH);

        JPanel formBody = new JPanel(new GridLayout(9, 2, 8, 6));
        nameField = UiTheme.createTextField("");
        phoneField = UiTheme.createTextField("");
        licenseField = UiTheme.createTextField("");
        areaCombo = new JComboBox<>(new String[]{"D7", "D1", "D3", "D5"});
        statusCombo = new JComboBox<>(DriverStatus.values());
        plateField = UiTheme.createTextField("");
        vehicleTypeCombo = new JComboBox<>(new String[]{"Motorbike", "Van", "Truck"});
        capacityField = UiTheme.createTextField("50");

        formBody.add(UiTheme.createFieldLabel("Full name"));
        formBody.add(nameField);
        formBody.add(UiTheme.createFieldLabel("Phone"));
        formBody.add(phoneField);
        formBody.add(UiTheme.createFieldLabel("License number"));
        formBody.add(licenseField);
        formBody.add(UiTheme.createFieldLabel("Service area"));
        formBody.add(areaCombo);
        formBody.add(UiTheme.createFieldLabel("Operating status"));
        formBody.add(statusCombo);
        formBody.add(new JLabel("--- Vehicle ---"));
        formBody.add(new JLabel(""));
        formBody.add(UiTheme.createFieldLabel("Plate number"));
        formBody.add(plateField);
        formBody.add(UiTheme.createFieldLabel("Type"));
        formBody.add(vehicleTypeCombo);
        formBody.add(UiTheme.createFieldLabel("Capacity (kg)"));
        formBody.add(capacityField);

        formCard.add(formBody, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancelBtn = UiTheme.createOutlineButton("Cancel");
        cancelBtn.addActionListener(e -> {
            if (selectedDriver != null) populateForm(selectedDriver);
            else clearFormForNewDriver();
        });
        JButton saveBtn = UiTheme.createPrimaryButton("Save driver");
        saveBtn.addActionListener(e -> handleSaveDriver());
        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        formCard.add(btnRow, BorderLayout.SOUTH);

        splitPane.setRightComponent(formCard);
        add(splitPane, BorderLayout.CENTER);
    }

    public void refreshData() {
        List<Driver> all = driverService.getAllDrivers();
        String kw = searchField.getText().trim().toLowerCase();
        String stFilter = (String) statusFilterCombo.getSelectedItem();

        displayedDrivers.clear();
        driverTableModel.setRowCount(0);
        for (Driver d : all) {
            if (stFilter != null && !stFilter.startsWith("Status:")) {
                if (!d.getStatus().name().equalsIgnoreCase(stFilter)) continue;
            }
            if (!kw.isEmpty()) {
                if (!d.getFullName().toLowerCase().contains(kw) && !d.getPhone().toLowerCase().contains(kw)) {
                    continue;
                }
            }
            displayedDrivers.add(d);
            Vehicle v = d.getVehicle();
            String vehStr = v != null ? (v.getPlateNo() + " · " + v.getVehicleType()) : "—";
            driverTableModel.addRow(new Object[]{
                    d.getFullName(),
                    d.getPhone(),
                    d.getLicenseNo(),
                    d.getServiceArea(),
                    d.getStatus().name(),
                    vehStr
            });
        }
        if (!displayedDrivers.isEmpty()) {
            driverTable.setRowSelectionInterval(0, 0);
            populateForm(displayedDrivers.get(0));
        }
    }

    private void populateForm(Driver d) {
        this.selectedDriver = d;
        formTitleLabel.setText("Driver profile · " + d.getFullName());
        nameField.setText(d.getFullName());
        phoneField.setText(d.getPhone());
        licenseField.setText(d.getLicenseNo());
        areaCombo.setSelectedItem(d.getServiceArea());
        statusCombo.setSelectedItem(d.getStatus());

        Vehicle v = d.getVehicle();
        if (v != null) {
            plateField.setText(v.getPlateNo());
            vehicleTypeCombo.setSelectedItem(v.getVehicleType());
            capacityField.setText(String.valueOf(v.getCapacityKg()));
        } else {
            plateField.setText("");
            capacityField.setText("50");
        }
    }

    private void clearFormForNewDriver() {
        this.selectedDriver = null;
        driverTable.clearSelection();
        formTitleLabel.setText("Driver profile · New Driver");
        nameField.setText("");
        phoneField.setText("0903");
        licenseField.setText("B2-");
        areaCombo.setSelectedItem("D7");
        statusCombo.setSelectedItem(DriverStatus.AVAILABLE);
        plateField.setText("59X1-");
        vehicleTypeCombo.setSelectedItem("Motorbike");
        capacityField.setText("50");
    }

    private void handleSaveDriver() {
        try {
            Driver drv = new Driver();
            if (selectedDriver != null) {
                drv.setDriverId(selectedDriver.getDriverId());
                drv.setCurrentOrdersCount(selectedDriver.getCurrentOrdersCount());
            }
            drv.setFullName(nameField.getText().trim());
            drv.setPhone(phoneField.getText().trim());
            drv.setLicenseNo(licenseField.getText().trim());
            drv.setServiceArea((String) areaCombo.getSelectedItem());
            drv.setStatus((DriverStatus) statusCombo.getSelectedItem());

            Vehicle veh = new Vehicle();
            veh.setPlateNo(plateField.getText().trim());
            veh.setVehicleType((String) vehicleTypeCombo.getSelectedItem());
            veh.setCapacityKg(Double.parseDouble(capacityField.getText().trim()));

            driverService.saveDriverAndVehicle(drv, veh);
            JOptionPane.showMessageDialog(this, "Driver and vehicle saved successfully.",
                    "FR-06 & FR-07", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
