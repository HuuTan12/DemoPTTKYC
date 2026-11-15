package fastship.dao;

import fastship.model.Vehicle;

import java.sql.*;

/**
 * DAO for Vehicle table (Table 30 & Table 40).
 */
public class VehicleDAO {

    public Vehicle findByDriverId(int driverId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            return InMemoryStore.vehicles.stream()
                    .filter(v -> v.getDriverId() != null && v.getDriverId() == driverId)
                    .findFirst()
                    .orElse(null);
        }
        String sql = "SELECT vehicle_id, driver_id, plate_no, vehicle_type, capacity_kg FROM Vehicle WHERE driver_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, driverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Vehicle(
                            rs.getInt("vehicle_id"),
                            rs.getInt("driver_id"),
                            rs.getString("plate_no"),
                            rs.getString("vehicle_type"),
                            rs.getDouble("capacity_kg")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying Vehicle: " + e.getMessage(), e);
        }
        return null;
    }

    public void saveOrUpdate(Vehicle vehicle) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Vehicle existing = vehicle.getDriverId() != null ? findByDriverId(vehicle.getDriverId()) : null;
            if (existing == null) {
                vehicle.setVehicleId(InMemoryStore.nextVehicleId());
                InMemoryStore.vehicles.add(vehicle);
            } else {
                existing.setPlateNo(vehicle.getPlateNo());
                existing.setVehicleType(vehicle.getVehicleType());
                existing.setCapacityKg(vehicle.getCapacityKg());
            }
            return;
        }
        Vehicle existing = vehicle.getDriverId() != null ? findByDriverId(vehicle.getDriverId()) : null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (existing == null) {
                String sql = "INSERT INTO Vehicle (driver_id, plate_no, vehicle_type, capacity_kg) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, vehicle.getDriverId());
                    ps.setString(2, vehicle.getPlateNo());
                    ps.setString(3, vehicle.getVehicleType());
                    ps.setDouble(4, vehicle.getCapacityKg());
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE Vehicle SET plate_no=?, vehicle_type=?, capacity_kg=? WHERE vehicle_id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, vehicle.getPlateNo());
                    ps.setString(2, vehicle.getVehicleType());
                    ps.setDouble(3, vehicle.getCapacityKg());
                    ps.setInt(4, existing.getVehicleId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving Vehicle: " + e.getMessage(), e);
        }
    }
}
