package fastship.dao;

import fastship.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO for Customer table (Table 24 & Table 40).
 */
public class CustomerDAO {

    public Customer findById(int customerId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            return InMemoryStore.customers.stream()
                    .filter(c -> c.getCustomerId() == customerId)
                    .findFirst()
                    .orElse(null);
        }
        String sql = "SELECT customer_id, full_name, phone, email FROM Customer WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                            rs.getInt("customer_id"),
                            rs.getString("full_name"),
                            rs.getString("phone"),
                            rs.getString("email")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying Customer: " + e.getMessage(), e);
        }
        return null;
    }
}
