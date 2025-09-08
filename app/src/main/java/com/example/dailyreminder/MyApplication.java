package com.example.dailyreminder;

import android.app.Application;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize NotificationHelper to create the notification channel
        new NotificationHelper(this);
    }
}
