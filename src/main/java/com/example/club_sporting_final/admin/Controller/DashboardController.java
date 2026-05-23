package com.example.club_sporting_final.admin.Controller;

import com.example.club_sporting_final.utils.DatabaseConnection;
import com.example.club_sporting_final.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardController {

    // Constants for FXML Paths
    private static final String MEMBERS_MANAGEMENT_PATH = "/com/example/club_sporting_final/admin/MembersManagement.fxml";
    private static final String TEAM_MANAGEMENT_PATH = "/com/example/club_sporting_final/admin/TeamManagementPage.fxml";
    private static final String EXPENSE_MANAGEMENT_PATH = "/com/example/club_sporting_final/admin/ExpenseManagement.fxml";
    private static final String SUBSCRIPTIONS_MANAGEMENT_PATH = "/com/example/club_sporting_final/admin/SubscriptionsManagement.fxml";
    private static final String REPORTS_PATH = "/com/example/club_sporting_final/admin/Reports.fxml";
    private static final String LOGIN_PATH = "/com/example/club_sporting_final/login/Login.fxml";
    private static final String ATTENDANCE_PATH = "/com/example/club_sporting_final/admin/Attendance.fxml";

    // FXML Chart Components
    @FXML private PieChart membersChart;
    @FXML private BarChart<String, Number> incomeChart;
    @FXML private CategoryAxis incomeXAxis;
    @FXML private NumberAxis incomeYAxis;

    // KPI Card Labels
    @FXML private Label kpiActiveMembersLabel;
    @FXML private Label kpiMonthlyRevenueLabel;
    @FXML private Label kpiMonthlyExpensesLabel;
    @FXML private Label kpiExpiringSoonLabel;

    // Expiry warning banner
    @FXML private HBox expiryAlertBanner;
    @FXML private Label expiryAlertLabel;

    // Navigation Buttons (for RBAC)
    @FXML private Button btnTeamManagement;
    @FXML private Button btnExpenseManagement;
    @FXML private Button btnSubscriptionsManagement;
    @FXML private Button btnAttendance;

    @FXML private VBox contentArea;

    // Navigation methods
    @FXML
    private void goToMemberManagement(ActionEvent event) {
        navigateToPage(event, MEMBERS_MANAGEMENT_PATH, "Member Management");
    }

    @FXML
    private void goToTeamManagement(ActionEvent event) {
        navigateToPage(event, TEAM_MANAGEMENT_PATH, "Team Management");
    }

    @FXML
    private void goToExpenseManagement(ActionEvent event) {
        navigateToPage(event, EXPENSE_MANAGEMENT_PATH, "Expense Management");
    }

    @FXML
    private void goToSubscriptionsManagement(ActionEvent event) {
        navigateToPage(event, SUBSCRIPTIONS_MANAGEMENT_PATH, "Subscriptions Management");
    }

    @FXML
    private void goToReports(ActionEvent event) {
        navigateToPage(event, REPORTS_PATH, "Reports");
    }

    @FXML
    private void goToAttendance(ActionEvent event) {
        navigateToPage(event, ATTENDANCE_PATH, "Attendance Tracking");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        navigateToPage(event, LOGIN_PATH, "Login");
    }

    private void navigateToPage(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene newScene = new Scene(loader.load());
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(newScene);
            currentStage.setTitle("Sport Hub — " + title);
        } catch (IOException e) {
            showErrorAlert("Navigation Error", "Failed to load: " + title + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // Initialize everything
    @FXML
    private void initialize() {
        applyRoleBasedAccess();
        loadKpiCards();
        loadMembersStatistics();
        loadIncomeStatistics();
        checkExpiringSubscriptions();
    }

    /**
     * Feature 2: Role-Based Access Control.
     * Hides admin-only buttons when the logged-in user is an employee.
     */
    private void applyRoleBasedAccess() {
        SessionManager session = SessionManager.getInstance();

        if (!session.isAdmin()) {
            // Employees can only view Members and Reports
            if (btnTeamManagement != null) btnTeamManagement.setVisible(false);
            if (btnExpenseManagement != null) btnExpenseManagement.setVisible(false);
            if (btnSubscriptionsManagement != null) btnSubscriptionsManagement.setVisible(false);
        }
    }

    /**
     * Feature 1: Load KPI Summary Cards.
     */
    private void loadKpiCards() {
        if (kpiActiveMembersLabel != null) {
            kpiActiveMembersLabel.setText(String.valueOf(queryScalar(
                "SELECT COUNT(*) FROM members WHERE SubscriptionStatus = 1")));
        }
        if (kpiMonthlyRevenueLabel != null) {
            double rev = queryDouble(
                "SELECT COALESCE(SUM(Amount), 0) FROM subscriptions " +
                "WHERE strftime('%m', StartDate) = strftime('%m', 'now') " +
                "AND strftime('%Y', StartDate) = strftime('%Y', 'now')");
            kpiMonthlyRevenueLabel.setText(String.format("$%.0f", rev));
        }
        if (kpiMonthlyExpensesLabel != null) {
            double exp = queryDouble(
                "SELECT COALESCE(SUM(Amount), 0) FROM expenses " +
                "WHERE strftime('%m', Date) = strftime('%m', 'now') " +
                "AND strftime('%Y', Date) = strftime('%Y', 'now')");
            kpiMonthlyExpensesLabel.setText(String.format("$%.0f", exp));
        }
        if (kpiExpiringSoonLabel != null) {
            int expiring = queryScalar(
                "SELECT COUNT(*) FROM subscriptions " +
                "WHERE EndDate BETWEEN date('now') AND date('now', '+7 days')");
            kpiExpiringSoonLabel.setText(String.valueOf(expiring));
        }
    }

    /**
     * Feature 3: Check for subscriptions expiring in the next 7 days and show an alert banner.
     */
    private void checkExpiringSubscriptions() {
        if (expiryAlertBanner == null || expiryAlertLabel == null) return;

        String query = "SELECT COUNT(*) FROM subscriptions " +
                       "WHERE EndDate BETWEEN date('now') AND date('now', '+7 days')";
        int count = queryScalar(query);

        if (count > 0) {
            expiryAlertLabel.setText("⚠️  " + count + " subscription(s) expiring within 7 days! Go to Subscriptions Management to review.");
            expiryAlertBanner.setVisible(true);
            expiryAlertBanner.setManaged(true);
        } else {
            expiryAlertBanner.setVisible(false);
            expiryAlertBanner.setManaged(false);
        }
    }

    private void loadMembersStatistics() {
        if (membersChart == null) return;
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        String query = "SELECT subscriptionStatus, COUNT(*) AS count " +
                "FROM members " +
                "GROUP BY subscriptionStatus";

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String status = rs.getInt("subscriptionStatus") == 1 ? "Active" : "Inactive";
                int count = rs.getInt("count");
                data.add(new PieChart.Data(status + " (" + count + ")", count));
            }

            membersChart.setData(data);
            membersChart.setTitle("Membership Distribution");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadIncomeStatistics() {
        if (incomeChart == null) return;
        ObservableList<BarChart.Data<String, Number>> incomeData = FXCollections.observableArrayList();

        String query = "SELECT strftime('%W', startDate) AS week, SUM(amount) AS totalIncome " +
                "FROM subscriptions " +
                "WHERE strftime('%m', startDate) = strftime('%m', 'now') AND strftime('%Y', startDate) = strftime('%Y', 'now') " +
                "GROUP BY strftime('%W', startDate)";

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
            series.setName("Weekly Income");

            while (rs.next()) {
                String week = "Week " + rs.getInt("week");
                double totalIncome = rs.getDouble("totalIncome");
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(week, totalIncome));
            }

            incomeChart.getData().clear();
            incomeChart.getXAxis().setLabel("Weeks");
            incomeChart.getYAxis().setLabel("Income ($)");
            incomeChart.getData().add(series);
            incomeChart.setTitle("Income This Month");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- DB Helpers ---
    private int queryScalar(String sql) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double queryDouble(String sql) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
