package com.example.club_sporting_final.admin.Controller;

import com.example.club_sporting_final.admin.module.Subscription;
import com.example.club_sporting_final.utils.DatabaseConnection;
import com.example.club_sporting_final.utils.EmailSender;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class SubscriptionsManagementController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Subscription> subscriptionTable;

    @FXML
    private TableColumn<Subscription, Integer> idColumn;

    @FXML
    private TableColumn<Subscription, Integer> memberColumn;

    @FXML
    private TableColumn<Subscription, String> planColumn;

    @FXML
    private TableColumn<Subscription, String> startDateColumn;

    @FXML
    private TableColumn<Subscription, String> endDateColumn;

    @FXML
    private TableColumn<Subscription, Double> amountColumn;

    @FXML
    private Button inactiveButton;

    private final ObservableList<Subscription> subscriptions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Bind columns to Subscription model properties
        idColumn.setCellValueFactory(data -> data.getValue().subscriptionIDProperty().asObject());
        memberColumn.setCellValueFactory(data -> data.getValue().memberIDProperty().asObject());
        planColumn.setCellValueFactory(data -> data.getValue().planTypeProperty());
        startDateColumn.setCellValueFactory(data -> data.getValue().startDateProperty());
        endDateColumn.setCellValueFactory(data -> data.getValue().endDateProperty());
        amountColumn.setCellValueFactory(data -> data.getValue().amountProperty().asObject());

        // Apply a custom row factory to change row color based on condition
        subscriptionTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Subscription subscription, boolean empty) {
                super.updateItem(subscription, empty);

                if (subscription == null || empty) {
                    setStyle(""); // Reset style for empty rows
                } else {
                    LocalDate endDate = LocalDate.parse(subscription.getEndDate());
                    if (endDate.isBefore(LocalDate.now())) {
                        setStyle("-fx-background-color: #ffcccc;"); // Light red background
                    } else {
                        setStyle(""); // Default style for other rows
                    }
                }
            }
        });

        // Load data into the TableView
        loadSubscriptions();
    }
    @FXML
    private void returnToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/club_sporting_final/admin/DashBoard.fxml"));
            Stage stage = (Stage) searchField.getScene().getWindow(); // Use a component to get the current stage
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not return to Dashboard.");
        }
    }


    private void loadSubscriptions() {
        subscriptions.clear();
        String query = "SELECT * FROM Subscriptions";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                subscriptions.add(new Subscription(
                        rs.getInt("SubscriptionID"),  // Ensure this matches your database column name
                        rs.getInt("MemberID"),       // Ensure this matches your database column name
                        rs.getString("PlanType"),    // Ensure this matches your database column name
                        rs.getString("StartDate"),   // Ensure this matches your database column name
                        rs.getString("EndDate"),     // Ensure this matches your database column name
                        rs.getDouble("Amount")       // Ensure this matches your database column name
                ));
            }


        } catch (SQLException e) {
            showError("Database Error", "Could not load subscriptions: " + e.getMessage());
        }
        subscriptionTable.setItems(subscriptions);
    }


    @FXML
    private void deleteSubscription() {
        Subscription selectedSubscription = subscriptionTable.getSelectionModel().getSelectedItem();

        if (selectedSubscription == null) {
            showError("No Selection", "Please select a subscription to delete.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Delete");
        confirmationAlert.setHeaderText("Are you sure you want to delete this subscription?");
        confirmationAlert.setContentText("Subscription ID: " + selectedSubscription.getSubscriptionID());

        if (confirmationAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String query = "DELETE FROM Subscriptions WHERE SubscriptionID = ?";
            try (Connection connection = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setInt(1, selectedSubscription.getSubscriptionID());
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    subscriptions.remove(selectedSubscription);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Subscription deleted successfully.");
                } else {
                    showError("Error", "Failed to delete the subscription.");
                }
            } catch (SQLException e) {
                showError("Database Error", "An error occurred while deleting the subscription: " + e.getMessage());
            }
        }
    }

    @FXML
    private void searchSubscriptions() {
        String searchQuery = searchField.getText().trim();
        subscriptions.clear();
        String query = "SELECT * FROM Subscriptions WHERE PlanType LIKE ? OR MemberID LIKE ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, "%" + searchQuery + "%");
            stmt.setString(2, "%" + searchQuery + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                subscriptions.add(new Subscription(
                        rs.getInt("SubscriptionID"),
                        rs.getInt("MemberID"),
                        rs.getString("PlanType"),
                        rs.getString("StartDate"),
                        rs.getString("EndDate"),
                        rs.getDouble("Amount")
                ));
            }

        } catch (SQLException e) {
            showError("Database Error", "Could not search subscriptions: " + e.getMessage());
        }
        subscriptionTable.setItems(subscriptions);
    }

    @FXML
    private void addSubscription() {
        showSubscriptionDialog(null); // Pass null for a new subscription
    }

    @FXML
    private void editSubscription() {
        Subscription selected = subscriptionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a subscription to edit.");
            return;
        }
        showSubscriptionDialog(selected); // Pass the selected subscription for editing
    }

    private void showSubscriptionDialog(Subscription subscription) {
        Dialog<Subscription> dialog = new Dialog<>();
        dialog.setTitle(subscription == null ? "Add Subscription" : "Edit Subscription");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField memberField = new TextField();
        memberField.setPromptText("Member ID");

        ToggleGroup planGroup = new ToggleGroup();
        RadioButton monthlyButton = new RadioButton("Monthly");
        RadioButton quarterlyButton = new RadioButton("Quarterly");
        RadioButton yearlyButton = new RadioButton("Yearly");
        monthlyButton.setToggleGroup(planGroup);
        quarterlyButton.setToggleGroup(planGroup);
        yearlyButton.setToggleGroup(planGroup);

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setEditable(false); // Prevent user editing
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setEditable(false); // Prevent user editing

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setEditable(false); // Auto-calculated, not editable

        if (subscription != null) {
            memberField.setText(String.valueOf(subscription.getMemberID()));
            String plan = subscription.getPlanType();
            if (plan.equalsIgnoreCase("monthly")) monthlyButton.setSelected(true);
            if (plan.equalsIgnoreCase("quarterly")) quarterlyButton.setSelected(true);
            if (plan.equalsIgnoreCase("yearly")) yearlyButton.setSelected(true);

            startDatePicker.setValue(LocalDate.parse(subscription.getStartDate()));
            endDatePicker.setValue(LocalDate.parse(subscription.getEndDate()));
            amountField.setText(String.valueOf(subscription.getAmount()));
        }

        planGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                LocalDate today = LocalDate.now();
                if (newValue == monthlyButton) {
                    startDatePicker.setValue(today);
                    endDatePicker.setValue(today.plusMonths(1));
                    amountField.setText("50.00");
                } else if (newValue == quarterlyButton) {
                    startDatePicker.setValue(today);
                    endDatePicker.setValue(today.plusMonths(3));
                    amountField.setText("150.00");
                } else if (newValue == yearlyButton) {
                    startDatePicker.setValue(today);
                    endDatePicker.setValue(today.plusYears(1));
                    amountField.setText("500.00");
                }
            }
        });

        grid.add(new Label("Member ID:"), 0, 0);
        grid.add(memberField, 1, 0);
        grid.add(new Label("Subscription Plan:"), 0, 1);
        grid.add(new HBox(10, monthlyButton, quarterlyButton, yearlyButton), 1, 1);
        grid.add(new Label("Start Date:"), 0, 2);
        grid.add(startDatePicker, 1, 2);
        grid.add(new Label("End Date:"), 0, 3);
        grid.add(endDatePicker, 1, 3);
        grid.add(new Label("Amount:"), 0, 4);
        grid.add(amountField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                int memberID = Integer.parseInt(memberField.getText());
                String planType = ((RadioButton) planGroup.getSelectedToggle()).getText().toLowerCase();
                String startDate = startDatePicker.getValue().toString();
                String endDate = endDatePicker.getValue().toString();
                double amount = Double.parseDouble(amountField.getText());

                if (subscription == null) {
                    return new Subscription(0, memberID, planType, startDate, endDate, amount);
                } else {
                    subscription.setMemberID(memberID);
                    subscription.setPlanType(planType);
                    subscription.setStartDate(startDate);
                    subscription.setEndDate(endDate);
                    subscription.setAmount(amount);
                    return subscription;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (subscription == null) {
                saveNewSubscription(result);
            } else {
                updateExistingSubscription(result);
            }
        });
    }

    private void saveNewSubscription(Subscription subscription) {
        String query = "INSERT INTO Subscriptions (MemberID, PlanType, StartDate, EndDate, Amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, subscription.getMemberID());
            stmt.setString(2, subscription.getPlanType());
            stmt.setString(3, subscription.getStartDate());
            stmt.setString(4, subscription.getEndDate());
            stmt.setDouble(5, subscription.getAmount());

            stmt.executeUpdate();
            loadSubscriptions();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Subscription added successfully.");
        } catch (SQLException e) {
            showError("Database Error", "Could not add subscription: " + e.getMessage());
        }
    }

    private void updateExistingSubscription(Subscription subscription) {
        String query = "UPDATE Subscriptions SET MemberID = ?, PlanType = ?, StartDate = ?, EndDate = ?, Amount = ? WHERE SubscriptionID = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, subscription.getMemberID());
            stmt.setString(2, subscription.getPlanType());
            stmt.setString(3, subscription.getStartDate());
            stmt.setString(4, subscription.getEndDate());
            stmt.setDouble(5, subscription.getAmount());
            stmt.setInt(6, subscription.getSubscriptionID());

            stmt.executeUpdate();
            loadSubscriptions();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Subscription updated successfully.");
        } catch (SQLException e) {
            showError("Database Error", "Could not update subscription: " + e.getMessage());
        }
    }

    @FXML
    private void showInactiveSubscriptions() {
        subscriptions.clear();
        String query = "SELECT * FROM Subscriptions WHERE EndDate < ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, LocalDate.now().toString());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                subscriptions.add(new Subscription(
                        rs.getInt("SubscriptionID"),
                        rs.getInt("MemberID"),
                        rs.getString("PlanType"),
                        rs.getString("StartDate"),
                        rs.getString("EndDate"),
                        rs.getDouble("Amount")
                ));
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not fetch inactive subscriptions: " + e.getMessage());
        }
        subscriptionTable.setItems(subscriptions);
    }

    /**
     * Feature 6: Send email reminders to members with subscriptions expiring within 7 days.
     */
    @FXML
    private void handleSendReminders() {
        String query = "SELECT s.SubscriptionID, s.EndDate, m.Name, m.Email " +
                       "FROM subscriptions s JOIN members m ON s.MemberID = m.MemberID " +
                       "WHERE s.EndDate BETWEEN date('now') AND date('now', '+7 days')";
        int sentCount = 0;
        int failCount = 0;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("Name");
                String email = rs.getString("Email");
                String endDate = rs.getString("EndDate");
                if (email != null && !email.isEmpty()) {
                    String subject = "Subscription Expiry Reminder — Sport Hub";
                    String body = "Dear " + name + ",\n\n" +
                                  "This is a friendly reminder that your subscription at Sport Hub " +
                                  "is expiring on " + endDate + ".\n\n" +
                                  "Please visit us or contact us to renew your membership.\n\n" +
                                  "Best regards,\nSport Hub Management";
                    boolean sent = EmailSender.sendEmail(email, subject, body);
                    if (sent) sentCount++; else failCount++;
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not fetch expiring subscriptions: " + e.getMessage());
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Reminders Sent",
                "Sent: " + sentCount + " email(s)\nFailed/Skipped: " + failCount);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}