package fastship.dao;

import fastship.model.Address;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for Address table (Table 25 & Table 40).
 */
public class AddressDAO {

    public List<Address> findByCustomerId(int customerId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            List<Address> list = new ArrayList<>();
            for (Address a : InMemoryStore.addresses) {
                if (a.getCustomerId() != null && a.getCustomerId() == customerId) {
                    list.add(a);
                }
            }
            return list;
        }
        List<Address> list = new ArrayList<>();
        String sql = "SELECT address_id, customer_id, contact_name, phone, street, ward, district, city, area " +
                     "FROM Address WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying customer addresses: " + e.getMessage(), e);
        }
        return list;
    }

    public Address findById(int addressId) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            return InMemoryStore.addresses.stream()
                    .filter(a -> a.getAddressId() == addressId)
                    .findFirst()
                    .orElse(null);
        }
        String sql = "SELECT address_id, customer_id, contact_name, phone, street, ward, district, city, area " +
                     "FROM Address WHERE address_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, addressId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding Address: " + e.getMessage(), e);
        }
        return null;
    }

    public int insert(Connection conn, Address address) throws SQLException {
        if (!DatabaseConnection.isMySqlAvailable()) {
            address.setAddressId(InMemoryStore.nextAddressId());
            InMemoryStore.addresses.add(address);
            return address.getAddressId();
        }
        String sql = "INSERT INTO Address (customer_id, contact_name, phone, street, ward, district, city, area) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (address.getCustomerId() != null) {
                ps.setInt(1, address.getCustomerId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, address.getContactName());
            ps.setString(3, address.getPhone());
            ps.setString(4, address.getStreet());
            ps.setString(5, address.getWard());
            ps.setString(6, address.getDistrict());
            ps.setString(7, address.getCity());
            ps.setString(8, address.getArea());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    address.setAddressId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to insert Address");
    }

    public void update(Address address) {
        if (!DatabaseConnection.isMySqlAvailable()) {
            Address existing = findById(address.getAddressId());
            if (existing != null) {
                existing.setContactName(address.getContactName());
                existing.setPhone(address.getPhone());
                existing.setStreet(address.getStreet());
                existing.setWard(address.getWard());
                existing.setDistrict(address.getDistrict());
                existing.setCity(address.getCity());
                existing.setArea(address.getArea());
            }
            return;
        }
        String sql = "UPDATE Address SET contact_name=?, phone=?, street=?, ward=?, district=?, city=?, area=? WHERE address_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, address.getContactName());
            ps.setString(2, address.getPhone());
            ps.setString(3, address.getStreet());
            ps.setString(4, address.getWard());
            ps.setString(5, address.getDistrict());
            ps.setString(6, address.getCity());
            ps.setString(7, address.getArea());
            ps.setInt(8, address.getAddressId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating Address: " + e.getMessage(), e);
        }
    }

    private Address mapRow(ResultSet rs) throws SQLException {
        int custId = rs.getInt("customer_id");
        Integer customerId = rs.wasNull() ? null : custId;
        return new Address(
                rs.getInt("address_id"),
                customerId,
                rs.getString("contact_name"),
                rs.getString("phone"),
                rs.getString("street"),
                rs.getString("ward"),
                rs.getString("district"),
                rs.getString("city"),
                rs.getString("area")
        );
    }
}
