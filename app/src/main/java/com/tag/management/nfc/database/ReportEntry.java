package com.tag.management.nfc.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "report")
public class ReportEntry {

    private String employeeFullName;
    private String employeeTimestampIn;
    private String employeeTimestampOut;
    private String employeeUniqueId;
    private String employerName;

    @PrimaryKey
    @NonNull
    private long id;

    @Ignore
    public ReportEntry(String employeeFullName, String employeeTimestampIn, String employeeTimestampOut, String employeeUniqueId, String employerName, long id) {
        this.employeeFullName = employeeFullName;
        this.employeeTimestampIn = employeeTimestampIn;
        this.employeeTimestampOut = employeeTimestampOut;
        this.employeeUniqueId = employeeUniqueId;
        this.employerName = employerName;
        this.id = id;
    }

    public ReportEntry() {
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

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
