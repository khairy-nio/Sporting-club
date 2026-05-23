package com.example.club_sporting_final.admin.module;

import javafx.beans.property.*;

public class Attendance {
    private final IntegerProperty attendanceID;
    private final IntegerProperty memberID;
    private final StringProperty memberName;
    private final StringProperty date;
    private final StringProperty sessionType;
    private final StringProperty status;

    public Attendance(int attendanceID, int memberID, String memberName, String date, String sessionType, String status) {
        this.attendanceID = new SimpleIntegerProperty(attendanceID);
        this.memberID = new SimpleIntegerProperty(memberID);
        this.memberName = new SimpleStringProperty(memberName);
        this.date = new SimpleStringProperty(date);
        this.sessionType = new SimpleStringProperty(sessionType);
        this.status = new SimpleStringProperty(status);
    }

    public int getAttendanceID() { return attendanceID.get(); }
    public IntegerProperty attendanceIDProperty() { return attendanceID; }

    public int getMemberID() { return memberID.get(); }
    public IntegerProperty memberIDProperty() { return memberID; }

    public String getMemberName() { return memberName.get(); }
    public StringProperty memberNameProperty() { return memberName; }

    public String getDate() { return date.get(); }
    public StringProperty dateProperty() { return date; }
    public void setDate(String date) { this.date.set(date); }

    public String getSessionType() { return sessionType.get(); }
    public StringProperty sessionTypeProperty() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType.set(sessionType); }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public void setStatus(String status) { this.status.set(status); }
}
