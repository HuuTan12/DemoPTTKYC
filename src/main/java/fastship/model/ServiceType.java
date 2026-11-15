package fastship.model;

/**
 * ServiceType entity (Figure 5 - Class Diagram & Table 26).
 */
public class ServiceType {
    private int serviceTypeId;
    private String name;
    private long baseFee;
    private long feePerKg;
    private double maxWeightKg;
    private boolean active = true;

    public ServiceType() {
    }

    public ServiceType(int serviceTypeId, String name, long baseFee, long feePerKg, double maxWeightKg, boolean active) {
        this.serviceTypeId = serviceTypeId;
        this.name = name;
        this.baseFee = baseFee;
        this.feePerKg = feePerKg;
        this.maxWeightKg = maxWeightKg;
        this.active = active;
    }

    /**
     * Formula in Table 27 & Table 41: base_fee + fee_per_kg * total_weight_kg
     */
    public long calculateFee(double weightKg) {
        if (weightKg <= 0) {
            return 0;
        }
        return Math.round(baseFee + feePerKg * weightKg);
    }

    @Override
    public String toString() {
        return String.format("%s (max %.0f kg)", name, maxWeightKg);
    }

    public int getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(int serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(long baseFee) {
        this.baseFee = baseFee;
    }

    public long getFeePerKg() {
        return feePerKg;
    }

    public void setFeePerKg(long feePerKg) {
        this.feePerKg = feePerKg;
    }

    public double getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(double maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

