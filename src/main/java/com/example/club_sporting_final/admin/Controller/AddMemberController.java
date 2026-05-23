package com.example.club_sporting_final.admin.Controller;

import com.example.club_sporting_final.admin.module.Team;
import com.example.club_sporting_final.utils.DatabaseConnection;
import com.example.club_sporting_final.utils.EmailSender;
import jakarta.mail.MessagingException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AddMemberController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private CheckBox subscriptionStatus;

    @FXML
    private ChoiceBox<Team> teamChoiceBox;

    @FXML
    private RadioButton monthlyPlan;

    @FXML
    private RadioButton quarterlyPlan;

    @FXML
    private RadioButton yearlyPlan;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private ToggleGroup planToggleGroup;

    private ObservableList<Team> teamList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize toggle group for subscription plans
        planToggleGroup = new ToggleGroup();
        monthlyPlan.setToggleGroup(planToggleGroup);
        quarterlyPlan.setToggleGroup(planToggleGroup);
        yearlyPlan.setToggleGroup(planToggleGroup);

        // Load teams from the database into the choice box
        loadTeams();
    }

    private void loadTeams() {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT TeamID, TeamName, TeamLeaderID, CoachName, Category FROM teams")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                teamList.add(new Team(
                        rs.getInt("TeamID"),
                        rs.getString("TeamName"),
                        rs.getString("CoachName"),
                        rs.getString("Category"),
                        0, // Assuming memberCount is not fetched here; update this if needed
                        rs.getObject("TeamLeaderID") != null ? rs.getInt("TeamLeaderID") : null // Handle nullable TeamLeaderID
                ));
            }
            teamChoiceBox.setItems(teamList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load teams: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        Team selectedTeam = teamChoiceBox.getValue();
        boolean isSubscribed = subscriptionStatus.isSelected();
        String planType = getSelectedPlanType();

        if (!validateInputs(name, email, phone, selectedTeam, planType)) return;

        String insertMemberQuery = "INSERT INTO members (Name, Email, PhoneNumber, SubscriptionStatus, TeamID) VALUES (?, ?, ?, ?, ?)";
        String associateTeamQuery = "INSERT INTO team_members (MemberID, TeamID) VALUES (?, ?)";
        String insertSubscriptionQuery = "INSERT INTO subscriptions (MemberID, PlanType, StartDate, EndDate, Amount) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);

            int memberID;
            // Insert member into the database
            try (PreparedStatement memberStmt = connection.prepareStatement(insertMemberQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                memberStmt.setString(1, name);
                memberStmt.setString(2, email);
                memberStmt.setString(3, phone);
                memberStmt.setBoolean(4, isSubscribed);
                memberStmt.setObject(5, selectedTeam != null ? selectedTeam.getTeamID() : null);
                memberStmt.executeUpdate();

                ResultSet rs = memberStmt.getGeneratedKeys();
                if (rs.next()) {
                    memberID = rs.getInt(1);
                } else {
                    throw new SQLException("Failed to retrieve Member ID.");
                }
            }

            // Associate member with a team
            if (selectedTeam != null) {
                try (PreparedStatement teamStmt = connection.prepareStatement(associateTeamQuery)) {
                    teamStmt.setInt(1, memberID);
                    teamStmt.setInt(2, selectedTeam.getTeamID());
                    teamStmt.executeUpdate();
                }
            }

            // Add subscription details if subscribed
            if (isSubscribed) {
                String startDate = java.time.LocalDate.now().toString();
                String endDate = calculateEndDate(startDate, planType);
                double amount = calculateSubscriptionAmount(planType);

                try (PreparedStatement subStmt = connection.prepareStatement(insertSubscriptionQuery)) {
                    subStmt.setInt(1, memberID);
                    subStmt.setString(2, planType);
                    subStmt.setString(3, startDate);
                    subStmt.setString(4, endDate);
                    subStmt.setDouble(5, amount);
                    subStmt.executeUpdate();
                }

                // Send subscription email
                sendSubscriptionEmail(name, email, planType, startDate, endDate);
            }

            connection.commit();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Member added successfully!");
            closeWindow();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private boolean validateInputs(String name, String email, String phone, Team team, String planType) {
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Name, email, and phone number are required.");
            return false;
        }
        if (!email.matches("\\S+@\\S+\\.\\S+")) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid email format.");
            return false;
        }
        if (!phone.matches("\\d{10,11}")) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Phone number must be 10 or 11 digits.");
            return false;
        }
        if (subscriptionStatus.isSelected() && planType == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please select a subscription plan type.");
            return false;
        }
        return true;
    }

    private String getSelectedPlanType() {
        RadioButton selectedPlan = (RadioButton) planToggleGroup.getSelectedToggle();
        if (selectedPlan == null) return null;
        String text = selectedPlan.getText().toLowerCase();
        if (text.contains("monthly")) return "monthly";
        if (text.contains("quarterly")) return "quarterly";
        if (text.contains("yearly")) return "yearly";
        return text;
    }

    private String calculateEndDate(String startDate, String planType) {
        java.time.LocalDate start = java.time.LocalDate.parse(startDate);
        return switch (planType) {
            case "monthly" -> start.plusMonths(1).toString();
            case "quarterly" -> start.plusMonths(3).toString();
            case "yearly" -> start.plusYears(1).toString();
            default -> start.toString();
        };
    }

    private double calculateSubscriptionAmount(String planType) {
        return switch (planType) {
            case "monthly" -> 50.0;
            case "quarterly" -> 150.0;
            case "yearly" -> 500.0;
            default -> 0.0;
        };
    }

    private void sendSubscriptionEmail(String name, String email, String planType, String startDate, String endDate) {
        if (email == null || email.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Email address is missing.");
            return;
        }
        if (!email.matches("\\S+@\\S+\\.\\S+")) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid email format: " + email);
            return;
        }

        String subject = "Welcome to Sport Hub!";
        String messageBody = String.format(
                "Dear %s,\n\nThank you for subscribing to our %s plan.\nYour subscription starts on %s and ends on %s.\n\nBest regards,\nSport Hub Team",
                name, planType, startDate, endDate
        );

        try {
            EmailSender.sendEmail(email, subject, messageBody);
        } catch (Exception e) {
            System.err.println("Warning: Failed to send welcome email: " + e.getMessage());
        }
    }



    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }
}