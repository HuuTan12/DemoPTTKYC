package fastship.model;

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

    public boolean checkPassword(String pw) {
        return pw != null && pw.equals(this.passwordHash);
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

