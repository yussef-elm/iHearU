package com.ensias.ihearu;

import android.app.Application;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;

import com.ensias.ihearu.R;

import java.util.Collections;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.createNotificationChannelsCompat(Collections.singletonList(
                new NotificationChannelCompat.Builder(getString(R.string.error_report_channel_id),
                        NotificationManagerCompat.IMPORTANCE_LOW)
                        .setName(getString(R.string.error_report_channel_name))
                        .setDescription(getString(R.string.error_report_channel_description))
                        .build()));
    }
}
