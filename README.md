# Shorts Blocker (Android)

A project for building app-specific native patches that remove Shorts/Reels UI from modified builds of YouTube, Instagram, and Facebook. The selected C architecture requires modified app builds; it does not patch untouched official apps.

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for the selected Custom Native Patch Architecture (C), including APK inspection, version fingerprints, app-specific patch packs, signing, installation choices, and validation.

## Current repository status

The repository currently contains the earlier v0.4.0 AccessibilityService/overlay prototype. The native patch manager and YouTube/Instagram/Facebook patch bundles are not implemented yet. The v0.4.0 APK is not a native patched-app build.

## Important limitations

Native patching requires exact app-version compatibility. A modified APK signed with a different key normally cannot update the official installation in place. A separate clone may break sign-in, notifications, deep links, or integrity checks. The patcher must fail closed on unsupported versions and must never package or redistribute proprietary original APKs.

## Build

The current Android app can still be built with Android Studio, but it is the old overlay prototype. The new native patch pipeline is a design plan and is not yet buildable as a feature.

## Setup on device

Do not install the v0.4.0 prototype expecting native patching. Native patch builds will be provided only after app-version compatibility and on-device regression tests pass.

## Privacy and policy

Accessibility access is powerful. The app discloses its purpose and asks the user to review the disclosure before opening system settings. Do not use it to collect credentials, read private messages, or transmit screen content. Before publishing, review current Google Play Accessibility API and User Data policies. Play Store approval is not guaranteed.

## License

No license has been added yet. Add a license before redistributing this project.
