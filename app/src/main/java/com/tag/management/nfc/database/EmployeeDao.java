package com.tag.management.nfc.database;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface EmployeeDao {

    @Query("SELECT * FROM employee ORDER BY employeeFullName")
    LiveData<List<EmployeeEntry>> loadAllEmployees();

    @Insert
    void insertEmployee(EmployeeEntry employeeEntry);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateEmployee(EmployeeEntry employeeEntry);

    @Delete
    void deleteEmployee(EmployeeEntry employeeEntry);

    @Query("SELECT * FROM employee WHERE employeeUniqueId = :employeeUniqueId")
    EmployeeEntry loadEmployeeByUid(String employeeUniqueId);
}
