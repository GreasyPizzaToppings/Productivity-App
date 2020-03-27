//TODO: Actually implement the notification system. It doesn't work at the moment.
package com.example.productivityappprototype;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/*
This class receives the alarm broadcast, creating and displaying the notification to remind the user to start the scheduled item.
*/
public class ScheduledItemReminder extends BroadcastReceiver {
    private NotificationManager notificationManager;
    private static final String PRIMARY_CHANNEL_ID = "primary_notification_channel"; //The id for the primary notification channel
    private static final int NOTIFICATION_ID = 0; //The id for the first notification
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        createNotificationChannel();

        //Get the builder and send the notification
        NotificationCompat.Builder builder = getNotificationBuilder(intent.getStringExtra("scheduledItemName"));
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private NotificationCompat.Builder getNotificationBuilder(String scheduledItemName) {
        Intent notificationIntent = new Intent(context, MainActivity.class); //An intent to launch the app to the main screen
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT); //Set up the pending intent

        //Use the builder to create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PRIMARY_CHANNEL_ID)
                .setContentTitle("Upcoming Scheduled Item")
                .setContentText("An item starts in 5 minutes! Item: '" + scheduledItemName + "'") //Tell the user what task is due to be started soon
                .setSubText("Click to view your schedule")
                .setContentIntent(notificationPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_scheduled_item_reminder_foreground)
                .setAutoCancel(true)
                .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/raw/slow_spring_board"));

        return builder;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(PRIMARY_CHANNEL_ID, "Scheduled Item Alert Channel", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminds the user to start scheduled items.");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}