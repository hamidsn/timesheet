package com.tag.management.nfc.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "employee")
public class EmployeeEntry {

    //@PrimaryKey(autoGenerate = true)
    private String employerName;
    private String employeeFullName;
    private String employerUid;
    private String employeeDownloadUrl;
    private String employeeEmail;
    private boolean employeeAvailable;

    @PrimaryKey
    @NonNull
    private String employeeUniqueId;

    @Ignore
    public EmployeeEntry(String employerName, String employeeFullName, String employerUid, String downloadUrl, String employeeEmail, String employeeUniqueId, boolean employeeAvailable) {
        this.employerName = employerName;
        this.employeeFullName = employeeFullName;
        this.employerUid = employerUid;
        this.employeeDownloadUrl = downloadUrl;
        this.employeeEmail = employeeEmail;
        this.employeeUniqueId = employeeUniqueId;
        this.employeeAvailable = employeeAvailable;
    }

    public EmployeeEntry() {
    }
/*
    public EmployeeEntry(int id, String employerName, String employeeFullName, String employerUid, String downloadUrl, String employeeEmail, String employeeUniqueId) {
        this.employerName = employerName;
        this.employeeFullName = employeeFullName;
        this.employerUid = employerUid;
        this.employeeDownloadUrl = downloadUrl;
        this.employeeEmail = employeeEmail;
        this.employeeUniqueId = employeeUniqueId;
        this.id = id;
    }*/

/*    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }*/

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

    public String getEmployerUid() {
        return employerUid;
    }

    public void setEmployerUid(String employerUid) {
        this.employerUid = employerUid;
    }

    public String getEmployeeDownloadUrl() {
        return employeeDownloadUrl;
    }

    public void setEmployeeDownloadUrl(String employeeDownloadUrl) {
        this.employeeDownloadUrl = employeeDownloadUrl;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    public String getEmployeeUniqueId() {
        return employeeUniqueId;
    }

    public void setEmployeeUniqueId(String employeeUniqueId) {
        this.employeeUniqueId = employeeUniqueId;
    }

    public boolean isEmployeeAvailable() {
        return employeeAvailable;
    }

    public void setEmployeeAvailable(boolean employeeAvailable) {
        this.employeeAvailable = employeeAvailable;
    }
}
