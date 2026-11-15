package fastship.ui;

import fastship.dao.DatabaseConnection;
import fastship.model.Role;
import fastship.model.UserAccount;
import fastship.service.AuthService;

import javax.swing.*;
import java.awt.*;

/**
 * Main Java Swing Application Frame (1440 x 900) with default Swing look & feel
 * matching Chapter 6 layout (left role menu, top bar, and main content area).
 */
public class FastShipApp extends JFrame {

    private final AuthService authService = new AuthService();
    private UserAccount loggedInUser;

    private final CardLayout rootCardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(rootCardLayout);

    // Login screen fields
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;

    // Workspace shell components (Chapter 6: left role menu + top bar + main panel)
    private JPanel sidebarMenuPanel;
    private JLabel breadcrumbLabel;
    private JLabel roleBadgeLabel;
    private JLabel userFullNameLabel;
    private JPanel contentAreaPanel;

    public FastShipApp() {
        super("FastShip - Delivery Order Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        rootPanel.add(buildLoginScreen(), "LOGIN");
        rootPanel.add(buildMainShell(), "SHELL");
        setContentPane(rootPanel);
        rootCardLayout.show(rootPanel, "LOGIN");
    }

    private JPanel buildLoginScreen() {
        JPanel wrapper = new JPanel(new GridBagLayout());

        JPanel card = UiTheme.createCardPanel("FastShip - Login");
        card.setPreferredSize(new Dimension(420, 280));
        card.setLayout(new BorderLayout(10, 10));

        String dbMode = DatabaseConnection.isMySqlAvailable()
                ? "MySQL 8 Connected (localhost:3306/fastship)"
                : "Embedded Demo Store (Table 41 Sample Data)";
        JLabel dbLabel = new JLabel(dbMode, SwingConstants.CENTER);
        dbLabel.setFont(UiTheme.FONT_SUBTITLE);
        card.add(dbLabel, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 10));
        loginUsernameField = UiTheme.createTextField("customer01");
        loginPasswordField = new JPasswordField("123456");

        JComboBox<String> accountHintCombo = new JComboBox<>(new String[]{
                "customer01",
                "staff01",
                "dispatch01",
                "driver01",
                "driver02",
                "manager01"
        });
        accountHintCombo.addActionListener(e -> {
            String sel = (String) accountHintCombo.getSelectedItem();
            if (sel != null) {
                loginUsernameField.setText(sel);
                loginPasswordField.setText("123456");
            }
        });

        form.add(UiTheme.createFieldLabel("Sample account (Table 41):"));
        form.add(accountHintCombo);
        form.add(UiTheme.createFieldLabel("Username:"));
        form.add(loginUsernameField);
        form.add(UiTheme.createFieldLabel("Password:"));
        form.add(loginPasswordField);
        form.add(new JLabel(""));
        JButton loginBtn = UiTheme.createPrimaryButton("Login");
        loginBtn.addActionListener(e -> performLogin(
                loginUsernameField.getText(),
                new String(loginPasswordField.getPassword())
        ));
        form.add(loginBtn);

        card.add(form, BorderLayout.CENTER);
        wrapper.add(card);
        return wrapper;
    }

