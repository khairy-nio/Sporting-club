package com.example.club_sporting_final.admin.Controller;

import com.example.club_sporting_final.admin.module.Expense;
import com.example.club_sporting_final.utils.DatabaseConnection;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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

public class ExpenseManagementController {

    @FXML
    private TableView<Expense> expenseTable;

    @FXML
    private TableColumn<Expense, Integer> expenseIDColumn;

    @FXML
    private TableColumn<Expense, String> expenseTypeColumn;

    @FXML
    private TableColumn<Expense, Double> amountColumn;

    @FXML
    private TableColumn<Expense, String> dateColumn;

    @FXML
    private TextField expenseTypeField;

    @FXML
    private TextField amountField;

    @FXML
    private DatePicker datePicker;

    private ObservableList<Expense> expenseList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize table columns
        expenseIDColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getExpenseID()).asObject());
        expenseTypeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getExpenseType()));
        amountColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));

        // Load expenses into the table
        loadExpenses();

        // Add listener for table row selection
        expenseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateFields(newSelection);
            }
        });

        // Add input restrictions for the Amount field
        restrictInputToNumeric(amountField, true); // Allow decimals
    }

    private void loadExpenses() {
        expenseList.clear();
        String query = "SELECT * FROM expenses";

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                expenseList.add(new Expense(
                        rs.getInt("ExpenseID"),
                        rs.getString("ExpenseType"),
                        rs.getDouble("Amount"),
                        rs.getString("Date")
                ));
            }
            expenseTable.setItems(expenseList);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load expenses: " + e.getMessage());
        }
    }

    private void populateFields(Expense expense) {
        expenseTypeField.setText(expense.getExpenseType());
        amountField.setText(String.valueOf(expense.getAmount()));
        if (expense.getDate() != null && !expense.getDate().isEmpty()) {
            datePicker.setValue(java.time.LocalDate.parse(expense.getDate()));
        } else {
            datePicker.setValue(null);
        }
    }

    @FXML
    private void handleAddExpense(ActionEvent event) {
        String expenseType = expenseTypeField.getText().trim();
        String date = datePicker.getValue() != null ? datePicker.getValue().toString() : "";
        String amountInput = amountField.getText().trim();

        // Validation
        if (expenseType.isEmpty() || date.isEmpty() || amountInput.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields must be filled.");
            return;
        }

        if (!amountInput.matches("\\d+(\\.\\d+)?")) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be a valid number.");
            return;
        }

        double amount = Double.parseDouble(amountInput);

        if (amount <= 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be a positive value.");
            return;
        }

        String query = "INSERT INTO expenses (ExpenseType, Amount, Date) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, expenseType);
            stmt.setDouble(2, amount);
            stmt.setString(3, date);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                loadExpenses(); // Reload the table
                showAlert(Alert.AlertType.INFORMATION, "Success", "Expense added successfully.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not add expense: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditExpense(ActionEvent event) {
        // Get the selected expense from the table
        Expense selectedExpense = expenseTable.getSelectionModel().getSelectedItem();

        // Check if an expense is selected
        if (selectedExpense == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Error", "Please select an expense to edit.");
            return;
        }

        // Get values from the input fields
        String expenseType = expenseTypeField.getText().trim();
        String date = datePicker.getValue() != null ? datePicker.getValue().toString() : "";
        String amountInput = amountField.getText().trim();

        // Validation
        if (expenseType.isEmpty() || date.isEmpty() || amountInput.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields must be filled.");
            return;
        }

        if (!amountInput.matches("\\d+(\\.\\d+)?")) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be a valid number.");
            return;
        }

        double amount = Double.parseDouble(amountInput);

        if (amount <= 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be a positive value.");
            return;
        }

        // Update the expense in the database
        String query = "UPDATE expenses SET ExpenseType = ?, Amount = ?, Date = ? WHERE ExpenseID = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, expenseType);
            stmt.setDouble(2, amount);
            stmt.setString(3, date);
            stmt.setInt(4, selectedExpense.getExpenseID());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                // Update the table view
                selectedExpense.setExpenseType(expenseType); // Update the property
                selectedExpense.setAmount(amount);           // Update the property
                selectedExpense.setDate(date);               // Update the property
                expenseTable.refresh();                      // Refresh the table to reflect changes
                showAlert(Alert.AlertType.INFORMATION, "Success", "Expense updated successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update the selected expense.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not update expense: " + e.getMessage());
        }
    }



    private void restrictInputToNumeric(TextField textField, boolean allowDecimal) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (allowDecimal) {
                if (!newValue.matches("\\d*(\\.\\d*)?")) {
                    textField.setText(oldValue);
                }
            } else {
                if (!newValue.matches("\\d*")) {
                    textField.setText(oldValue);
                }
            }
        });
    }

    @FXML
    private void handleDeleteExpense(ActionEvent event) {
        Expense selectedExpense = expenseTable.getSelectionModel().getSelectedItem();

        if (selectedExpense == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Error", "Please select an expense to delete.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Delete");
        confirmationAlert.setHeaderText("Are you sure you want to delete this expense?");
        confirmationAlert.setContentText("Expense ID: " + selectedExpense.getExpenseID());

        if (confirmationAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        String query = "DELETE FROM expenses WHERE ExpenseID = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, selectedExpense.getExpenseID());
            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted > 0) {
                expenseList.remove(selectedExpense);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Expense deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete the selected expense.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while deleting the expense: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearFields(ActionEvent event) {
        expenseTypeField.clear();
        amountField.clear();
        datePicker.setValue(null);
        expenseTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/club_sporting_final/admin/DashBoard.fxml"));
            Scene dashboardScene = new Scene(loader.load());

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(dashboardScene);
            currentStage.setTitle("Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to load the dashboard. Please try again.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
