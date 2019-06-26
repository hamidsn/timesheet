package com.tag.management.nfc.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReportDao {

    @Query("SELECT * FROM report ORDER BY employeeFullName")
    List<ReportEntry> loadAllReports();

    @Insert
    void insertReport(ReportEntry reportEntry);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateReport(ReportEntry reportEntry);

    @Delete
    void deleteReport(ReportEntry reportEntry);

    @Query("SELECT * FROM report WHERE employeeUniqueId = :employeeUniqueId")
    ReportEntry loadReportByUid(String employeeUniqueId);

    @Query("DELETE FROM report")
    public void nukeTable();
}
