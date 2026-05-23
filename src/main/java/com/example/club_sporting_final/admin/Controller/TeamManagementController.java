package com.example.club_sporting_final.admin.Controller;

import com.example.club_sporting_final.utils.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.example.club_sporting_final.admin.module.Team;

import java.io.IOException;
import java.sql.*;

public class TeamManagementController {

    @FXML
    private TableView<Team> teamTable;

    @FXML
    private TableColumn<Team, Integer> idColumn;

    @FXML
    private TableColumn<Team, String> nameColumn;

    @FXML
    private TableColumn<Team, String> coachColumn;

    @FXML
    private TableColumn<Team, String> categoryColumn;

    @FXML
    private TableColumn<Team, Integer> memberCountColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Button addButton, editButton, deleteButton, searchButton, backButton;

    private ObservableList<Team> teamList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> data.getValue().teamIDProperty().asObject());
        nameColumn.setCellValueFactory(data -> data.getValue().teamNameProperty());
        coachColumn.setCellValueFactory(data -> data.getValue().coachNameProperty());
        categoryColumn.setCellValueFactory(data -> data.getValue().categoryProperty());
        memberCountColumn.setCellValueFactory(data -> data.getValue().memberCountProperty().asObject());

        loadTeams();
    }

    private void loadTeams() {
        teamList.clear();
        String query = """
            SELECT t.TeamID, t.TeamName, t.CoachName, t.Category, COUNT(m.MemberID) AS MemberCount
            FROM Teams t
            LEFT JOIN Members m ON t.TeamID = m.TeamID
            GROUP BY t.TeamID, t.TeamName, t.CoachName, t.Category
            """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                teamList.add(new Team(
                        rs.getInt("TeamID"),
                        rs.getString("TeamName"),
                        rs.getString("CoachName"),
                        rs.getString("Category"),
                        rs.getInt("MemberCount"),
                        null
                ));
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load teams: " + e.getMessage());
        }
        teamTable.setItems(teamList);
    }

    @FXML
    private void addTeam() {
        openForm("/com/example/club_sporting_final/admin/AddTeamPage.fxml", "Add Team");
    }

    @FXML
    private void editTeam() {
        Team selectedTeam = teamTable.getSelectionModel().getSelectedItem();
        if (selectedTeam == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a team to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/club_sporting_final/admin/EditTeamPage.fxml"));
            Parent root = loader.load();

            EditTeamController controller = loader.getController();
            controller.setTeam(selectedTeam);

            Stage stage = new Stage();
            stage.setTitle("Edit Team");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadTeams(); // Reload teams after editing
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not open Edit Team page: " + e.getMessage());
        }
    }

    @FXML
    private void deleteTeam() {
        Team selectedTeam = teamTable.getSelectionModel().getSelectedItem();
        if (selectedTeam == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a team to delete.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Delete");
        confirmationAlert.setHeaderText("Are you sure you want to delete this team?");
        confirmationAlert.setContentText("Team ID: " + selectedTeam.getTeamID() + "\nTeam Name: " + selectedTeam.getTeamName());

        if (confirmationAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection connection = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement stmt = connection.prepareStatement("DELETE FROM Teams WHERE TeamID = ?")) {

                stmt.setInt(1, selectedTeam.getTeamID());
                stmt.executeUpdate();

                teamList.remove(selectedTeam);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Team deleted successfully.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Could not delete team: " + e.getMessage());
            }
        }
    }

    @FXML
    private void searchTeam() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            teamTable.setItems(teamList);
            return;
        }

        ObservableList<Team> filteredList = FXCollections.observableArrayList();
        for (Team team : teamList) {
            if (team.getTeamName().toLowerCase().contains(searchText) ||
                    team.getCoachName().toLowerCase().contains(searchText) ||
                    team.getCategory().toLowerCase().contains(searchText)) {
                filteredList.add(team);
            }
        }
        teamTable.setItems(filteredList);
    }

    @FXML
    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/club_sporting_final/admin/DashBoard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }

    private void openForm(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadTeams();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not open the requested form.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
