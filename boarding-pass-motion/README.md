# Boarding Pass Motion

Jetpack Compose motion experiment focused on a single polished boarding pass interaction.

The experience simulates a folded physical ticket opening in three connected sections, flipping to a QR-code back side, reacting to Supabase Realtime validation, and then continuing as a live flight capsule with a gate countdown.

## Demo

https://github.com/user-attachments/assets/30995f03-1697-41ce-853e-d36940b3aefb


## Highlights

- Three-part physical fold animation using `graphicsLayer`, `rotationX`, transform origins, shadows, and perspective.
- Sequential open and close motion, avoiding card expansion shortcuts.
- QR-code back side with Supabase Realtime validation.
- Haptic feedback and scan-light animation when the ticket is read.
- Minimal `FlightCapsule` synced with the ticket state.
- Gate closing countdown after boarding approval.
- Past tickets section styled with the same boarding-pass language.

## Realtime Flow

The demo listens to Supabase Realtime updates from:

```text
public.tickets
```

The active demo ticket is:

```text
TICKET_DEMO_001
```

When `scan_count` increases, the app treats it as a successful scan and plays the validation sequence.

## Run

```bash
./gradlew :app:assembleDebug
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Stack

- Kotlin
- Jetpack Compose
- Supabase Kotlin SDK
- ZXing QR generation
