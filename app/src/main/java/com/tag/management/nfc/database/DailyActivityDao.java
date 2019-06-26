package com.tag.management.nfc.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DailyActivityDao {

    @Query("SELECT * FROM dailyactivity ORDER BY employeeTimestampIn")
    List<DailyActivityEntry> loadAllEmployees();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEmployee(DailyActivityEntry dailyActivityEntry);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateEmployee(DailyActivityEntry dailyActivityEntry);

    @Delete
    void deleteEmployee(DailyActivityEntry dailyActivityEntry);

    @Query("SELECT * FROM dailyactivity WHERE id = :id")
    DailyActivityEntry loadEmployeeById(int id);

    @Query("SELECT * FROM dailyactivity WHERE employeeUniqueId = :employeeUniqueId")
    DailyActivityEntry loadEmployeeByUid(String employeeUniqueId);
}
