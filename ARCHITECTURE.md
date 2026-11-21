# Shorts Blocker — Advanced Rootless Architecture

## Product constraints

- No root, Magisk, Xposed, LSPosed, VPN, or modified social-app APKs.
- Keep the official YouTube, Instagram, and Facebook apps installed and unchanged.
- Preserve ordinary posts, photos, Stories, and standard video playback wherever they are not identified as Shorts/Reels.
- All screen analysis stays on-device. Raw screenshots are transient and are not uploaded or retained by default.

## Important platform limitation

A normal Android app cannot directly delete or re-layout private UI views inside another app. Without root, app modification, or vendor support, Shorts Blocker cannot truly remove a host app's internal view. The supported rootless approach is to identify the exact unwanted region, draw an accessibility overlay over it, and consume touches in that region. The host app may therefore retain blank space beneath a mask. Do not describe masking as native UI removal.

## Recommended architecture

```text
Official social app (unchanged)
        |
        v
AccessibilityService event monitor
        |
        +--> Accessibility tree snapshot
        |      - package name
        |      - view ID / text / content description
        |      - clickable, selected, checked state
        |      - exact screen bounds
        |
        +--> Screenshot provider (Android API 30+)
        |      - AccessibilityService.takeScreenshot()
        |      - API 34+: takeScreenshotOfWindow() where useful
        |      - event-driven, debounced, rate-limited
        |
        v
On-device perception pipeline
        |
        +--> OCR (ML Kit bundled model where offline-first is required)
        +--> app-specific icon / badge template matching
        +--> screen-layout classifier (later phase; requires test samples)
        |
        v
App-specific decision engine
        - YouTube Shorts shelf / Shorts tab / Shorts player
        - Instagram Reels tab / Reels cards / Reels player
        - Facebook Reels entry / Reels cards / Reels player
        |
        v
Confidence + safety gate
        |
        +--> high confidence: exact target rectangle
        +--> uncertain: do not mask; record local diagnostic
        |
        v
Precision overlay controller
        - small exact rectangles only
        - touch interception only on blocked controls/cards
        - full-screen block only on a positively identified Shorts/Reels player
        - never mask a large parent container based only on a text match
```

## Android implementation choices

### 1. Accessibility event monitor

Keep one AccessibilityService scoped to the three supported package names. Trigger scans on window-state, window-content, scroll, and window changes. Debounce repeated events. Read the active window and its package before doing any work. Never clear or apply masks based only on a stale event package.

### 2. Screenshot provider

For Android 11 / API 30 and later, use `AccessibilityService.takeScreenshot()`. Declare `android:canTakeScreenshot="true"` in the accessibility-service XML metadata. On Android 14 / API 34 and later, use `takeScreenshotOfWindow()` when a window-specific capture is useful.

Prefer this AccessibilityService screenshot API over MediaProjection for this product: it avoids building a separate screen-capture foreground-service flow and its per-session MediaProjection consent lifecycle. Screenshots must still be handled as sensitive data.

For API 26–29, fall back to accessibility-tree-only detection. Show a clear capability status in the app; do not silently claim visual detection is active.

Capture only after relevant UI events, not at video frame rate. Debounce, enforce a minimum interval, and discard stale results when the active package/window changes.

### 3. OCR and visual matching

- Use on-device ML Kit Text Recognition for visible labels such as “Shorts”, “Reels”, and “Watch Reels”.
- Prefer a bundled OCR model when first-run offline availability is important; a Play-services model can reduce APK size but may need to download before use.
- ML Kit's documented Text Recognition v2 script support does not include Bengali. If Bengali-language UI support is required, add a separately evaluated Bengali OCR engine/model; do not claim Bengali OCR based on Latin/Devanagari support.
- Use app-version-aware icon templates for unlabeled navigation icons.
- Add a trained on-device classifier only after collecting a consented, local test set across app versions, screen sizes, themes, and languages. A vertical aspect ratio alone is never enough to classify a video as a Reel/Short.

### 4. App-specific rules

#### YouTube

- Hide the Shorts navigation control only when its accessibility label/view ID or visual template is confidently identified.
- Hide a Home Shorts shelf only when a Shorts heading/anchor and its associated shelf/card layout are both identified.
- Keep ordinary video cards and the normal video player.
- If the Shorts player screen is confidently identified, show a clear blocking mask/panel and consume taps in the player region; preserve a safe Back route.

#### Instagram

