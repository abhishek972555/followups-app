package app.followups;

import android.app.NotificationManager;
import android.webkit.JavascriptInterface;

public class Bridge {
    private final MainActivity activity;

    Bridge(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void syncReminders(String json) {
        Reminders.sync(activity.getApplicationContext(), json);
    }

    @JavascriptInterface
    public void askNotifications() {
        activity.runOnUiThread(activity::requestNotifications);
    }

    @JavascriptInterface
    public boolean notificationsAllowed() {
        NotificationManager nm = activity.getSystemService(NotificationManager.class);
        return nm != null && nm.areNotificationsEnabled();
    }

    @JavascriptInterface
    public void openNotificationSettings() {
        activity.runOnUiThread(activity::openNotificationSettings);
    }

    @JavascriptInterface
    public boolean saveFile(String name, String mime, String content) {
        return FileSaver.saveToDownloads(activity.getApplicationContext(), name, mime, content);
    }
}
