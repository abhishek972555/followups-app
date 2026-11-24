# Follow-ups (Android)

A small Android app for tracking clients and leads: client details, numbered follow-ups, and a reminder that notifies you at the exact time you set.

## Getting the app
Every push to `main` builds the app on GitHub. Open **Releases** on your phone, download the newest `FollowUps.apk`, and open it. If Android asks, allow installs from your browser or the GitHub app.

Updates install over the old version and keep your data, because every build is signed with the same key (`app/followups.keystore`). Keep this repository **private**: it holds that key and your starting client list.

## Where data lives
Only on the phone, inside the app. Use the download icon (top right) → **Save backup** now and then. **Restore** brings a backup back on a new phone.

## Reminders
Set a date and time on a client. The app schedules an exact alarm and shows a notification at that minute, even after a restart. Allow notifications when Android asks.
