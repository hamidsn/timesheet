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
