# Roulette Watch App

A European roulette wheel for Samsung Galaxy Watch 8 (Wear OS 6), built with Jetpack Compose for Wear OS.

## What it does

Tap the screen to spin a European roulette wheel. A ball orbits counterclockwise while the wheel rotates clockwise, then drops into a numbered pocket. The result — number and colour — is displayed in the centre. Tap again to spin again.

- **37 pockets** — standard European wheel layout (single zero)
- **Animated ball** — orbits the rim, decelerates, and drops into the colour band
- **Ball follows the wheel** — after landing, the ball locks to its sector and moves with the wheel
- **Winning pocket highlight** — the landed sector pulses briefly after each spin
- **Visual only** — no betting, no chips, no history

## Tech stack

- Kotlin + Jetpack Compose for Wear OS (Material 3)
- `Animatable` + coroutines for all animation
- `Canvas` + `DrawScope` for the wheel rendering
- Min SDK 30 (Wear OS 3) · Target SDK 35 (Wear OS 6)

## Project structure

```
app/src/main/java/com/vincentla/roulette/
├── MainActivity.kt
├── data/
│   └── EuropeanWheel.kt      # pocket order, red/black/green sets
└── ui/
    ├── RouletteApp.kt         # spin logic, state, animation
    ├── RouletteWheel.kt       # canvas-drawn wheel + ball
    └── theme/Theme.kt
```

## Running locally

1. Open in Android Studio (Quail 2026.1.1 or newer)
2. Connect a Wear OS emulator (round, 454×454) or a physical Galaxy Watch 8 via Wi-Fi ADB
3. Run the `:app` configuration