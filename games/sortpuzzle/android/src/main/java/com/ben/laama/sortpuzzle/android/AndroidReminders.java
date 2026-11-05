package com.ben.laama.sortpuzzle.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.badlogic.gdx.Gdx;

import com.ben.laama.sortpuzzle.BuildConfig;
import com.ben.laama.sortpuzzle.R;
import com.ben.laama.sortpuzzle.notify.Reminder;
import com.ben.laama.sortpuzzle.notify.Reminders;

import java.util.ArrayList;
import java.util.List;

/**
 * Local daily-puzzle reminders on Android: inexact, Doze-friendly alarms
 * ({@link AlarmManager#setAndAllowWhileIdle}, so no exact-alarm permission is needed) that fire
 * {@link ReminderReceiver}, which posts the notification on the "reminders" channel.
 *
 * <p>The plan is also written to private preferences so {@link BootReceiver} can put it back
 * after a reboot (alarms do not survive one). Nothing leaves the device.</p>
 */
public final class AndroidReminders implements Reminders {

    static final String CHANNEL_ID = "reminders";
    static final String EXTRA_SLOT = "slot";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_BODY = "body";
    /** Set on the launch intent of a tapped reminder. */
    static final String EXTRA_OPEN_DAILY = "open_daily";
    static final int PERMISSION_REQUEST = 4701;

    private static final String PREFS = "tidy_reminders";
    private static final String KEY_PLAN = "plan";
    private static final char FIELD = '\u001F';
    private static final char RECORD = '\u001E';
    private static final int REQUEST_BASE = 4800;

    private final Activity activity;
    private volatile boolean openDailyRequested;
    private PermissionCallback pendingCallback;

    public AndroidReminders(Activity activity) {
        this.activity = activity;
        ensureChannel(activity);
    }

    // ------------------------------------------------------------- Reminders
    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public boolean hasPermission() {
        return canPost(activity);
    }

    @Override
    public void requestPermission(PermissionCallback callback) {
        if (Build.VERSION.SDK_INT < 33 || hasPermission()) {
            callback.onResult(hasPermission());
            return;
        }
        pendingCallback = callback;
        activity.runOnUiThread(() -> ActivityCompat.requestPermissions(activity,
            new String[] {Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST));
    }

    /** Forwarded from {@code AndroidLauncher.onRequestPermissionsResult}. */
    void onPermissionResult(boolean granted) {
        PermissionCallback callback = pendingCallback;
        pendingCallback = null;
        if (callback != null) {
            Gdx.app.postRunnable(() -> callback.onResult(granted));
        }
    }

    @Override
    public void openSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= 26) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.getPackageName(), null));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.runOnUiThread(() -> activity.startActivity(intent));
    }

    @Override
    public void schedule(List<Reminder> reminders) {
        cancelAlarms(activity);
        StringBuilder plan = new StringBuilder();
        for (Reminder r : reminders) {
            arm(activity, r.slot, r.atMillis, r.title, r.body);
            plan.append(r.slot).append(FIELD).append(r.atMillis).append(FIELD)
                .append(r.title).append(FIELD).append(r.body).append(RECORD);
        }
        prefs(activity).edit().putString(KEY_PLAN, plan.toString()).apply();
    }

    @Override
    public void cancelAll() {
        cancelAlarms(activity);
        prefs(activity).edit().remove(KEY_PLAN).apply();
        NotificationManagerCompat.from(activity).cancelAll();
    }

    @Override
    public boolean consumeOpenDailyRequest() {
        boolean requested = openDailyRequested;
        openDailyRequested = false;
        return requested;
    }

    /** Debug builds only, so a release can never show a test notification. */
    @Override
    public boolean supportsTest() {
        return BuildConfig.DEBUG;
    }

    /**
     * Fires the receiver directly after the delay, bypassing AlarmManager: inexact alarms may be
     * held back up to an hour and {@link #schedule} would cancel a test slot when the game is
     * left, so a test through the alarm would prove nothing. This proves what the player can
     * check: permission, channel, the notification itself and the tap that opens the puzzle.
     */
    @Override
    public void scheduleTest(int seconds) {
        if (!supportsTest()) return;
        Intent intent = new Intent(activity, ReminderReceiver.class)
            .putExtra(EXTRA_SLOT, MAX_SCHEDULED)
            .putExtra(EXTRA_TITLE, "Test reminder")
            .putExtra(EXTRA_BODY, "If you can read this, reminders work on this phone.");
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            () -> activity.sendBroadcast(intent), Math.max(3, seconds) * 1000L);
    }

    /** Called by the launcher for the intent that started or re-opened the activity. */
    void handleLaunchIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_OPEN_DAILY, false)) {
            openDailyRequested = true;
            intent.removeExtra(EXTRA_OPEN_DAILY); // a rotation or recreate must not reopen it
        }
    }

    // ---------------------------------------------------------- static helpers
    static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
            context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        manager.createNotificationChannel(channel);
    }

    /** Re-arms the saved plan after a reboot, skipping what is already in the past. */
    static void restore(Context context) {
        String plan = prefs(context).getString(KEY_PLAN, "");
        long now = System.currentTimeMillis();
        for (String record : split(plan, RECORD)) {
            List<String> f = split(record, FIELD);
            if (f.size() != 4) continue;
            try {
                long at = Long.parseLong(f.get(1));
                if (at > now) {
                    arm(context, Integer.parseInt(f.get(0)), at, f.get(2), f.get(3));
                }
            } catch (NumberFormatException ignored) {
                // a corrupt record is just skipped
            }
        }
    }

    private static void arm(Context context, int slot, long atMillis, String title, String body) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) return;
        Intent intent = new Intent(context, ReminderReceiver.class)
            .putExtra(EXTRA_SLOT, slot).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_BODY, body);
        PendingIntent pending = PendingIntent.getBroadcast(context, REQUEST_BASE + slot, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (slot == 0) {
            // The one-minute nudge: a tight window (Android 12+ widens anything under 10 min to
            // 10 min). The phone was just in use, so Doze is not a concern yet.
            alarms.setWindow(AlarmManager.RTC_WAKEUP, atMillis, FIRST_NUDGE_WINDOW_MS, pending);
        } else {
            // Hours away: inexact (about an hour of slack) but delivered even in Doze.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending);
        }
    }

    private static final long FIRST_NUDGE_WINDOW_MS = 5L * 60L * 1000L;

    private static void cancelAlarms(Context context) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) return;
        for (int slot = 0; slot < MAX_SCHEDULED; slot++) {
            PendingIntent pending = PendingIntent.getBroadcast(context, REQUEST_BASE + slot,
                new Intent(context, ReminderReceiver.class),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pending != null) {
                alarms.cancel(pending);
                pending.cancel();
            }
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static List<String> split(String text, char separator) {
        List<String> parts = new ArrayList<>();
        if (text == null || text.isEmpty()) return parts;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == separator) {
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }
        if (start < text.length()) parts.add(text.substring(start));
        return parts;
    }
}
