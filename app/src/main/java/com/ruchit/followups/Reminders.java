package com.ruchit.followups;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

/** Keeps one alarm per client, at the exact reminder minute. */
final class Reminders {

    private static final String PREFS = "reminders";
    private static final String KEY = "all";

    private Reminders() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** json: [{id, at (epoch ms), title, text}] — the full list of future reminders. */
    static synchronized void sync(Context c, String json) {
        try {
            JSONArray arr = new JSONArray(json);
            JSONObject old = new JSONObject(prefs(c).getString(KEY, "{}"));
            JSONObject next = new JSONObject();
            long now = System.currentTimeMillis();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.optLong("at", 0) > now && o.has("id")) next.put(o.getString("id"), o);
            }
            Iterator<String> oldIds = old.keys();
            while (oldIds.hasNext()) {
                String id = oldIds.next();
                if (!next.has(id)) cancel(c, id);
            }
            Iterator<String> ids = next.keys();
            while (ids.hasNext()) {
                String id = ids.next();
                JSONObject o = next.getJSONObject(id);
                JSONObject prev = old.optJSONObject(id);
                if (prev == null || !prev.toString().equals(o.toString())) schedule(c, o);
            }
            prefs(c).edit().putString(KEY, next.toString()).apply();
        } catch (Exception ignored) { }
    }

    /** After a reboot, update or clock change: set every future alarm again. */
    static synchronized void rescheduleAll(Context c) {
        try {
            JSONObject all = new JSONObject(prefs(c).getString(KEY, "{}"));
            JSONObject keep = new JSONObject();
            long now = System.currentTimeMillis();
            Iterator<String> ids = all.keys();
            while (ids.hasNext()) {
                String id = ids.next();
                JSONObject o = all.getJSONObject(id);
                if (o.optLong("at", 0) > now) {
                    schedule(c, o);
                    keep.put(id, o);
                }
            }
            prefs(c).edit().putString(KEY, keep.toString()).apply();
        } catch (Exception ignored) { }
    }

    static synchronized void fired(Context c, String id) {
        try {
            JSONObject all = new JSONObject(prefs(c).getString(KEY, "{}"));
            all.remove(id);
            prefs(c).edit().putString(KEY, all.toString()).apply();
        } catch (Exception ignored) { }
    }

    private static PendingIntent intentFor(Context c, String id, JSONObject o) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction("com.ruchit.followups.REMIND." + id);
        i.putExtra(ReminderReceiver.EXTRA_ID, id);
        if (o != null) {
            i.putExtra(ReminderReceiver.EXTRA_TITLE, o.optString("title", "Follow-up reminder"));
            i.putExtra(ReminderReceiver.EXTRA_TEXT, o.optString("text", ""));
        }
        return PendingIntent.getBroadcast(c, id.hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void schedule(Context c, JSONObject o) throws Exception {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        String id = o.getString("id");
        long at = o.getLong("at");
        PendingIntent pi = intentFor(c, id, o);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    private static void cancel(Context c, String id) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(intentFor(c, id, null));
    }
}
