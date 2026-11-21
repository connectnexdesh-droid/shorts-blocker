# Shorts Blocker — Custom Native Patch Architecture (C)

## Product direction

The selected direction is **C: modified native applications with app-specific patches**.

Constraints and consequences:
- No root, Magisk, LSPosed, Xposed, or runtime overlay.
- Shorts Blocker is a patch manager and custom patch-pack project, not a tool that edits the live private UI of an untouched app.
- The output apps are modified builds. They are not the unchanged official apps.
- Do not distribute copyrighted original APKs. The user supplies APKs obtained from their own device or another lawful source.
- Never collect or transmit app credentials, session cookies, private messages, or personal app data.

## Critical Android signing constraint

A modified APK cannot normally update an installed official app if it is signed with a different certificate. Android's package manager requires the signing certificate to match for an in-place update. Therefore the supported installation choices are:

1. **Separate patched clone:** change the package ID only when the app and its dependencies support it. This may break OAuth, Google/Facebook sign-in, notifications, deep links, integrity checks, or push services.
2. **Replace the official installation:** uninstall the official app, then install the modified build. This can remove local app data and requires a backup/account sync first.

The patch manager must explain this before patching and must never silently uninstall the official app.

## Architecture

```text
Shorts Blocker Manager (Android UI)
       |
       +--> APK / split-APK intake (user selected files)
       |
       +--> Package inspector
       |      - package ID
       |      - version name / version code
       |      - min/target SDK
       |      - split APK set
       |      - signing certificate fingerprint
       |
       +--> Compatibility registry
       |      - supported app versions
       |      - required base/split set
       |      - patch fingerprints
       |
       +--> Patch orchestration layer
       |      - ReVanced Patcher-compatible patch runtime / CLI adapter
       |      - Shorts Blocker custom patch bundle
       |      - deterministic patch selection
       |
       +--> App-specific patch packs
       |      - YouTube Shorts Pack
       |      - Instagram Reels Pack
       |      - Facebook Reels Pack
       |
       +--> Rebuild, align, sign, verify
       |
       +--> Installation hand-off (explicit user confirmation)
```

## Why build on a proven patcher framework?

Do not write a DEX parser, smali assembler, resource compiler, APK signer, and ZIP rewriter from scratch. Use a maintained patching framework as the binary transformation layer and build our own patch definitions, compatibility checks, UI, tests, and release workflow around it.

ReVanced CLI is a command-line application built on ReVanced Patcher and documents patch-bundle selection, package/version checks, patching, and ADB installation. The first implementation should integrate or invoke this toolchain in a reproducible build environment rather than pretend an ordinary Android app can safely patch every installed app in-place.

Official CLI documentation:
- https://github.com/ReVanced/revanced-cli
- https://github.com/ReVanced/revanced-cli/blob/main/docs/1_usage.md

## App-specific patch plan

### Phase 1 — YouTube (first proof of concept)

Target package: `com.google.android.youtube`.

Patch objectives:
1. Hide the Shorts navigation control.
2. Remove Shorts shelves/cards from Home and other selected feeds.
3. Disable entry into the Shorts player through Shorts-specific routes.
4. Preserve standard video watch pages, subscriptions, search, channels, playlists, and ordinary video cards.

Existing community patch ecosystems already document a YouTube Shorts navigation-button patch and Shorts-related component patches. We should first verify the exact patch names and compatibility for the user's installed YouTube version. Reuse compatible patch logic where its license permits; write a Shorts Blocker-specific patch only for gaps. Do not claim that a patch hides the Home shelf until tested on the target version.

### Phase 2 — Instagram

Target package: `com.instagram.android`.

Patch objectives:
1. Hide the Reels navigation destination.
2. Remove Reels-only surfaces from Home/Explore where they can be reliably distinguished.
3. Block dedicated Reels player entry points.
4. Preserve photo posts, Stories, DMs, profile pages, and ordinary feed video.

No Instagram patch is assumed to exist. First perform static analysis of a user-supplied target APK, map the relevant UI/navigation components, create version-specific fingerprints, and write instrumentation tests. If the app's obfuscation or integrity checks make reliable patching impractical, mark that feature unsupported rather than ship a broken APK.

### Phase 3 — Facebook

Target package: `com.facebook.katana`.

Patch objectives:
1. Hide Reels-specific navigation entry points.
2. Remove Reels-only cards/shelves.
3. Block the dedicated Reels viewer.
4. Preserve ordinary video posts, photos, groups, pages, Marketplace, and generic Watch/video surfaces unless proven to be Reels.

Facebook has multiple surfaces and frequent server-driven UI changes. Each target must be distinguished from generic video UI. Do not hide a generic Watch or Videos entry as a shortcut.

## Patch-engine modules

| Module | Responsibility |
|---|---|
| `patcher/engine` | Adapter around the selected patch framework |
| `patcher/apk-inspector` | Package/version/splits/signing metadata |
| `patcher/compatibility` | Exact supported fingerprints and fail-closed checks |
| `patches/youtube` | YouTube Shorts patch definitions |
| `patches/instagram` | Instagram Reels patch definitions |
| `patches/facebook` | Facebook Reels patch definitions |
| `patcher/rebuilder` | Rebuild, align, sign, verify |
| `patcher/install` | Explicit install/replace/clone workflow |
| `manager-ui` | APK selection, patch options, progress, warnings |
| `tests/fixtures` | User-provided test fixtures and version-specific regression cases |

## Compatibility and safety rules

- Patch only exact package/version fingerprints that have been tested.
- Fail closed on unknown versions; never guess method signatures or resource IDs.
- Verify that ordinary video entry points still work before marking a build compatible.
- Keep a backup/export path for user data and clearly warn before uninstalling an official app.
- Never ask users to share passwords, session tokens, or account databases.
- Do not bundle or redistribute original proprietary APK files.
- Do not bypass paid access, age gates, account protections, or security checks.
- Store signing keys securely; do not commit private signing keys to GitHub.

## Validation matrix

| App | Must remove/block | Must preserve |
|---|---|---|
| YouTube | Shorts button, Shorts shelf, Shorts player | Standard videos, search, subscriptions, channels |
| Instagram | Reels tab, Reels surfaces, Reels player | Photos, Stories, DMs, normal feed videos |
| Facebook | Reels tab/cards/player | Posts, photos, groups, pages, ordinary videos |

Every app/version needs:
- Static patch verification.
- Install and launch test.
- Home-feed regression test.
- Standard-video playback test.
- Shorts/Reels entry test.
- Sign-in and notification smoke test where clone mode is used.

## Delivery sequence

1. Collect exact app package/version/split information from the user's phone.
2. Build a reproducible patching pipeline for a user-supplied YouTube APK.
3. Verify the Shorts navigation patch and investigate Home shelf/player patches.
4. Produce one test build and validate it on the user's device.
5. Only after YouTube passes, start Instagram reverse engineering.
6. Start Facebook after Instagram has a stable test harness.
7. Add an update checker for supported app-version fingerprints and patch releases.

## Current implementation status

The current repository's v0.4.0 Android app is still the earlier AccessibilityService/overlay prototype. This document defines the newly selected C direction; the native patch manager and app-specific patch bundles have **not** yet been implemented. Do not install or advertise the v0.4.0 prototype as a native patched-app solution.
