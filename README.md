# Shorts Blocker (Android)

An Android accessibility-based prototype that attempts to hide explicitly labelled YouTube Shorts, Instagram Reels, and Facebook Reels UI elements in the official apps. It does not modify or repackage those apps.

## Current scope

- Kotlin native Android app (min SDK 26).
- User-controlled master switch and per-app switches.
- Accessibility service scans visible UI nodes in the three supported apps.
- A touch-blocking accessibility overlay covers detected Shorts/Reels labels or candidate feed containers.
- Ordinary video content is not intentionally filtered unless the app exposes an explicit Shorts/Reels label.

## Important limitations

This is a best-effort prototype, not a guaranteed feed-cleaning solution. Instagram and Facebook do not consistently expose enough accessibility metadata to distinguish every Reel from every ordinary video. UI changes, language, app versions, A/B tests, dark themes, and custom layouts can cause missed detections or false positives. Overlay masking may leave blank areas; it does not remove elements from the host app's layout or source code.

The service only runs when the user explicitly enables it in Android Accessibility settings. No content is uploaded or transmitted by this prototype.

## Build

Open this repository in Android Studio (Gradle 8.9+ / Android Gradle Plugin 8.7.3), allow Gradle sync, then run the `app` configuration on an Android device. Or run:

```bash
gradle :app:assembleDebug
```

## Setup on device

1. Install and open the app.
2. Turn on the apps you want to filter.
3. Tap **Open Accessibility Settings** and enable **Shorts Blocker**.
4. Return to the app. The blocker runs only while the master switch is enabled.

## Privacy and policy

Accessibility access is powerful. The app must clearly disclose its purpose and obtain the user's explicit consent before enabling the service. Do not use it to collect credentials, read private messages, or transmit screen content. Before publishing, review current Google Play Accessibility API and User Data policies. Play Store approval is not guaranteed.

## License

No license has been added yet. Add a license before redistributing this project.
