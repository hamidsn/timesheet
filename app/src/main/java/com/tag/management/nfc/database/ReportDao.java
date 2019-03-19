package com.tag.management.nfc.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface ReportDao {

    @Query("SELECT * FROM report ORDER BY id")
    List<ReportEntry> loadAllReports();

    @Insert
    void insertReport(ReportEntry reportEntry);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateReport(ReportEntry reportEntry);

    @Delete
    void deleteReport(ReportEntry reportEntry);

    @Query("SELECT * FROM report WHERE employeeUniqueId = :employeeUniqueId")
    ReportEntry loadReportByUid(String employeeUniqueId);
}
