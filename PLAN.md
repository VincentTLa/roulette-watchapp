# Plan: Wear OS Roulette App for Samsung Galaxy Watch 8

## Context

You want a simple European-style roulette wheel app for the Samsung Galaxy Watch 8, which runs **Wear OS 6** (Android-based). The app is a visual-only roulette — no betting, no chips, no history. The user taps the wheel to spin, the wheel animates, and the result (number + color) is shown with the winning pocket highlighted. Fresh state every launch.

This is a from-scratch project (currently only contains `README.md`), so the plan covers project scaffolding, the single-screen UI, and the wheel/animation logic.

## Tooling & Setup

- **IDE:** Android Studio (Ladybug or newer recommended for Wear OS 6 support).
- **Project type:** Create a new project via *File → New → New Project → Wear OS → Empty Wear App (Compose Material)*.
- **Min SDK:** 30 (Wear OS 3 — covers older paired watches); **Target SDK:** 35 or 36 to match the Galaxy Watch 8's Wear OS 6 / Android 16 platform.
- **Language:** Kotlin.
- **UI:** Jetpack Compose for Wear OS using **Material 3** (`androidx.wear.compose:compose-material3`, `compose-foundation`, `compose-navigation`).
- **Pairing/testing:** You can run on a Wear OS emulator matched to the Galaxy Watch 8 — round, **438×438** for the 40mm model or **480×480** for the 44mm model — or pair the physical watch via Wi-Fi ADB.

## File Structure

A single-activity, single-screen Compose app:

```
app/
├── src/main/
│   ├── AndroidManifest.xml                    # declares Wear OS feature, MainActivity
│   ├── java/com/example/roulette/
│   │   ├── MainActivity.kt                    # ComponentActivity hosting Compose content
│   │   ├── ui/
│   │   │   ├── RouletteApp.kt                 # Top-level Scaffold + screen
│   │   │   ├── RouletteWheel.kt               # Canvas-drawn wheel composable (the wheel is the spin control)
│   │   │   └── theme/Theme.kt                 # Wear Material 3 colors
│   │   └── data/
│   │       └── EuropeanWheel.kt               # Pocket order, red/black sets
│   └── res/
│       └── values/strings.xml
└── build.gradle.kts                           # Wear Compose dependencies
```

## Implementation Notes

### 1. `EuropeanWheel.kt` — wheel data
- **Pocket order** (clockwise, starting at 0, this is the real European wheel sequence — do not just use 0..36 in numerical order):
  `0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26`
- **Red numbers:** `1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36`
- **Black numbers:** everything else from 1–36.
- **Green:** `0`.
- Expose helpers: `colorFor(number: Int): PocketColor` and the order list.

### 2. `RouletteWheel.kt` — wheel rendering & animation
- Use `Canvas` inside a `Box`. Draw 37 sectors with `drawArc` (sweep angle = 360/37 ≈ 9.73°), each filled with red/black/green. **Do not draw number labels on the wheel** — at 37 sectors on a ~1.4" display the labels are illegible. The wheel reads as a color pattern only; the actual landed number is communicated by the result label (see §3).
- A static **pointer/triangle** is drawn at the top (12 o'clock) using `drawPath`.
- Use a single `Animatable<Float>` for `rotationDegrees`. Apply `Modifier.graphicsLayer { rotationZ = rotation.value }` to the wheel.
- **Tap-to-spin:** the wheel composable owns the tap input via `Modifier.clickable { onSpin() }` (or `pointerInput { detectTapGestures }` if you want to suppress ripple). When `spinState` is `Spinning`, the tap is ignored — there is no separate spin button.
- **Spin animation:**
  - Pick a random target index 0..36.
  - Compute target rotation = `currentRotation + (several full turns, e.g. 5..8) * 360 + offsetForIndex(targetIndex)`.
  - Animate with `animateTo(target, tween(durationMillis = 3500, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))` — slow ease-out feels like real deceleration.
  - On completion, set result state and trigger the highlight animation.
- **Winning pocket highlight:** after the wheel stops, use an `infiniteRepeatable` `animateFloat` (or a 2–3-pulse `repeatable`) to oscillate the winning sector's brightness/stroke. Keep it subtle so it doesn't overwhelm the small screen.

### 3. `RouletteApp.kt` — screen composition
- Use `AppScaffold` / `ScreenScaffold` from `androidx.wear.compose.material3` (handles round-screen insets, time text at top).
- Layout:
  - Time text (provided by Scaffold) — accepted that it overlays the pointer area at 12 o'clock.
  - The `RouletteWheel` fills the screen and **is itself the spin control** (tap anywhere on the wheel to spin). No separate Spin button.
  - Result label overlaid in the center of the wheel showing e.g. "17 BLACK" / "0 GREEN" once the wheel stops. While spinning, show nothing (or a subtle "…" hint).
- A `spinState` (sealed class: `Idle`, `Spinning`, `Result(number)`) hoisted in `RouletteApp` gates tap input on the wheel and controls what the result label shows.

### 4. `MainActivity.kt`
- A minimal `ComponentActivity` whose `onCreate` calls `setContent { RouletteTheme { RouletteApp() } }`. Nothing else — no manual theme/window boilerplate; the modern Wear Compose template doesn't need it.

### 5. `AndroidManifest.xml`
- `<uses-feature android:name="android.hardware.type.watch" />`
- `<uses-library android:name="com.google.android.wearable" android:required="true" />`
- Standard `MainActivity` with `MAIN`/`LAUNCHER` intent filter.

## Verification

1. **Build:** `./gradlew :app:assembleDebug` from the project root — confirms Gradle + Wear Compose deps resolve.
2. **Run on emulator:**
   - In Android Studio, create a Wear OS AVD matched to the Galaxy Watch 8: round, **438×438** (40mm) or **480×480** (44mm), with a Wear OS 5 or 6 system image. (Watch 8 ships with Wear OS 6 / One UI Watch 8 / Android 16.)
   - Run the app and visually confirm:
     - Wheel renders fully on the round screen, not clipped.
     - The red/black/green pattern around the wheel is visually correct (no number labels — verification is by color pattern only).
     - Tapping anywhere on the wheel rotates it for ~3.5s with smooth deceleration.
     - Result label in the center shows the correct number + color matching the pocket under the top pointer.
     - Winning pocket highlight pulses briefly after landing.
     - Tapping the wheel while it's already spinning is a no-op.
3. **Run on Watch 8 (optional):** Enable developer mode → ADB debugging over Wi-Fi → `adb connect <watch-ip>` → run from Android Studio. Confirm performance is smooth (60fps animation, no jank).
4. **Edge cases to eyeball:**
   - Spinning 10+ times in a row produces a visually uniform distribution (not always landing in the same arc — sanity-check the RNG).
   - The pointer alignment math is correct: result label matches the pocket directly under the pointer (off-by-one in `offsetForIndex` is the most likely bug).
