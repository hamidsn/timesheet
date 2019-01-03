package com.tag.management.nfc.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface DailyActivityDao {

    @Query("SELECT * FROM dailyactivity ORDER BY employeeTimestampIn")
    LiveData<List<DailyActivityEntry>> loadAllEmployees();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEmployee(DailyActivityEntry dailyActivityEntry);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateEmployee(DailyActivityEntry dailyActivityEntry);

    @Delete
    void deleteEmployee(DailyActivityEntry dailyActivityEntry);

    @Query("SELECT * FROM dailyactivity WHERE id = :id")
    LiveData<DailyActivityEntry> loadEmployeeById(int id);

    @Query("SELECT * FROM dailyactivity WHERE employeeUniqueId = :employeeUniqueId")
    DailyActivityEntry loadEmployeeByUid(String employeeUniqueId);
}
