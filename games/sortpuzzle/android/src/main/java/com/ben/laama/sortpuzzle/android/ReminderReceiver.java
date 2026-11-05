package com.ben.laama.sortpuzzle.android;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.ben.laama.sortpuzzle.R;

/** Fired by a reminder alarm: posts the notification, which opens today's puzzle when tapped. */
public final class ReminderReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 4900;

    @SuppressLint("MissingPermission") // checked by canPost() just above
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!AndroidReminders.canPost(context)) return;
        String title = intent.getStringExtra(AndroidReminders.EXTRA_TITLE);
        String body = intent.getStringExtra(AndroidReminders.EXTRA_BODY);
        if (title == null || body == null) return;
        AndroidReminders.ensureChannel(context);

        Intent open = new Intent(context, AndroidLauncher.class)
            .putExtra(AndroidReminders.EXTRA_OPEN_DAILY, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap = PendingIntent.getActivity(context, NOTIFICATION_ID, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
            new NotificationCompat.Builder(context, AndroidReminders.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_reminder)
                .setColor(ContextCompat.getColor(context, R.color.reminder_accent))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        // One id: a new reminder replaces an unread older one instead of stacking up.
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }
}
