package com.example.club_sporting_final.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    // Singleton instance
    private static DatabaseConnection instance;

    // SQLite Database URL (saves to sportingclub.db in the project folder)
    private static final String URL = "jdbc:sqlite:sportingclub.db";

    // Connection object
    private Connection connection;

    // Private constructor for singleton
    private DatabaseConnection() {
        try {
            System.out.println("Initializing SQLite database connection...");
            connection = DriverManager.getConnection(URL);
            connection.setAutoCommit(true); // Enable auto-commit for transactions
            System.out.println("SQLite database connection established successfully.");
            initializeDatabase(connection);
        } catch (SQLException e) {
            System.err.println("Error connecting to the SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Automatically initialize SQLite database tables and seed test data
    private void initializeDatabase(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Enable foreign keys in SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. Create users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "password TEXT NOT NULL, " +
                    "role TEXT NOT NULL" +
                    ");");

            // 2. Create Teams table
            stmt.execute("CREATE TABLE IF NOT EXISTS Teams (" +
                    "TeamID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "TeamName TEXT NOT NULL, " +
                    "CoachName TEXT, " +
                    "Category TEXT, " +
                    "MemberCount INTEGER DEFAULT 0, " +
                    "TeamLeaderID INTEGER" +
                    ");");

            // 3. Create members table
            stmt.execute("CREATE TABLE IF NOT EXISTS members (" +
                    "MemberID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Name TEXT NOT NULL, " +
                    "Email TEXT, " +
                    "PhoneNumber TEXT, " +
                    "SubscriptionStatus BOOLEAN DEFAULT 0, " +
                    "TeamID INTEGER, " +
                    "FOREIGN KEY(TeamID) REFERENCES Teams(TeamID) ON DELETE SET NULL" +
                    ");");

            // 4. Create team_members table
            stmt.execute("CREATE TABLE IF NOT EXISTS team_members (" +
                    "MemberID INTEGER, " +
                    "TeamID INTEGER, " +
                    "PRIMARY KEY (MemberID, TeamID), " +
                    "FOREIGN KEY(MemberID) REFERENCES members(MemberID) ON DELETE CASCADE, " +
                    "FOREIGN KEY(TeamID) REFERENCES Teams(TeamID) ON DELETE CASCADE" +
                    ");");

            // 5. Create subscriptions table
            stmt.execute("CREATE TABLE IF NOT EXISTS subscriptions (" +
                    "SubscriptionID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "MemberID INTEGER, " +
                    "PlanType TEXT NOT NULL, " +
                    "StartDate TEXT, " +
                    "EndDate TEXT, " +
                    "Amount REAL NOT NULL, " +
                    "FOREIGN KEY(MemberID) REFERENCES members(MemberID) ON DELETE CASCADE" +
                    ");");

            // 6. Create expenses table
            stmt.execute("CREATE TABLE IF NOT EXISTS expenses (" +
                    "ExpenseID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "ExpenseType TEXT NOT NULL, " +
                    "Amount REAL NOT NULL, " +
                    "Date TEXT NOT NULL" +
                    ");");

            // 7. Create attendance table
            stmt.execute("CREATE TABLE IF NOT EXISTS attendance (" +
                    "AttendanceID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "MemberID INTEGER NOT NULL, " +
                    "Date TEXT NOT NULL, " +
                    "SessionType TEXT NOT NULL, " +
                    "Status TEXT NOT NULL DEFAULT 'Present', " +
                    "FOREIGN KEY(MemberID) REFERENCES members(MemberID) ON DELETE CASCADE" +
                    ");");


            // ==========================================
            // Seed Default Data if Tables are Empty
            // ==========================================

            // Seed default users if empty
            stmt.execute("INSERT OR IGNORE INTO users (username, password, role) VALUES ('admin', 'admin', 'admin');");
            stmt.execute("INSERT OR IGNORE INTO users (username, password, role) VALUES ('emp', 'emp', 'emp');");

            // Seed default Teams
            try (ResultSet rsTeams = stmt.executeQuery("SELECT COUNT(*) FROM Teams;")) {
                if (rsTeams.next() && rsTeams.getInt(1) == 0) {
                    stmt.execute("INSERT INTO Teams (TeamID, TeamName, CoachName, Category, MemberCount, TeamLeaderID) VALUES " +
                            "(1, 'Football Team A', 'Coach John', 'Senior', 2, NULL), " +
                            "(2, 'Basketball Team B', 'Coach Sarah', 'Junior', 2, NULL), " +
                            "(3, 'Tennis Club', 'Coach Michael', 'All Ages', 1, NULL);");
                }
            }

            // Seed default members
            try (ResultSet rsMembers = stmt.executeQuery("SELECT COUNT(*) FROM members;")) {
                if (rsMembers.next() && rsMembers.getInt(1) == 0) {
                    stmt.execute("INSERT INTO members (MemberID, Name, Email, PhoneNumber, SubscriptionStatus, TeamID) VALUES " +
                            "(1, 'Mohamed Aly', 'mohamed@example.com', '+20123456789', 1, 1), " +
                            "(2, 'Jessica Taylor', 'jessica@example.com', '+15550199', 1, 1), " +
                            "(3, 'Ahmed El-Khoury', 'ahmed@example.com', '+9613123456', 0, 2), " +
                            "(4, 'Sarah Connor', 'sarah@example.com', '+15550244', 1, 2), " +
                            "(5, 'Pierre Dubois', 'pierre@example.com', '+3361234567', 1, 3);");
                }
            }

            // Seed default team_members mapping
            try (ResultSet rsTeamMembers = stmt.executeQuery("SELECT COUNT(*) FROM team_members;")) {
                if (rsTeamMembers.next() && rsTeamMembers.getInt(1) == 0) {
                    stmt.execute("INSERT INTO team_members (MemberID, TeamID) VALUES " +
                            "(1, 1), (2, 1), (3, 2), (4, 2), (5, 3);");
                }
            }

            // Seed default subscriptions
            try (ResultSet rsSubscriptions = stmt.executeQuery("SELECT COUNT(*) FROM subscriptions;")) {
                if (rsSubscriptions.next() && rsSubscriptions.getInt(1) == 0) {
                    stmt.execute("INSERT INTO subscriptions (SubscriptionID, MemberID, PlanType, StartDate, EndDate, Amount) VALUES " +
                            "(1, 1, 'Gold Plan', '2026-05-02', '2026-12-31', 500.0), " +
                            "(2, 2, 'Silver Plan', '2026-05-12', '2026-08-31', 300.0), " +
                            "(3, 4, 'Basic Plan', '2026-05-18', '2026-06-30', 150.0), " +
                            "(4, 5, 'Premium Annual', '2026-05-20', '2027-01-14', 600.0);");
                }
            }

            // Seed default expenses
            try (ResultSet rsExpenses = stmt.executeQuery("SELECT COUNT(*) FROM expenses;")) {
                if (rsExpenses.next() && rsExpenses.getInt(1) == 0) {
                    stmt.execute("INSERT INTO expenses (ExpenseID, ExpenseType, Amount, Date) VALUES " +
                            "(1, 'Equipment Maintenance', 1200.0, '2026-04-10'), " +
                            "(2, 'Coach Salaries', 4500.0, '2026-05-01'), " +
                            "(3, 'Electricity & Utilities', 650.0, '2026-05-15'), " +
                            "(4, 'Marketing & Ads', 350.0, '2026-05-18');");
                }
            }
            
            System.out.println("SQLite database schema and testing data initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing SQLite database tables/seeding: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get the singleton instance
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // Get the connection
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("Reinitializing SQLite database connection...");
            connection = DriverManager.getConnection(URL);
            connection.setAutoCommit(true); // Ensure auto-commit is enabled
            System.out.println("SQLite database connection reestablished.");
        }
        return connection;
    }

    // Close the connection
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("SQLite database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing SQLite database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Test the database connection
    public static void main(String[] args) {
        try {
            DatabaseConnection dbConnection = DatabaseConnection.getInstance();
            Connection connection = dbConnection.getConnection();
            if (connection != null && !connection.isClosed()) {
                System.out.println("SQLite database connection test passed.");
                dbConnection.closeConnection();
            } else {
                System.err.println("Database connection test failed: Connection is null or closed.");
            }
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
