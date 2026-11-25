package app.followups;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class Reminders {
    private static final String PREFS = "reminders";
    private static final String ACTION = "app.followups.REMIND.";

    private Reminders() {
    }

    /** Replace the whole set of scheduled reminders with the list the app sends. */
    static synchronized void sync(Context c, String json) {
        JSONArray arr;
        try {
            arr = new JSONArray(json);
        } catch (JSONException e) {
            return;
        }
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> previous = new HashSet<>(p.getAll().keySet());
        SharedPreferences.Editor e = p.edit();
        e.clear();
        long now = System.currentTimeMillis();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String id = o.optString("id", "");
            long at = o.optLong("at", 0);
            if (id.isEmpty() || at <= now) continue;
            e.putString(id, o.toString());
            schedule(c, id, at, o.optString("title", "Follow up"), o.optString("body", ""));
            previous.remove(id);
        }
        e.apply();
        for (String id : previous) cancel(c, id);
    }

    static synchronized void rescheduleAll(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, ?> entry : p.getAll().entrySet()) {
            try {
                JSONObject o = new JSONObject(String.valueOf(entry.getValue()));
                long at = o.optLong("at", 0);
                if (at <= now) {
                    e.remove(entry.getKey());
                    continue;
                }
                schedule(c, entry.getKey(), at, o.optString("title", "Follow up"), o.optString("body", ""));
            } catch (JSONException ex) {
                e.remove(entry.getKey());
            }
        }
        e.apply();
    }

    static synchronized void markFired(Context c, String id) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(id).apply();
    }

    private static Intent intentFor(Context c, String id) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction(ACTION + id);
        i.putExtra("id", id);
        return i;
    }

    private static void schedule(Context c, String id, long at, String title, String body) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        Intent i = intentFor(c, id);
        i.putExtra("title", title);
        i.putExtra("body", body);
        PendingIntent pi = PendingIntent.getBroadcast(c, id.hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException se) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    private static void cancel(Context c, String id) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, id.hashCode(), intentFor(c, id),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            if (am != null) am.cancel(pi);
            pi.cancel();
        }
    }
}
