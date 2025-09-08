package com.example.dailyreminder;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.dailyreminder.R; // Corrected import
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class AddTaskActivity extends AppCompatActivity {

    private TextInputEditText titleEditText, descriptionEditText;
    private MaterialButton datePickerButton;
    private MaterialButton timePickerButton;
    private DBHelper dbHelper;
    private int mYear, mMonth, mDay, mHour, mMinute;

    private boolean isEditMode = false;
    private int taskId = -1;

    // ... (onCreate and other methods remain the same) ...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        dbHelper = new DBHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        datePickerButton = findViewById(R.id.datePickerButton);
        timePickerButton = findViewById(R.id.timePickerButton);
        MaterialButton saveTaskButton = findViewById(R.id.saveTaskButton);

        if (getIntent().hasExtra("TASK_ID")) {
            isEditMode = true;
            taskId = getIntent().getIntExtra("TASK_ID", -1);
            Objects.requireNonNull(getSupportActionBar()).setTitle("Edit Task");
            saveTaskButton.setText("Update Task");
            loadTaskData();
        } else {
            isEditMode = false;
            Objects.requireNonNull(getSupportActionBar()).setTitle("Add New Task");
            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);
            mHour = c.get(Calendar.HOUR_OF_DAY);
            mMinute = c.get(Calendar.MINUTE);
            updateDateText();
            updateTimeText();
        }

        datePickerButton.setOnClickListener(v -> showDatePicker());
        timePickerButton.setOnClickListener(v -> showTimePicker());
        saveTaskButton.setOnClickListener(v -> saveTask());
    }

    private void saveTask() {
        // --- PERMISSION CHECK ADDED HERE ---
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // If permission is not granted, guide the user and stop.
                Toast.makeText(this, "Permission required to set reminders. Please grant it.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return; // Stop the save process until permission is granted
            }
        }
        // --- END PERMISSION CHECK ---


        String title = Objects.requireNonNull(titleEditText.getText()).toString().trim();
        String description = Objects.requireNonNull(descriptionEditText.getText()).toString().trim();
        String date = datePickerButton.getText().toString();
        String time = timePickerButton.getText().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(mYear, mMonth, mDay, mHour, mMinute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, "The selected time is in the past. Please choose a future time.", Toast.LENGTH_LONG).show();
            return;
        }

        int newOrExistingId = isEditMode ? taskId : (int) System.currentTimeMillis();
        Task task = new Task(newOrExistingId, title, description, date, time);

        if (isEditMode) {
            dbHelper.updateTask(task);
            Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.addTaskWithId(task);
            Toast.makeText(this, "Task saved!", Toast.LENGTH_SHORT).show();
        }

        scheduleNotification(task);
        finish();
    }

    private void scheduleNotification(Task task) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("TASK_ID", task.getId());
        intent.putExtra("TASK_TITLE", task.getTitle());
        intent.putExtra("TASK_DESCRIPTION", task.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, task.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(mYear, mMonth, mDay, mHour, mMinute);
        calendar.set(Calendar.SECOND, 0);

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            Toast.makeText(this, "Reminder set for " + task.getDate() + " at " + task.getTime(), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            // This is a fallback, but the check in saveTask() should prevent it.
            Toast.makeText(this, "Security error: Could not set exact alarm.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // ... (The rest of the file: loadTaskData, showDatePicker, etc. remains unchanged) ...
    private void loadTaskData() {
        Task task = dbHelper.getTask(taskId);
        if (task == null) {
            Toast.makeText(this, "Error: Task not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        titleEditText.setText(task.getTitle());
        descriptionEditText.setText(task.getDescription());

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Date date = dateFormat.parse(task.getDate());
            Calendar calDate = Calendar.getInstance();
            if (date != null) {
                calDate.setTime(date);
            }
            mYear = calDate.get(Calendar.YEAR);
            mMonth = calDate.get(Calendar.MONTH);
            mDay = calDate.get(Calendar.DAY_OF_MONTH);
            updateDateText();

            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date time = timeFormat.parse(task.getTime());
            Calendar calTime = Calendar.getInstance();
            if (time != null) {
                calTime.setTime(time);
            }
            mHour = calTime.get(Calendar.HOUR_OF_DAY);
            mMinute = calTime.get(Calendar.MINUTE);
            updateTimeText();

        } catch (ParseException e) {
            e.printStackTrace();
            datePickerButton.setText(task.getDate());
            timePickerButton.setText(task.getTime());
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    mYear = year;
                    mMonth = monthOfYear;
                    mDay = dayOfMonth;
                    updateDateText();
                }, mYear, mMonth, mDay);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    mHour = hourOfDay;
                    mMinute = minute;
                    updateTimeText();
                }, mHour, mMinute, false);
        timePickerDialog.show();
    }

    private void updateDateText() {
        datePickerButton.setText(String.format(Locale.getDefault(), "%02d-%02d-%d", mDay, mMonth + 1, mYear));
    }

    private void updateTimeText() {
        String amPm = mHour < 12 ? "AM" : "PM";
        int hour = mHour % 12;
        if (hour == 0) hour = 12; // Adjust for 12 AM/PM
        timePickerButton.setText(String.format(Locale.getDefault(), "%02d:%02d %s", hour, mMinute, amPm));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}