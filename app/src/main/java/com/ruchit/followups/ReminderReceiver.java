package com.ruchit.followups;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Shows the follow-up notification when a reminder's minute arrives. */
public class ReminderReceiver extends BroadcastReceiver {

    static final String EXTRA_ID = "clientId";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TEXT = "text";
    static final String CHANNEL = "followup_reminders";

    @Override
    public void onReceive(Context c, Intent in) {
        String id = in.getStringExtra(EXTRA_ID);
        if (id == null) return;
        String title = in.getStringExtra(EXTRA_TITLE);
        String text = in.getStringExtra(EXTRA_TEXT);
        if (title == null || title.isEmpty()) title = "Follow-up reminder";
        if (text == null || text.isEmpty()) text = "Time to follow up";

        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Follow-up reminders", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Alerts at the time you set for a client");
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);

        Intent open = new Intent(c, MainActivity.class);
        open.setAction("com.ruchit.followups.OPEN." + id);
        open.putExtra(EXTRA_ID, id);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(c, id.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_bell)
                .setColor(0xFF2F4BFF)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .build();
        try {
            nm.notify(id.hashCode(), n);
        } catch (SecurityException ignored) { }

        Reminders.fired(c, id);
    }
}
