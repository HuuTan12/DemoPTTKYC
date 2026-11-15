package fastship.dao;

import fastship.model.Role;
import fastship.model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for UserAccount table (Table 36 & Table 40).
 */
public class UserAccountDAO {

    public UserAccount findByUsername(String username) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            return InMemoryStore.userAccounts.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElse(null);
        }
        String sql = "SELECT user_id, username, password_hash, full_name, role, customer_id, driver_id, is_active " +
                     "FROM UserAccount WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying UserAccount: " + e.getMessage(), e);
        }
        return null;
    }

    public List<UserAccount> findAll() {
        if (!DatabaseConnection.isMySqlAvailable()) {
            return new ArrayList<>(InMemoryStore.userAccounts);
        }
        List<UserAccount> list = new ArrayList<>();
        String sql = "SELECT user_id, username, password_hash, full_name, role, customer_id, driver_id, is_active FROM UserAccount";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading UserAccounts: " + e.getMessage(), e);
        }
        return list;
    }

    private UserAccount mapRow(ResultSet rs) throws SQLException {
        UserAccount u = new UserAccount();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(Role.valueOf(rs.getString("role")));
        int custId = rs.getInt("customer_id");
        u.setCustomerId(rs.wasNull() ? null : custId);
        int drvId = rs.getInt("driver_id");
        u.setDriverId(rs.wasNull() ? null : drvId);
        u.setActive(rs.getBoolean("is_active"));
        return u;
    }
}
