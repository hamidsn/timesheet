package com.tag.management.nfc.model;

public class Employee {

    private String employerName;
    private String employeeFullName;
    private String employerUid;
    private String employeeDownloadUrl;
    private String employeeEmail;
    private String employeeUniqueId;

    public Employee() {
    }

    public Employee(String employerName, String employeeFullName, String employerUid, String downloadUrl, String employeeEmail, String employeeUniqueId) {
        this.employerName = employerName;
        this.employeeFullName = employeeFullName;
        this.employerUid = employerUid;
        this.employeeDownloadUrl = downloadUrl;
        this.employeeEmail = employeeEmail;
        this.employeeUniqueId = employeeUniqueId;
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
}
