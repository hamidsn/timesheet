package com.tag.management.nfc.model;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import android.util.Log;

import com.tag.management.nfc.database.AppDatabase;
import com.tag.management.nfc.database.EmployeeEntry;

import java.util.List;

public class MainViewModel extends AndroidViewModel {

    // Constant for logging
    private static final String TAG = MainViewModel.class.getSimpleName();

    private LiveData<List<EmployeeEntry>> employees;

    public MainViewModel(Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(this.getApplication());
        Log.d(TAG, "Actively retrieving the employees from the DataBase");
        employees = database.employeeDao().loadAllEmployees();
    }

    public LiveData<List<EmployeeEntry>> getEmployees() {
        return employees;
    }
}
