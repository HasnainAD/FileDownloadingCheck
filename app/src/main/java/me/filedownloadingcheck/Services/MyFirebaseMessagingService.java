package me.filedownloadingcheck.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import me.filedownloadingcheck.R;
import me.filedownloadingcheck.activities.ShareActivity;

/**
 * Created by Abdullah on 11/19/2017.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {


    static int notify_id = 0;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage.getData().size() > 0) {
            Log.e("OnMessageReceived","Where am I? in IF");

            Map<String , String> payload = remoteMessage.getData();

            showNotification(payload);
        }



    }



    private void showNotification(Map<String, String> payload) {


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentTitle(payload.get("title"));
        builder.setContentText(payload.get("body"));
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);


        Intent resultIntent = new Intent(this, ShareActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(notify_id, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notify_id++, builder.build());


    }
}
