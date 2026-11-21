# Shorts Blocker (Android)

A native Android accessibility-based prototype that attempts to hide explicitly labelled YouTube Shorts, Instagram Reels, and Facebook Reels UI elements in the official apps. It does not modify or repackage those apps.

## Current scope (v0.2.0)

- Kotlin native Android app (min SDK 26).
- User-controlled master switch and per-app switches.
- Accessibility service scans visible UI nodes in YouTube, Instagram, and Facebook.
- English and selected Bangla/Hindi Shorts/Reels labels are recognized.
- When an explicitly labelled Shorts/Reels bottom tab is reported as selected, the app covers the feed area while leaving the bottom navigation area available.
- A touch-blocking accessibility overlay covers matching labels or candidate feed containers.
- Unit tests cover supported package names and label matching.

## Important limitations

This is a best-effort prototype, not a guaranteed feed-cleaning solution. The social apps may not expose their feed type or selected tab consistently through Android accessibility metadata. UI changes, language, app versions, A/B tests, themes, and custom layouts can cause missed detections or false positives. Overlay masking may leave blank areas; it does not remove elements from the host app's layout or source code. The selected-tab feed mask is heuristic and needs real-device testing.

The service only runs when the user explicitly enables it in Android Accessibility settings. Matching is performed on-device. This prototype does not store or transmit screen content and does not request internet access.

## Build

Open this repository in Android Studio, allow Gradle sync, then run the app configuration on an Android device. Or run:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

## Setup on device

1. Install the APK and open the app.
2. Turn on the apps you want to filter.
3. Tap **Open Accessibility Settings** and review the disclosure.
4. Enable **Shorts Blocker** in Android Accessibility settings.
5. Return to the app. The blocker runs only while the master switch is enabled.

## Privacy and policy

Accessibility access is powerful. The app discloses its purpose and asks the user to review the disclosure before opening system settings. Do not use it to collect credentials, read private messages, or transmit screen content. Before publishing, review current Google Play Accessibility API and User Data policies. Play Store approval is not guaranteed.

## License

No license has been added yet. Add a license before redistributing this project.
