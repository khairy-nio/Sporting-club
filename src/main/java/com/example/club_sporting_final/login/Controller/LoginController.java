package com.example.club_sporting_final.login.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import com.example.club_sporting_final.utils.DatabaseConnection;
import com.example.club_sporting_final.utils.SessionManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    /**
     * Handles the login action when the Login button is pressed.
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter username and password!");
            errorLabel.setVisible(true);
            return;
        }

        // Try hashed login first, fall back to plain-text for legacy seeded data
        String role = validateCredentials(username, hashPassword(password));
        if (role == null) {
            role = validateCredentials(username, password); // legacy fallback
        }

        if (role != null) {
            System.out.println("Login successful. Role: " + role);
            SessionManager.getInstance().login(username, role);
            if (role.equals("admin")) {
                loadDashboard("Sport Hub — Admin Dashboard");
            } else if (role.equals("emp")) {
                loadDashboard("Sport Hub — Employee Dashboard");
            }
        } else {
            System.out.println("Login failed.");
            errorLabel.setText("Invalid username or password!");
            errorLabel.setVisible(true);
        }
    }

    private String validateCredentials(String username, String password) {
        String query = "SELECT role FROM users WHERE username = ? AND password = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            System.out.println("Executing query: " + stmt);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (Exception e) {
            System.err.println("Error validating credentials: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Hashes a plain-text password using SHA-256.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private void loadDashboard(String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/club_sporting_final/admin/DashBoard.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception e) {
            System.err.println("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
