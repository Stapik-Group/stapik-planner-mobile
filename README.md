# Stapik Planner (Android)

A read-only mobile companion to [Stapik Planner](https://github.com/Stapik-Group/stapik-planner), the desktop weekly planner app. Written in Kotlin with Jetpack Compose, styled after the same retro old-school aesthetic as the desktop version and its sibling app.

![Screenshot](images/screenshot.png)

## Features

- **Looping daily view** – swipe horizontally between the 7 weekdays, endlessly looping (Sun → Mon → ... → Sat → Sun); the app always opens on today
- **Local task checkoff** – tick off today's completed slots; kept only on this device (unencrypted local storage), never synced to the cloud, and never affects the source plan on the desktop app
- **Day load bar** – shows how loaded each day is (activity difficulty adds up to a 0–100%+ load, same formula as the desktop app)
- **Cloud sync (read-only)** – fetches the weekly plan from the Stapik Cloud compatible self-hosted API
- **Offline cache** – automatically caches the last retrieved plan so you can view it without internet access
- **Themes** – Classic, Classic Pink and Modern, matching the desktop app
- **Pull-to-refresh** – drag down to fetch the latest data on demand
- **Encrypted connection config** – server URL and API key are encrypted with a key held in the Android Keystore before being stored on device
- **Multilingual UI** – Polish, English and German, following the phone's system language
- **Retro aesthetic** – same raised-button, blue-navbar, grey-cell look as the desktop app

This app does not edit the plan, its activity catalog, or slot configuration – all of that happens in the desktop app. It only reads and displays whatever is currently in the cloud, plus a small local-only "done today" overlay.

### What's intentionally not here

Unlike Stapik Calendar's companion app, this app has **no notifications** and **no home screen widget** — a deliberate scope decision for the planner companion.

## Requirements

- Android 8.0 (API 26) or newer
- A running instance of the Stapik Cloud compatible sync API used by the desktop app (see [Stapik Planner](https://github.com/Stapik-Group/stapik-planner))

## Building

Requires [Android Studio](https://developer.android.com/studio) (Kotlin, Jetpack Compose).

```
git clone https://github.com/Stapik-Group/stapik-planner-mobile
```

Open the project folder in Android Studio, let Gradle sync, then build/run via the green Run button or:

```
./gradlew assembleDebug
```

The resulting APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Installation

### Option 1 – install via Android Studio

Connect a device (USB or [wireless debugging](https://developer.android.com/tools/wireless-debugging)) or start an emulator, then hit Run in Android Studio.

### Option 2 – install a built APK manually

```
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or transfer the APK to the device and open it directly (requires allowing installs from the source you used).

## Cloud Sync

On first launch, open the settings menu (gear icon, bottom right) – **Connect**, and enter the same server URL and API key configured on the desktop app (**File – Connect** there).

The app fetches `planner.json` from the server on startup, whenever you return to the plan screen after connecting, and whenever you pull to refresh. The sync API is fully compatible with **Stapik Cloud**.

Since this app is read-only, there's no write conflict to worry about on the mobile side – all conflict resolution (last-write-wins by timestamp) happens between desktop instances, as described in the [desktop app's README](https://github.com/Stapik-Group/stapik-planner#cloud-sync).

## Local task checkoff

Ticking a checkbox next to one of today's slots only writes to an on-device DataStore, keyed by date and slot index. It is:

- **not** sent to the server,
- **not** part of the synced plan document,
- automatically irrelevant once the date rolls over (a new day starts with everything unchecked, no reset step needed),
- pruned lazily after a couple of days so the local store doesn't grow forever.

If you want the desktop plan itself to reflect completed tasks, that's out of scope for this app by design — edit it there.
