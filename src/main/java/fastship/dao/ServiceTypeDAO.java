package fastship.dao;

import fastship.model.ServiceType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for ServiceType table (Table 26 & Table 40).
 */
public class ServiceTypeDAO {

    public List<ServiceType> findAllActive() {
        if (!DatabaseConnection.isMySqlAvailable()) {
            List<ServiceType> list = new ArrayList<>();
            for (ServiceType st : InMemoryStore.serviceTypes) {
                if (st.isActive()) {
                    list.add(st);
                }
            }
            return list;
        }
        List<ServiceType> list = new ArrayList<>();
        String sql = "SELECT service_type_id, name, base_fee, fee_per_kg, max_weight_kg, is_active " +
                     "FROM ServiceType WHERE is_active = TRUE ORDER BY service_type_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading ServiceTypes: " + e.getMessage(), e);
        }
        return list;
    }

    public ServiceType findById(int serviceTypeId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            return InMemoryStore.serviceTypes.stream()
                    .filter(s -> s.getServiceTypeId() == serviceTypeId)
                    .findFirst()
                    .orElse(null);
        }
        String sql = "SELECT service_type_id, name, base_fee, fee_per_kg, max_weight_kg, is_active " +
                     "FROM ServiceType WHERE service_type_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serviceTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding ServiceType: " + e.getMessage(), e);
        }
        return null;
    }

    private ServiceType mapRow(ResultSet rs) throws SQLException {
        return new ServiceType(
                rs.getInt("service_type_id"),
                rs.getString("name"),
                rs.getLong("base_fee"),
                rs.getLong("fee_per_kg"),
                rs.getDouble("max_weight_kg"),
                rs.getBoolean("is_active")
        );
    }
}