    private JPanel buildMainShell() {
        JPanel shell = new JPanel(new BorderLayout());

        // Left Sidebar (Chapter 6: lists only the functions of the logged-in role)
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(210, 900));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY),
                BorderFactory.createEmptyBorder(15, 12, 15, 12)
        ));

        JLabel brand = new JLabel("FastShip");
        brand.setFont(UiTheme.FONT_TITLE);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 4, 15, 0));
        sidebar.add(brand, BorderLayout.NORTH);

        sidebarMenuPanel = new JPanel();
        sidebarMenuPanel.setLayout(new BoxLayout(sidebarMenuPanel, BoxLayout.Y_AXIS));
        sidebar.add(sidebarMenuPanel, BorderLayout.CENTER);

        shell.add(sidebar, BorderLayout.WEST);

        // Right Area: Top Bar + Content Area
        JPanel rightArea = new JPanel(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setPreferredSize(new Dimension(1200, 48));
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));

        breadcrumbLabel = new JLabel("Orders");
        breadcrumbLabel.setFont(UiTheme.FONT_SUBTITLE);
        topBar.add(breadcrumbLabel, BorderLayout.WEST);

        JPanel userBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        roleBadgeLabel = new JLabel("[Customer]");
        roleBadgeLabel.setFont(UiTheme.FONT_LABEL);
        userFullNameLabel = new JLabel("Nguyen Van An");
        userFullNameLabel.setFont(UiTheme.FONT_LABEL);

        JButton logoutBtn = UiTheme.createOutlineButton("Logout");
        logoutBtn.addActionListener(e -> {
            loggedInUser = null;
            rootCardLayout.show(rootPanel, "LOGIN");
        });

        userBar.add(roleBadgeLabel);
        userBar.add(userFullNameLabel);
        userBar.add(logoutBtn);
        topBar.add(userBar, BorderLayout.EAST);

        rightArea.add(topBar, BorderLayout.NORTH);

        contentAreaPanel = new JPanel(new BorderLayout());
        rightArea.add(contentAreaPanel, BorderLayout.CENTER);

        shell.add(rightArea, BorderLayout.CENTER);
        return shell;
    }

    private void performLogin(String username, String password) {
        try {
            loggedInUser = authService.login(username, password);
            roleBadgeLabel.setText("[" + loggedInUser.getRole().name() + "]");
            userFullNameLabel.setText(loggedInUser.getFullName());
            rebuildRoleMenuAndDefaultScreen();
            rootCardLayout.show(rootPanel, "SHELL");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rebuildRoleMenuAndDefaultScreen() {
        sidebarMenuPanel.removeAll();

        Role role = loggedInUser.getRole();
        JLabel sectionTitle = new JLabel(role.name() + " MENU");
        sectionTitle.setFont(UiTheme.FONT_LABEL);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarMenuPanel.add(sectionTitle);
        sidebarMenuPanel.add(Box.createVerticalStrut(8));

        JButton firstBtn = null;
        if (role == Role.CUSTOMER) {
            firstBtn = addMenuButton("Create order", () -> showContent("Orders / Create order",
                    new CreateOrderPanel(loggedInUser, () -> showContent("Orders / My orders", new OrderListAndDetailPanel(loggedInUser)))));
            addMenuButton("My orders", () -> showContent("Orders / My orders", new OrderListAndDetailPanel(loggedInUser)));
            addMenuButton("Track order", () -> showContent("Track order", new OrderTrackingPanel(loggedInUser)));
        } else if (role == Role.STAFF) {
            firstBtn = addMenuButton("Orders", () -> showContent("Orders", new OrderListAndDetailPanel(loggedInUser)));
            addMenuButton("Deliveries and COD", () -> showContent("Deliveries and COD", new DeliveryAndCodPanel(loggedInUser)));
            addMenuButton("Track order", () -> showContent("Track order", new OrderTrackingPanel(loggedInUser)));
        } else if (role == Role.DISPATCHER) {
            firstBtn = addMenuButton("Order assignment", () -> showContent("Order assignment", new OrderAssignmentPanel(loggedInUser)));
            addMenuButton("Track order", () -> showContent("Track order", new OrderTrackingPanel(loggedInUser)));
        } else if (role == Role.DRIVER) {
            firstBtn = addMenuButton("My assignments", () -> showContent("My assignments", new DriverAssignmentPanel(loggedInUser)));
        } else if (role == Role.MANAGER) {
            firstBtn = addMenuButton("Drivers and vehicles", () -> showContent("Drivers and vehicles", new DriverManagementPanel()));
            addMenuButton("Reports", () -> showContent("Reports", new ReportDashboardPanel()));
        }

        sidebarMenuPanel.revalidate();
        sidebarMenuPanel.repaint();

        if (firstBtn != null) {
            firstBtn.doClick();
        }
    }

    private JButton addMenuButton(String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.setFont(UiTheme.FONT_BUTTON);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> action.run());
        sidebarMenuPanel.add(btn);
        sidebarMenuPanel.add(Box.createVerticalStrut(4));
        return btn;
    }

    private void showContent(String breadcrumb, JComponent panel) {
        breadcrumbLabel.setText(breadcrumb);
        contentAreaPanel.removeAll();
        contentAreaPanel.add(panel, BorderLayout.CENTER);
        contentAreaPanel.revalidate();
        contentAreaPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FastShipApp app = new FastShipApp();
            app.setVisible(true);
        });
    }
}
