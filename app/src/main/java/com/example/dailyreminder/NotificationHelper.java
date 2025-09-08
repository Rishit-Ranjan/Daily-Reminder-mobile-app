package com.example.dailyreminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

public class NotificationHelper extends ContextWrapper {
    public static final String CHANNEL_ID = "dailyReminderChannel";
    public static final String CHANNEL_NAME = "Daily Reminder Channel";
    private NotificationManager mManager;

    public NotificationHelper(Context base) {
        super(base);
        createNotificationChannel(); // Always attempt to create, safe if already exists
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for Daily Reminder notifications");
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            getManager().createNotificationChannel(channel);
        }
    }

    public NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }
}
