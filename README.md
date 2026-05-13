# Cobrow

Cobrow is a native Android WebView browser focused on fast local browsing, bundled ad blocking, and practical privacy controls. It stores browsing data on the device and does not include analytics or tracking SDKs.

## Features

- Tabbed browsing with a responsive grid overview, tab thumbnails, drag-to-reorder, swipe-to-close, clone actions, and optional tab groups.
- Built-in ad and tracker blocking using bundled EasyList, EasyPrivacy, Peter Lowe, uBlock filters, uBlock badware, and cookie notice filters.
- Privacy dashboard with session and lifetime blocked-request counts plus an estimated data-saved total.
- Local bookmarks, bookmark folders, browsing history, downloads, and saved credentials using Room.
- Incognito mode with cookies disabled, cache bypassed, and history saving skipped.
- Custom `cobrow://newtab` start page bundled in app assets.
- Search suggestions from local history/bookmarks and selectable search engines.
- Reader mode with configurable serif, sans-serif, or monospace font.
- Find in page, text size controls, page info, view source, desktop mode, share, screenshot, save page as MHTML, and print/PDF export.
- Backup and restore for bookmarks and history through JSON import/export.
- App shortcuts for New Tab, New Incognito, and Bookmarks.

## Tech Stack

- Java 17
- Android Gradle Plugin 8.2.2
- Gradle 8.2 wrapper
- AndroidX AppCompat, Material Components, ConstraintLayout, SwipeRefreshLayout, WebKit
- Room persistence library
- Android WebView

## Requirements

- Android Studio with Android SDK 34 installed
- JDK 17
- An Android device or emulator running Android 5.0 or newer

## Build

Clone the repository and build with the Gradle wrapper:

```bash
./gradlew assembleDebug
```

Install the debug build on a connected device:

```bash
./gradlew installDebug
```

The generated debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```text
app/src/main/java/com/cobrow/browser/
  activities/   UI screens and browser actions
  adapters/     RecyclerView and URL suggestion adapters
  data/         Room entities, DAOs, and tab persistence
  engine/       Ad blocker and credential manager
  utils/        URL normalization helpers

app/src/main/assets/
  new_tab.html  Built-in new tab page
  filters/      Bundled ad-block filter lists
```

## Privacy

Cobrow is designed to keep user data local:

- History, bookmarks, downloads, tabs, and credentials are stored on the device.
- Third-party cookies are disabled by default in the browser WebView.
- Ad-block filters are bundled with the app.
- No analytics SDKs are included.

Saved credentials are stored in the app database. Treat debug builds and local device data accordingly.

## Notes

The repository intentionally keeps generated or local agent/tool files out of source control. Files such as `.agents/`, `.gemini/`, `package-lock.json`, and `skills-lock.json` are not required to build the Android app.
