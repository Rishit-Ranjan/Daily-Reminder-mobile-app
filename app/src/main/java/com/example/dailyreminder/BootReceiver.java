package com.example.dailyreminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            DBHelper dbHelper = new DBHelper(context);
            List<Task> tasks = dbHelper.getAllTasks();

            if (tasks != null) {
                for (Task task : tasks) {
                    rescheduleNotification(context, task);
                }
            }
        }
    }

    private void rescheduleNotification(Context context, Task task) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Check for SCHEDULE_EXACT_ALARM permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Cannot schedule exact alarms, potentially log this or notify user in a different way
                // For a BootReceiver, direct UI like Toast or starting an activity is not recommended.
                // Consider logging or a fallback notification if this permission is critical.
                System.err.println("BootReceiver: Cannot schedule exact alarms, permission not granted.");
                return;
            }
        }

        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        notificationIntent.putExtra("TASK_ID", task.getId());
        notificationIntent.putExtra("TASK_TITLE", task.getTitle());
        notificationIntent.putExtra("TASK_DESCRIPTION", task.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId(),
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Date parsedDate = dateFormat.parse(task.getDate());
            if (parsedDate != null) {
                Calendar dateCal = Calendar.getInstance();
                dateCal.setTime(parsedDate);
                calendar.set(Calendar.YEAR, dateCal.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, dateCal.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH));
            } else {
                System.err.println("BootReceiver: Parsed date is null for task " + task.getId());
                return; // Skip if date is invalid
            }

            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date parsedTime = timeFormat.parse(task.getTime());
            if (parsedTime != null) {
                Calendar timeCal = Calendar.getInstance();
                timeCal.setTime(parsedTime);
                calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            } else {
                System.err.println("BootReceiver: Parsed time is null for task " + task.getId());
                return; // Skip if time is invalid
            }
            calendar.set(Calendar.SECOND, 0);

            // Only schedule if the time is in the future
            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                System.out.println("BootReceiver: Task " + task.getId() + " is in the past, not rescheduling.");
            }

        } catch (ParseException e) {
            e.printStackTrace();
            System.err.println("BootReceiver: Error parsing date/time for task " + task.getId());
        } catch (SecurityException se) {
            // This might happen if SCHEDULE_EXACT_ALARM was revoked after being granted
            se.printStackTrace();
            System.err.println("BootReceiver: SecurityException while setting alarm for task " + task.getId());
        }
    }
}
