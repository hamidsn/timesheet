package com.tag.management.nfc.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "dailyactivity")
public class DailyActivityEntry {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String employerName;
    private String employeeFullName;
    private String employeeTimestampIn;
    private String employeeTimestampOut;
    private String employeeUniqueId;

    @Ignore
    public DailyActivityEntry(String employerName, String employeeFullName, String employeeTimestampIn, String employeeTimestampOut, String employeeUniqueId) {
        this.employerName = employerName;
        this.employeeFullName = employeeFullName;
        this.employeeTimestampIn = employeeTimestampIn;
        this.employeeTimestampOut = employeeTimestampOut;
        this.employeeUniqueId = employeeUniqueId;
    }

    public DailyActivityEntry(int id, String employerName, String employeeFullName, String employeeTimestampIn, String employeeTimestampOut, String employeeUniqueId) {
        this.id = id;
        this.employerName = employerName;
        this.employeeFullName = employeeFullName;
        this.employeeTimestampIn = employeeTimestampIn;
        this.employeeTimestampOut = employeeTimestampOut;
        this.employeeUniqueId = employeeUniqueId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Ignore
    public DailyActivityEntry() {
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getEmployeeFullName() {
        return employeeFullName;
    }

    public void setEmployeeFullName(String employeeFullName) {
        this.employeeFullName = employeeFullName;
    }

    public String getEmployeeTimestampIn() {
        return employeeTimestampIn;
    }

    public void setEmployeeTimestampIn(String employeeTimestampIn) {
        this.employeeTimestampIn = employeeTimestampIn;
    }

    public String getEmployeeTimestampOut() {
        return employeeTimestampOut;
    }

    public void setEmployeeTimestampOut(String employeeTimestampOut) {
        this.employeeTimestampOut = employeeTimestampOut;
    }

    public String getEmployeeUniqueId() {
        return employeeUniqueId;
    }

    public void setEmployeeUniqueId(String employeeUniqueId) {
        this.employeeUniqueId = employeeUniqueId;
    }

}
