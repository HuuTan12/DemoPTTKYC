package fastship.service;

import fastship.dao.UserAccountDAO;
import fastship.model.UserAccount;

import java.util.List;

/**
 * Authentication service for all 5 roles (Table 36 & Table 41).
 */
public class AuthService {

    private final UserAccountDAO userAccountDAO = new UserAccountDAO();

    public UserAccount login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Please enter username.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Please enter password.");
        }
        UserAccount account = userAccountDAO.findByUsername(username.trim());
        if (account == null || !account.isActive() || !account.checkPassword(password)) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        return account;
    }

    public List<UserAccount> getAllDemoAccounts() {
        return userAccountDAO.findAll();
    }
}
