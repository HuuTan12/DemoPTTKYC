package fastship.service;

import fastship.dao.ServiceTypeDAO;
import fastship.model.ServiceType;

import java.util.List;

/**
 * PricingService (Figure 6: Sequence Diagram - UC1 Create Order, FR-02).
 */
public class PricingService {

    private final ServiceTypeDAO serviceTypeDAO = new ServiceTypeDAO();

    public List<ServiceType> getActiveServiceTypes() {
        return serviceTypeDAO.findAllActive();
    }

    public ServiceType getServiceType(int serviceTypeId) {
        return serviceTypeDAO.findById(serviceTypeId);
    }

    /**
     * Sequence Diagram UC1 (Figure 6): getFee(serviceType, totalWeight)
     */
    public long getFee(ServiceType serviceType, double totalWeightKg) {
        if (serviceType == null) {
            throw new IllegalArgumentException("Please select a service type.");
        }
        if (totalWeightKg <= 0) {
            return 0;
        }
        if (totalWeightKg > serviceType.getMaxWeightKg()) {
            throw new IllegalArgumentException(String.format(
                    "Total weight (%.2f kg) exceeds the limit of %s service (max %.0f kg). Please choose another service.",
                    totalWeightKg, serviceType.getName(), serviceType.getMaxWeightKg()));
        }
        return serviceType.calculateFee(totalWeightKg);
    }
}
