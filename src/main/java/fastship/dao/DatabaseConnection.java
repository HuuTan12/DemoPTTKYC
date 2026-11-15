package fastship.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/fastship?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&connectTimeout=1000";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static String url = System.getProperty("db.url",
            System.getenv().getOrDefault("FASTSHIP_DB_URL", DEFAULT_URL));
    private static String user = System.getProperty("db.user",
            System.getenv().getOrDefault("FASTSHIP_DB_USER", DEFAULT_USER));
    private static String password = System.getProperty("db.password",
            System.getenv().getOrDefault("FASTSHIP_DB_PASSWORD", DEFAULT_PASSWORD));

    private static Boolean mysqlAvailable = null;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
        }
    }

    private DatabaseConnection() {
    }

    public static void configure(String customUrl, String customUser, String customPassword) {
        url = customUrl;
        user = customUser;
        password = customPassword;
        mysqlAvailable = null;
    }

    public static synchronized boolean isMySqlAvailable() {
        if (Boolean.TRUE.equals(mysqlAvailable)) {
            return true;
        }
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            mysqlAvailable = (conn != null && !conn.isClosed());
        } catch (SQLException e) {
            mysqlAvailable = false;
        }
        return mysqlAvailable;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }
}
