package fastship.dao;

import fastship.model.AssignmentStatus;
import fastship.model.Driver;
import fastship.model.DriverStatus;
import fastship.model.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for Driver table (Table 29 & Table 40).
 */
public class DriverDAO {

    private final VehicleDAO vehicleDAO = new VehicleDAO();

    public List<Driver> findAll() {
        if (!DatabaseConnection.isMySqlAvailable()) {
            for (Driver d : InMemoryStore.drivers) {
                int activeCount = (int) InMemoryStore.assignments.stream()
                        .filter(a -> a.getDriverId() == d.getDriverId()
                                && (a.getStatus() == AssignmentStatus.PENDING || a.getStatus() == AssignmentStatus.ACCEPTED))
                        .count();
                d.setCurrentOrdersCount(activeCount);
                d.setVehicle(vehicleDAO.findByDriverId(d.getDriverId()));
            }
            return new ArrayList<>(InMemoryStore.drivers);
        }
        List<Driver> list = new ArrayList<>();
        String sql = "SELECT d.*, " +
                     "(SELECT COUNT(*) FROM Assignment a WHERE a.driver_id = d.driver_id AND a.status IN ('PENDING', 'ACCEPTED')) AS current_orders " +
                     "FROM Driver d ORDER BY d.driver_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Driver d = mapRow(rs);
                d.setCurrentOrdersCount(rs.getInt("current_orders"));
                d.setVehicle(vehicleDAO.findByDriverId(d.getDriverId()));
                list.add(d);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading Drivers: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Driver> findAvailableByArea(String area) {
        List<Driver> all = findAll();
        List<Driver> filtered = new ArrayList<>();
        for (Driver d : all) {
            if (d.getStatus() == DriverStatus.AVAILABLE) {
                if (area == null || area.isBlank() || "ALL".equalsIgnoreCase(area)
                        || area.equalsIgnoreCase(d.getServiceArea())) {
                    filtered.add(d);
                }
            }
        }
        return filtered;
    }

    public Driver findById(int driverId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Driver d = InMemoryStore.drivers.stream()
                    .filter(drv -> drv.getDriverId() == driverId)
                    .findFirst()
                    .orElse(null);
            if (d != null) {
                d.setVehicle(vehicleDAO.findByDriverId(d.getDriverId()));
            }
            return d;
        }
        String sql = "SELECT d.*, " +
                     "(SELECT COUNT(*) FROM Assignment a WHERE a.driver_id = d.driver_id AND a.status IN ('PENDING', 'ACCEPTED')) AS current_orders " +
                     "FROM Driver d WHERE d.driver_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, driverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Driver d = mapRow(rs);
                    d.setCurrentOrdersCount(rs.getInt("current_orders"));
                    d.setVehicle(vehicleDAO.findByDriverId(d.getDriverId()));
                    return d;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding Driver: " + e.getMessage(), e);
        }
        return null;
    }

    public void updateStatus(Connection conn, int driverId, DriverStatus status) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Driver d = findById(driverId);
            if (d != null) d.setStatus(status);
            return;
        }
        String sql = "UPDATE Driver SET status = ? WHERE driver_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, driverId);
            ps.executeUpdate();
        }
    }

    public int saveOrUpdate(Driver driver, Vehicle vehicle) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            if (driver.getDriverId() <= 0) {
                driver.setDriverId(InMemoryStore.nextDriverId());
                InMemoryStore.drivers.add(driver);
            } else {
                Driver existing = findById(driver.getDriverId());
                if (existing != null) {
                    existing.setFullName(driver.getFullName());
                    existing.setPhone(driver.getPhone());
                    existing.setLicenseNo(driver.getLicenseNo());
                    existing.setServiceArea(driver.getServiceArea());
                    existing.setStatus(driver.getStatus());
                }
            }
            if (vehicle != null && vehicle.getPlateNo() != null && !vehicle.getPlateNo().isBlank()) {
                vehicle.setDriverId(driver.getDriverId());
                vehicleDAO.saveOrUpdate(vehicle);
                driver.setVehicle(vehicle);
            }
            return driver.getDriverId();
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (driver.getDriverId() <= 0) {
                String sql = "INSERT INTO Driver (full_name, phone, license_no, service_area, status) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, driver.getFullName());
                    ps.setString(2, driver.getPhone());
                    ps.setString(3, driver.getLicenseNo());
                    ps.setString(4, driver.getServiceArea());
                    ps.setString(5, driver.getStatus().name());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) driver.setDriverId(rs.getInt(1));
                    }
                }
            } else {
                String sql = "UPDATE Driver SET full_name=?, phone=?, license_no=?, service_area=?, status=? WHERE driver_id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, driver.getFullName());
                    ps.setString(2, driver.getPhone());
                    ps.setString(3, driver.getLicenseNo());
                    ps.setString(4, driver.getServiceArea());
                    ps.setString(5, driver.getStatus().name());
                    ps.setInt(6, driver.getDriverId());
                    ps.executeUpdate();
                }
            }
            if (vehicle != null && vehicle.getPlateNo() != null && !vehicle.getPlateNo().isBlank()) {
                vehicle.setDriverId(driver.getDriverId());
                vehicleDAO.saveOrUpdate(vehicle);
            }
            return driver.getDriverId();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving Driver: " + e.getMessage(), e);
        }
    }

    private Driver mapRow(ResultSet rs) throws SQLException {
        return new Driver(
                rs.getInt("driver_id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("license_no"),
                rs.getString("service_area"),
                DriverStatus.valueOf(rs.getString("status"))
        );
    }
}
