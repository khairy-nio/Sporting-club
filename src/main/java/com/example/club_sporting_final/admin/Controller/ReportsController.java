package com.example.club_sporting_final.admin.Controller;

import com.example.club_sporting_final.admin.module.ReportItem;
import com.example.club_sporting_final.utils.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReportsController {

    @FXML
    private Label totalIncomeLabel;

    @FXML
    private Label totalExpensesLabel;
    @FXML
    private Button generateReportButton;


    @FXML
    private Label netBalanceLabel;

    @FXML
    private ChoiceBox<String> filterChoiceBox;

    @FXML
    private TableView<ReportItem> reportsTable;

    @FXML
    private TableColumn<ReportItem, String> columnName;

    @FXML
    private TableColumn<ReportItem, String> columnCategory;

    @FXML
    private TableColumn<ReportItem, Double> columnAmount;

    @FXML
    private TableColumn<ReportItem, String> columnDate;

    @FXML
    private TableColumn<ReportItem, String> columnDetails;

    private ObservableList<ReportItem> reportItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set up table columns
        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        columnAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        columnDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        columnDetails.setCellValueFactory(new PropertyValueFactory<>("details"));

        // Load filters into ChoiceBox
        filterChoiceBox.setItems(FXCollections.observableArrayList("All", "Subscriptions", "Expenses"));
        filterChoiceBox.setValue("All");

        // Load initial data
        loadReportsData();
        calculateSummary();
    }
    @FXML
    private void goBackToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/club_sporting_final/admin/DashBoard.fxml"));
            Scene scene = new Scene(loader.load());
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(scene);
            currentStage.setTitle("Dashboard");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadReportsData() {
        reportItems.clear();

        // Query to fetch subscriptions and expenses
        String query = "SELECT " +
                "    m.Name AS Name, " +
                "    'Income' AS Category, " +
                "    s.Amount, " +
                "    s.StartDate AS Date, " +
                "    s.PlanType AS Details " +
                "FROM subscriptions s " +
                "JOIN members m ON s.MemberID = m.MemberID " +
                "UNION ALL " +
                "SELECT " +
                "    e.ExpenseType AS Name, " +
                "    'Expense' AS Category, " +
                "    e.Amount, " +
                "    e.Date, " +
                "    'Expense Details' AS Details " +
                "FROM expenses e";

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();

            // Iterate through result set and add data to reportItems list
            while (rs.next()) {
                String name = rs.getString("Name");
                String category = rs.getString("Category");
                double amount = rs.getDouble("Amount");
                String date = rs.getString("Date");
                String details = rs.getString("Details");

                // Debugging: Print data to console to verify
                System.out.println("Name: " + name + ", Category: " + category + ", Amount: " + amount + ", Date: " + date + ", Details: " + details);

                // Add to reportItems list
                reportItems.add(new ReportItem(name, category, amount, date, details));
            }

            // Set the list to the TableView
            reportsTable.setItems(reportItems);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load reports data: " + e.getMessage());
        }
    }

    private void calculateSummary() {
        double totalIncome = 0;
        double totalExpenses = 0;

        // Iterate through reportItems and calculate totals
        for (ReportItem item : reportItems) {
            if ("Income".equalsIgnoreCase(item.getCategory())) {
                totalIncome += item.getAmount();
            } else if ("Expense".equalsIgnoreCase(item.getCategory())) {
                totalExpenses += item.getAmount();
            }
        }

        // Update labels
        totalIncomeLabel.setText(String.format("%.2f", totalIncome));
        totalExpensesLabel.setText(String.format("%.2f", totalExpenses));
        netBalanceLabel.setText(String.format("%.2f", totalIncome - totalExpenses));
    }

    @FXML
    private void handleGenerateReport() {
        // Open a FileChooser for the user to select where to save the file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        // Show save dialog
        File file = fileChooser.showSaveDialog(reportsTable.getScene().getWindow());

        if (file != null) {
            saveReportToFile(file);
        }
    }

    private void saveReportToFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Write header
            writer.write("Name,Category,Amount,Date,Details");
            writer.newLine();

            // Write each report item
            for (ReportItem item : reportItems) {
                writer.write(String.format("%s,%s,%.2f,%s,%s",
                        item.getName(),
                        item.getCategory(),
                        item.getAmount(),
                        item.getDate(),
                        item.getDetails()));
                writer.newLine();
            }

            showAlert(Alert.AlertType.INFORMATION, "Success", "Report saved successfully!");

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save the report: " + e.getMessage());
        }
    }

    @FXML
    private void handleApplyFilter(ActionEvent event) {
        // Get the selected filter from the ChoiceBox
        String selectedFilter = filterChoiceBox.getValue();

        // Create a list to store filtered items
        ObservableList<ReportItem> filteredItems = FXCollections.observableArrayList();

        // Apply filtering logic based on the selected filter
        for (ReportItem item : reportItems) {
            if ("All".equals(selectedFilter)) {
                // Add all items if "All" is selected
                filteredItems.add(item);
            } else if ("Subscriptions".equalsIgnoreCase(selectedFilter) && "Income".equalsIgnoreCase(item.getCategory())) {
                // Add only "Income" items for Subscriptions filter
                filteredItems.add(item);
            } else if ("Expenses".equalsIgnoreCase(selectedFilter) && "Expense".equalsIgnoreCase(item.getCategory())) {
                // Add only "Expense" items for Expenses filter
                filteredItems.add(item);
            }
        }

        // Sort the filtered list
        filteredItems.sort((item1, item2) -> {
            // Sort by amount in descending order
            return Double.compare(item2.getAmount(), item1.getAmount());
        });

        // Update the table with the filtered and sorted data
        reportsTable.setItems(filteredItems);

        // Optionally, update the totals for the filtered items
        updateFilteredTotals(filteredItems);
    }

    // Helper function to update totals for the filtered items
    private void updateFilteredTotals(ObservableList<ReportItem> filteredItems) {
        double totalIncome = 0;
        double totalExpenses = 0;

        for (ReportItem item : filteredItems) {
            if ("Income".equalsIgnoreCase(item.getCategory())) {
                totalIncome += item.getAmount();
            } else if ("Expense".equalsIgnoreCase(item.getCategory())) {
                totalExpenses += item.getAmount();
            }
        }

        // Update labels with the filtered totals
        totalIncomeLabel.setText(String.format("%.2f", totalIncome));
        totalExpensesLabel.setText(String.format("%.2f", totalExpenses));
        netBalanceLabel.setText(String.format("%.2f", totalIncome - totalExpenses));
    }



    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