- Identify the Reels navigation icon using the accessibility node first, then an app-version-specific visual template.
- Mask only cards confidently labelled or classified as Reels.
- Do not classify every vertical video as a Reel. Preserve ordinary feed videos unless independent Reels evidence exists.
- If the dedicated Reels player is positively identified, block that screen with an explicit local overlay and a safe exit.

#### Facebook

- Distinguish Reels from generic Watch/Video destinations. Never hide a generic Videos or Watch control merely because it opens video content.
- Use Reels-specific labels, view IDs, badges, and screen-layout evidence.
- Preserve ordinary video posts in Home Feed unless Reels evidence is strong.
- If a dedicated Reels player is positively identified, block it and provide a safe Back route.

### 5. Confidence and overlay safety

Initial thresholds are engineering starting points and must be calibrated with device testing, not treated as measured accuracy:

- 0.90 or higher: allow a precise tab/icon mask when the target geometry is valid.
- 0.85 or higher: allow a card/shelf mask only when at least two independent signals agree (for example, a Reels label plus a matching card layout).
- Full-screen player blocking: require a dedicated screen signature, not one text token.
- Below threshold: do not mask. Record a compact local diagnostic instead.

Overlay rules:

- Never expand from a matched label to an arbitrary large ancestor.
- Validate bounds against display size and minimum/maximum region limits.
- Reject overlapping or implausibly large rectangles.
- Provide a debug mode that draws outlines and labels instead of opaque masks.
- Provide an emergency “disable blocker” action and always let the user disable the Accessibility Service in Android Settings.

## Suggested project modules

| Module | Responsibility |
|---|---|
| `service/BlockerAccessibilityService` | Events, active-window lifecycle, package routing |
| `capture/AccessibilityScreenshotProvider` | API 30+ screenshot capture and throttling |
| `snapshot/AccessibilityTreeReader` | Node text, IDs, state, bounds |
| `vision/OcrEngine` | On-device OCR |
| `vision/IconTemplateMatcher` | App-version-specific icon/badge matching |
| `vision/ScreenClassifier` | Later-stage screen/card classification |
| `rules/YouTubeRules` | Shorts-specific detection |
| `rules/InstagramRules` | Reels-specific detection |
| `rules/FacebookRules` | Reels-specific detection |
| `decision/ConfidenceGate` | Evidence fusion and false-positive prevention |
| `overlay/PrecisionOverlayController` | Exact masks and touch interception |
| `diagnostics/LocalDiagnostics` | Counts, labels, bounds, timing; no raw screenshots by default |

## Delivery plan

1. **Instrumentation first:** show active package, node count, screenshot capability, OCR text, candidate bounds, and decision reason in a local debug panel.
2. **Screenshot pipeline:** implement API 30+ AccessibilityService screenshots and verify that captures correspond to the active social-app window.
3. **OCR:** add bundled/on-device OCR, event-driven capture, and app-specific keyword anchors.
4. **Precision rules:** implement YouTube first; then Instagram; then Facebook. Add exact view-ID and icon-template rules per tested app version.
5. **Safe overlays:** replace broad parent masking with exact rectangles and conservative confidence gates.
6. **Device validation:** test Home, ordinary video, Shorts/Reels, search, profile, Stories, dark/light themes, and supported app versions. Require proof that normal video remains visible before enabling a rule by default.

## Test matrix

| App | Must block | Must preserve |
|---|---|---|
| YouTube | Shorts tab, Home Shorts shelf, Shorts player | Home feed, subscriptions, search, standard video player |
| Instagram | Reels tab, positively identified Reels cards/player | Photos, Stories, standard feed video |
| Facebook | Reels entry, positively identified Reels cards/player | Posts, photos, standard video posts, generic Watch/Video content unless identified as Reels |

## Privacy and Play policy

- Process frames locally and discard them after OCR/classification.
- Do not upload screenshots, OCR text, private messages, credentials, or unrelated screen content.
- Explain the Accessibility Service purpose prominently and obtain explicit user consent.
- Google Play treats Accessibility API usage as restricted; this app must not falsely designate itself as an accessibility tool. Review current Play policy before distribution.

## Current implementation status

The existing v0.4.0 repository is still a text/accessibility-tree plus heuristic-overlay prototype. The screenshot/OCR/classifier pipeline described above is the recommended next architecture; it is not yet implemented. Do not label an APK as using this architecture until the corresponding modules are committed, built, and tested on a real device.
