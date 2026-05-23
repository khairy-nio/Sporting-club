package com.example.club_sporting_final.admin.Controller;

import com.example.club_sporting_final.admin.module.Attendance;
import com.example.club_sporting_final.admin.module.Members;
import com.example.club_sporting_final.admin.module.Subscription;
import com.example.club_sporting_final.utils.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MemberProfileController {

    @FXML private Label profileIdLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profilePhoneLabel;
    @FXML private Label profileStatusLabel;
    @FXML private Label profileTeamLabel;

    @FXML private TableView<Subscription> subscriptionHistoryTable;
    @FXML private TableColumn<Subscription, String> subPlanCol;
    @FXML private TableColumn<Subscription, String> subStartCol;
    @FXML private TableColumn<Subscription, String> subEndCol;
    @FXML private TableColumn<Subscription, Double> subAmountCol;

    @FXML private TableView<Attendance> attendanceSummaryTable;
    @FXML private TableColumn<Attendance, String> attDateCol;
    @FXML private TableColumn<Attendance, String> attSessionCol;
    @FXML private TableColumn<Attendance, String> attStatusCol;

    @FXML
    public void initialize() {
        subPlanCol.setCellValueFactory(d -> d.getValue().planTypeProperty());
        subStartCol.setCellValueFactory(d -> d.getValue().startDateProperty());
        subEndCol.setCellValueFactory(d -> d.getValue().endDateProperty());
        subAmountCol.setCellValueFactory(d -> d.getValue().amountProperty().asObject());

        attDateCol.setCellValueFactory(d -> d.getValue().dateProperty());
        attSessionCol.setCellValueFactory(d -> d.getValue().sessionTypeProperty());
        attStatusCol.setCellValueFactory(d -> d.getValue().statusProperty());
    }

    /**
     * Called from MemberManagementController to inject the selected member.
     */
    public void loadMember(Members member) {
        profileIdLabel.setText(String.valueOf(member.getMemberID()));
        profileNameLabel.setText(member.getName());
        profileEmailLabel.setText(member.getEmail());
        profilePhoneLabel.setText(member.getPhoneNumber());

        boolean active = member.isSubscriptionStatus();
        profileStatusLabel.setText(active ? "✅ Active" : "❌ Inactive");
        profileStatusLabel.setStyle(active
                ? "-fx-text-fill: #66BB6A; -fx-font-size: 18px; -fx-font-weight: bold;"
                : "-fx-text-fill: #EF5350; -fx-font-size: 18px; -fx-font-weight: bold;");

        loadTeamName(member.getMemberID());
        loadSubscriptionHistory(member.getMemberID());
        loadAttendanceSummary(member.getMemberID());
    }

    private void loadTeamName(int memberId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT t.TeamName FROM Teams t JOIN members m ON t.TeamID = m.TeamID WHERE m.MemberID = ?")) {
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();
            profileTeamLabel.setText(rs.next() ? rs.getString("TeamName") : "No Team");
        } catch (SQLException e) {
            profileTeamLabel.setText("Unknown");
        }
    }

    private void loadSubscriptionHistory(int memberId) {
        ObservableList<Subscription> list = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM subscriptions WHERE MemberID = ? ORDER BY StartDate DESC")) {
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Subscription(
                        rs.getInt("SubscriptionID"), rs.getInt("MemberID"),
                        rs.getString("PlanType"), rs.getString("StartDate"),
                        rs.getString("EndDate"), rs.getDouble("Amount")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        subscriptionHistoryTable.setItems(list);
    }

    private void loadAttendanceSummary(int memberId) {
        ObservableList<Attendance> list = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT a.AttendanceID, a.MemberID, m.Name, a.Date, a.SessionType, a.Status " +
                     "FROM attendance a JOIN members m ON a.MemberID = m.MemberID " +
                     "WHERE a.MemberID = ? ORDER BY a.Date DESC LIMIT 20")) {
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Attendance(
                        rs.getInt("AttendanceID"), rs.getInt("MemberID"),
                        rs.getString("Name"), rs.getString("Date"),
                        rs.getString("SessionType"), rs.getString("Status")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        attendanceSummaryTable.setItems(list);
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) profileIdLabel.getScene().getWindow();
        stage.close();
    }
}
