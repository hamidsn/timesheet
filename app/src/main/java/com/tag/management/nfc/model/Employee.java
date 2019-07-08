package com.tag.management.nfc.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "employee")
public class Employee {

    // @PrimaryKey(autoGenerate = true)
    // private int id;

    @ColumnInfo(name = "employer_name")
    private String employerName;

    // @ColumnInfo(name = "employee_full_name")
    private String employeeFullName;

    @ColumnInfo(name = "employer_uid")
    private String employerUid;

    @ColumnInfo(name = "employee_download_url")
    private String employeeDownloadUrl;

    @ColumnInfo(name = "employee_email")
    private String employeeEmail;

    @ColumnInfo(name = "employee_available")
    private boolean employeeAvailable;

    @PrimaryKey
    @NonNull
    private String employeeUniqueId;

    //@Ignore
    public Employee(String employerName, String employeeFullName, String employerUid, String downloadUrl, String employeeEmail, String employeeUniqueId, boolean employeeAvailable) {
        this.employerName = employerName;
        this.employeeFullName = employeeFullName;
        this.employerUid = employerUid;
        this.employeeDownloadUrl = downloadUrl;
        this.employeeEmail = employeeEmail;
        this.employeeUniqueId = employeeUniqueId;
        this.employeeAvailable = employeeAvailable;
    }

    @Ignore
    public Employee(String employerName, String employeeFullName, String employerUid, String downloadUrl, String employeeEmail, String employeeUniqueId) {
        this.employerName = employerName;
        this.employeeFullName = employeeFullName;
        this.employerUid = employerUid;
        this.employeeDownloadUrl = downloadUrl;
        this.employeeEmail = employeeEmail;
        this.employeeUniqueId = employeeUniqueId;
    }

    public Employee() {
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
