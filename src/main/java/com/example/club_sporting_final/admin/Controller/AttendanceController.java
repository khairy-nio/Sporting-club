package com.example.club_sporting_final.admin.Controller;

import com.example.club_sporting_final.admin.module.Attendance;
import com.example.club_sporting_final.utils.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class AttendanceController {

    @FXML private TextField memberIdField;
    @FXML private ChoiceBox<String> sessionTypeBox;
    @FXML private DatePicker datePicker;
    @FXML private ChoiceBox<String> statusBox;
    @FXML private TextField searchField;
    @FXML private TableView<Attendance> attendanceTable;
    @FXML private TableColumn<Attendance, Integer> colID;
    @FXML private TableColumn<Attendance, Integer> colMemberID;
    @FXML private TableColumn<Attendance, String> colMemberName;
    @FXML private TableColumn<Attendance, String> colDate;
    @FXML private TableColumn<Attendance, String> colSession;
    @FXML private TableColumn<Attendance, String> colStatus;

    private final ObservableList<Attendance> attendanceList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        sessionTypeBox.setItems(FXCollections.observableArrayList(
                "Football Training", "Basketball Training", "Tennis Training",
                "Swimming Session", "Gym Session", "General Meeting"));
        sessionTypeBox.setValue("Football Training");

        statusBox.setItems(FXCollections.observableArrayList("Present", "Absent", "Late"));
        statusBox.setValue("Present");

        datePicker.setValue(LocalDate.now());

        colID.setCellValueFactory(d -> d.getValue().attendanceIDProperty().asObject());
        colMemberID.setCellValueFactory(d -> d.getValue().memberIDProperty().asObject());
        colMemberName.setCellValueFactory(d -> d.getValue().memberNameProperty());
        colDate.setCellValueFactory(d -> d.getValue().dateProperty());
        colSession.setCellValueFactory(d -> d.getValue().sessionTypeProperty());
        colStatus.setCellValueFactory(d -> d.getValue().statusProperty());

        // Color-code rows: red = Absent, yellow = Late
        attendanceTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Attendance item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if ("Absent".equalsIgnoreCase(item.getStatus())) {
                    setStyle("-fx-background-color: rgba(239,83,80,0.25);");
                } else if ("Late".equalsIgnoreCase(item.getStatus())) {
                    setStyle("-fx-background-color: rgba(255,152,0,0.2);");
                } else {
                    setStyle("");
                }
            }
        });

        loadAllAttendance();
    }

    @FXML
    private void handleMarkAttendance() {
        String memberIdText = memberIdField.getText().trim();
        String sessionType = sessionTypeBox.getValue();
        LocalDate date = datePicker.getValue();
        String status = statusBox.getValue();

        if (memberIdText.isEmpty() || date == null || sessionType == null || status == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields", "Please fill in all fields before marking attendance.");
            return;
        }

        int memberId;
        try {
            memberId = Integer.parseInt(memberIdText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Member ID must be a number.");
            return;
        }

        // Check member exists
        if (!memberExists(memberId)) {
            showAlert(Alert.AlertType.ERROR, "Not Found", "No member found with ID: " + memberId);
            return;
        }

        String query = "INSERT INTO attendance (MemberID, Date, SessionType, Status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, memberId);
            stmt.setString(2, date.toString());
            stmt.setString(3, sessionType);
            stmt.setString(4, status);
            stmt.executeUpdate();
            loadAllAttendance();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Attendance marked: " + status + " for Member #" + memberId);
            memberIdField.clear();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save attendance: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadAllAttendance();
            return;
        }
        attendanceList.clear();
        String query = "SELECT a.AttendanceID, a.MemberID, m.Name, a.Date, a.SessionType, a.Status " +
                       "FROM attendance a JOIN members m ON a.MemberID = m.MemberID " +
                       "WHERE CAST(a.MemberID AS TEXT) LIKE ? OR m.Name LIKE ? " +
                       "ORDER BY a.Date DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, "%" + searchText + "%");
            stmt.setString(2, "%" + searchText + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                attendanceList.add(new Attendance(
                        rs.getInt("AttendanceID"), rs.getInt("MemberID"),
                        rs.getString("Name"), rs.getString("Date"),
                        rs.getString("SessionType"), rs.getString("Status")));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Search failed: " + e.getMessage());
        }
        attendanceTable.setItems(attendanceList);
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        loadAllAttendance();
    }

    @FXML
    private void handleDelete() {
        Attendance selected = attendanceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a record to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Attendance Record?");
        confirm.setContentText("ID: " + selected.getAttendanceID());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String query = "DELETE FROM attendance WHERE AttendanceID = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, selected.getAttendanceID());
                stmt.executeUpdate();
                attendanceList.remove(selected);
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Could not delete: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/club_sporting_final/admin/DashBoard.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Dashboard");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }

    private void loadAllAttendance() {
        attendanceList.clear();
        String query = "SELECT a.AttendanceID, a.MemberID, m.Name, a.Date, a.SessionType, a.Status " +
                       "FROM attendance a JOIN members m ON a.MemberID = m.MemberID " +
                       "ORDER BY a.Date DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                attendanceList.add(new Attendance(
                        rs.getInt("AttendanceID"), rs.getInt("MemberID"),
                        rs.getString("Name"), rs.getString("Date"),
                        rs.getString("SessionType"), rs.getString("Status")));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load attendance: " + e.getMessage());
        }
        attendanceTable.setItems(attendanceList);
    }

    private boolean memberExists(int memberId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM members WHERE MemberID = ?")) {
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
