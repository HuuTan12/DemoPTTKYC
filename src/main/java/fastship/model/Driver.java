package fastship.model;

/**
 * Driver entity (Figure 5 - Class Diagram & Table 29).
 */
public class Driver {
    private int driverId;
    private String fullName;
    private String phone;
    private String licenseNo;
    private String serviceArea;
    private DriverStatus status = DriverStatus.AVAILABLE;

    // Optional fields for UI display
    private Vehicle vehicle;
    private int currentOrdersCount;

    public Driver() {
    }

    public Driver(int driverId, String fullName, String phone, String licenseNo, String serviceArea, DriverStatus status) {
        this.driverId = driverId;
        this.fullName = fullName;
        this.phone = phone;
        this.licenseNo = licenseNo;
        this.serviceArea = serviceArea;
        this.status = status;
    }

    public boolean isAvailable() {
        return status == DriverStatus.AVAILABLE;
    }

    public int getDriverId() {
        return driverId;
    }

    public void setDriverId(int driverId) {
        this.driverId = driverId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLicenseNo() {
        return licenseNo;
    }

    public void setLicenseNo(String licenseNo) {
        this.licenseNo = licenseNo;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public int getCurrentOrdersCount() {
        return currentOrdersCount;
    }

    public void setCurrentOrdersCount(int currentOrdersCount) {
        this.currentOrdersCount = currentOrdersCount;
    }
}

