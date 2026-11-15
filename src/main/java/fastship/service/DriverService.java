package fastship.service;

import fastship.dao.DriverDAO;
import fastship.model.Driver;
import fastship.model.DriverStatus;
import fastship.model.Vehicle;

import java.util.Comparator;
import java.util.List;

/**
 * DriverService (Figure 8: Sequence Diagram - UC3 Assign Driver, FR-06, FR-07, FR-09).
 */
public class DriverService {

    private final DriverDAO driverDAO = new DriverDAO();

    public List<Driver> getAllDrivers() {
        return driverDAO.findAll();
    }

    public List<Driver> getAvailableDrivers(String area) {
        return driverDAO.findAvailableByArea(area);
    }

    public Driver getDriverById(int driverId) {
        return driverDAO.findById(driverId);
    }

    /**
     * Suggest driver rule (FR-09 & UC3 - A1):
     * Proposes the available driver in the area with the fewest current orders.
     */
    public Driver suggestDriver(String area) {
        List<Driver> available = driverDAO.findAvailableByArea(area);
        if (available.isEmpty()) {
            return null;
        }
        return available.stream()
                .min(Comparator.comparingInt(Driver::getCurrentOrdersCount)
                        .thenComparingInt(Driver::getDriverId))
                .orElse(null);
    }

    public void saveDriverAndVehicle(Driver driver, Vehicle vehicle) {
        if (driver.getFullName() == null || driver.getFullName().isBlank()) {
            throw new IllegalArgumentException("Driver full name is required.");
        }
        if (driver.getPhone() == null || !driver.getPhone().replaceAll("\\s+", "").matches("^0\\d{9}$")) {
            throw new IllegalArgumentException("Driver phone must be 10 digits starting with 0.");
        }
        if (driver.getLicenseNo() == null || driver.getLicenseNo().isBlank()) {
            throw new IllegalArgumentException("License number is required.");
        }
        if (driver.getStatus() == DriverStatus.OFF && driver.getCurrentOrdersCount() > 0) {
            throw new IllegalStateException("Cannot set driver to OFF while an active assignment exists (Event E-13).");
        }
        driverDAO.saveOrUpdate(driver, vehicle);
    }
}
