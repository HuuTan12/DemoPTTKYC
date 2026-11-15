package fastship.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * User account entity (Figure 5 - Class Diagram & Table 36).
 */
public class UserAccount {
    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private Role role;
    private Integer customerId;
    private Integer driverId;
    private boolean active = true;

    public UserAccount() {
    }

    public UserAccount(int userId, String username, String passwordHash, String fullName,
                       Role role, Integer customerId, Integer driverId, boolean active) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.customerId = customerId;
        this.driverId = driverId;
        this.active = active;
    }

    /**
     * Hashes a password with SHA-256, salted with the username, as a 64-character lowercase hex string
     * (Table 36: plain passwords are never stored). Same result as MySQL
     * SHA2(CONCAT(username, ':', password), 256), which sample_data.sql uses for the demo accounts.
     */
    public static String hashPassword(String username, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public boolean checkPassword(String pw) {
        if (pw == null || passwordHash == null || username == null) {
            return false;
        }
        byte[] expected = passwordHash.toLowerCase().getBytes(StandardCharsets.US_ASCII);
        byte[] actual = hashPassword(username, pw).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    public boolean hasRole(Role r) {
        return this.role == r;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getDriverId() {
        return driverId;
    }

    public void setDriverId(Integer driverId) {
        this.driverId = driverId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

