package com.example.dailyreminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.dailyreminder.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class MainActivity extends AppCompatActivity {

  private ListView taskListView;
  private TextView emptyStateTextView;
  private DBHelper dbHelper;
    private List<Task> taskList;

  private final ActivityResultLauncher<String> requestPermissionLauncher =
          registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (!isGranted) {
              Toast.makeText(this, "Notification permission is required to show reminders.", Toast.LENGTH_LONG).show();
            }
          });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    dbHelper = new DBHelper(this);
    taskListView = findViewById(R.id.taskListView);
    emptyStateTextView = findViewById(R.id.emptyStateTextView);
    FloatingActionButton addTaskFAB = findViewById(R.id.addTaskFAB);

    askNotificationPermission();

    addTaskFAB.setOnClickListener(v -> {
      Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
      startActivity(intent);
    });

    taskListView.setOnItemClickListener((parent, view, position, id) -> {
      Task selectedTask = taskList.get(position);
      Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
      intent.putExtra("TASK_ID", selectedTask.getId());
      startActivity(intent);
    });

    taskListView.setOnItemLongClickListener((parent, view, position, id) -> {
      final Task selectedTask = taskList.get(position);
      new AlertDialog.Builder(MainActivity.this)
              .setTitle("Delete Task")
              .setMessage("Are you sure you want to delete this task?")
              .setPositiveButton("Yes", (dialog, which) -> {
                cancelNotification(selectedTask.getId());
                dbHelper.deleteTask(selectedTask.getId());
                loadTasks(); // Refresh the list
                Toast.makeText(MainActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
              })
              .setNegativeButton("No", null)
              .show();
      return true;
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);

    MenuItem item = menu.findItem(R.id.action_theme_switch);
    // Get the custom action view
    View actionView = item.getActionView();
    if (actionView != null) {
        // Find the SwitchMaterial within the custom action view
        SwitchMaterial themeSwitch = actionView.findViewById(R.id.theme_switch_widget);

        if (themeSwitch != null) {
          // --- THEME LOGIC ---
          SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
          boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);

          themeSwitch.setChecked(isDarkMode);

          // Apply theme on creation (though typically this is done in onCreate or MyApplication for initial setup)
          if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
          } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
          }

          themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (isChecked) {
              AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
              editor.putBoolean("isDarkMode", true);
            } else {
              AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
              editor.putBoolean("isDarkMode", false);
            }
            editor.apply();
          });
        }
    }
    return true;
  }


  @Override
  protected void onResume() {
    super.onResume();
    loadTasks();
  }

  private void loadTasks() {
    taskList = dbHelper.getAllTasks();
    if (taskList != null && !taskList.isEmpty()) {
        TaskListAdapter adapter = new TaskListAdapter(this, taskList);
      taskListView.setAdapter(adapter);
      taskListView.setVisibility(View.VISIBLE);
      emptyStateTextView.setVisibility(View.GONE);
    } else {
      taskListView.setVisibility(View.GONE);
      emptyStateTextView.setVisibility(View.VISIBLE);
    }
  }

  private void cancelNotification(int taskId) {
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    Intent intent = new Intent(this, NotificationReceiver.class);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    alarmManager.cancel(pendingIntent);
  }

  private void askNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
              PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
      }
    }
  }
}
