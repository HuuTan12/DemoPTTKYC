package fastship.model;

/**
 * Vehicle entity (Figure 5 - Class Diagram & Table 30).
 */
public class Vehicle {
    private int vehicleId;
    private Integer driverId;
    private String plateNo;
    private String vehicleType;
    private double capacityKg;

    public Vehicle() {
    }

    public Vehicle(int vehicleId, Integer driverId, String plateNo, String vehicleType, double capacityKg) {
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.plateNo = plateNo;
        this.vehicleType = vehicleType;
        this.capacityKg = capacityKg;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Integer getDriverId() {
        return driverId;
    }

    public void setDriverId(Integer driverId) {
        this.driverId = driverId;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public double getCapacityKg() {
        return capacityKg;
    }

    public void setCapacityKg(double capacityKg) {
        this.capacityKg = capacityKg;
    }
}

