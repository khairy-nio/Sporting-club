-- ==========================================
-- Sporting Club Management Database Schema
-- Database Name: sportingclub
-- ==========================================

-- 1. Create the database if it doesn't exist
CREATE DATABASE IF NOT EXISTS sportingclub;
USE sportingclub;

-- 2. Drop existing tables if they exist to start fresh
DROP TABLE IF EXISTS team_members;
DROP TABLE IF EXISTS subscriptions;
DROP TABLE IF EXISTS expenses;
DROP TABLE IF EXISTS members;
DROP TABLE IF EXISTS Teams;
DROP TABLE IF EXISTS users;

-- 3. Users Table (Plain text credentials as expected by LoginController.java)
CREATE TABLE users (
    username VARCHAR(50) NOT NULL PRIMARY KEY,
    password VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL -- 'admin' or 'emp'
);

-- 4. Teams Table
CREATE TABLE Teams (
    TeamID INT AUTO_INCREMENT PRIMARY KEY,
    TeamName VARCHAR(100) NOT NULL,
    CoachName VARCHAR(100),
    Category VARCHAR(50),
    MemberCount INT DEFAULT 0,
    TeamLeaderID INT NULL
);

-- 5. Members Table
CREATE TABLE members (
    MemberID INT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Email VARCHAR(100),
    PhoneNumber VARCHAR(30),
    SubscriptionStatus BOOLEAN DEFAULT FALSE,
    TeamID INT DEFAULT NULL,
    FOREIGN KEY (TeamID) REFERENCES Teams(TeamID) ON DELETE SET NULL
);

-- 6. Team Members Table (Join Table)
CREATE TABLE team_members (
    MemberID INT NOT NULL,
    TeamID INT NOT NULL,
    PRIMARY KEY (MemberID, TeamID),
    FOREIGN KEY (MemberID) REFERENCES members(MemberID) ON DELETE CASCADE,
    FOREIGN KEY (TeamID) REFERENCES Teams(TeamID) ON DELETE CASCADE
);

-- 7. Subscriptions Table
CREATE TABLE subscriptions (
    SubscriptionID INT AUTO_INCREMENT PRIMARY KEY,
    MemberID INT NOT NULL,
    PlanType VARCHAR(50) NOT NULL,
    StartDate VARCHAR(50),
    EndDate VARCHAR(50),
    Amount DOUBLE NOT NULL,
    FOREIGN KEY (MemberID) REFERENCES members(MemberID) ON DELETE CASCADE
);

-- 8. Expenses Table
CREATE TABLE expenses (
    ExpenseID INT AUTO_INCREMENT PRIMARY KEY,
    ExpenseType VARCHAR(100) NOT NULL,
    Amount DOUBLE NOT NULL,
    Date VARCHAR(50) NOT NULL
);

-- ==========================================
-- Seed Data
-- ==========================================

-- Default user credentials:
-- Admin Dashboard: username "admin", password "admin"
-- Employee Dashboard: username "emp", password "emp"
INSERT INTO users (username, password, role) VALUES
('admin', 'admin', 'admin'),
('emp', 'emp', 'emp');

-- Default Teams
INSERT INTO Teams (TeamName, CoachName, Category, MemberCount) VALUES
('Football Team A', 'Coach John', 'Senior', 0),
('Basketball Team B', 'Coach Sarah', 'Junior', 0);
