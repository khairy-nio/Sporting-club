package com.example.club_sporting_final.admin.Controller;

import com.example.club_sporting_final.admin.module.Members;
import com.example.club_sporting_final.admin.module.Team;
import com.example.club_sporting_final.utils.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EditMemberController {

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
    private Label nameErrorLabel;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Label phoneErrorLabel;

    @FXML
    private Label teamErrorLabel;

    private Members member;
    private ObservableList<Team> teamList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadTeams();
    }

    /**
     * Sets the member to be edited.
     *
     * @param member the member object to edit
     */
    public void setMember(Members member) {
        this.member = member;

        // Populate fields with the selected member's data
        nameField.setText(member.getName());
        emailField.setText(member.getEmail());
        phoneField.setText(member.getPhoneNumber());
        subscriptionStatus.setSelected(member.isSubscriptionStatus());

        // Select the current team in the ChoiceBox
        if (member.getTeamID() != 0) { // Assuming 0 indicates no team
            teamChoiceBox.getSelectionModel().select(getTeamById(member.getTeamID()));
        }
    }

    /**
     * Loads teams into the ChoiceBox.
     */
    private void loadTeams() {
        teamList.clear();
        String query = "SELECT TeamID, TeamName FROM teams";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                teamList.add(new Team(
                        rs.getInt("TeamID"),
                        rs.getString("TeamName")
                ));
            }

            teamChoiceBox.setItems(teamList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load teams: " + e.getMessage());
        }
    }

    private Team getTeamById(int teamId) {
        for (Team team : teamList) {
            if (team.getTeamID() == teamId) {
                return team;
            }
        }
        return null;
    }

    /**
     * Handles the save action to update the member's details in the database.
     *
     * @param event the ActionEvent
     */
    @FXML
    void handleSave(ActionEvent event) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        boolean isSubscribed = subscriptionStatus.isSelected();
        Team selectedTeam = teamChoiceBox.getValue();

        if (!validateInputs(name, email, phone, selectedTeam)) return;

        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false); // Start transaction

            // Update member details
            String updateQuery = "UPDATE members SET Name = ?, Email = ?, PhoneNumber = ?, SubscriptionStatus = ?, TeamID = ? WHERE MemberID = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
                stmt.setString(1, name);
                stmt.setString(2, email);
                stmt.setString(3, phone);
                stmt.setBoolean(4, isSubscribed);
                stmt.setObject(5, selectedTeam != null ? selectedTeam.getTeamID() : null); // Set NULL if no team selected
                stmt.setInt(6, member.getMemberID());
                stmt.executeUpdate();
            }

            connection.commit(); // Commit transaction
            showAlert(Alert.AlertType.INFORMATION, "Success", "Member updated successfully.");
            closeWindow();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while updating the member: " + e.getMessage());
        }
    }

    private boolean validateInputs(String name, String email, String phone, Team team) {
        boolean valid = true;

        if (name.isEmpty()) {
            nameErrorLabel.setVisible(true);
            nameErrorLabel.setManaged(true);
            valid = false;
        } else {
            nameErrorLabel.setVisible(false);
            nameErrorLabel.setManaged(false);
        }

        if (!email.matches("^\\S+@\\S+\\.\\S+$")) {
            emailErrorLabel.setVisible(true);
            emailErrorLabel.setManaged(true);
            valid = false;
        } else {
            emailErrorLabel.setVisible(false);
            emailErrorLabel.setManaged(false);
        }

        if (!phone.matches("\\d{10,11}")) {
            phoneErrorLabel.setText("Must be 10 or 11 digits");
            phoneErrorLabel.setVisible(true);
            phoneErrorLabel.setManaged(true);
            valid = false;
        } else {
            phoneErrorLabel.setVisible(false);
            phoneErrorLabel.setManaged(false);
        }

        // Team selection is optional
        teamErrorLabel.setVisible(false);
        teamErrorLabel.setManaged(false);

        return valid;
    }

    /**
     * Handles the cancel action to close the edit member window.
     *
     * @param event the ActionEvent
     */
    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
