# Kids Math Game

A local, landscape Android game built with Kotlin and Jetpack Compose. No account, network permission, advertising, or backend is required.

## Open and run for the first time

1. Start Android Studio. At the welcome screen choose **Open** (or **File → Open** if a project is already open).
2. Select this **KidsMathGame1** folder, containing `settings.gradle.kts`, and click **OK**. Trust the project if prompted.
3. Open **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**. Set **Gradle JDK** to **JDK 17**. If it is absent, use **Download JDK**, select version **17**, and finish the download. Click **Apply → OK**, then **File → Sync Project with Gradle Files**. This project uses Gradle 8.11.1; the Java 25 bundled with some newer Android Studio releases cannot run that version. Let Gradle sync finish. If prompted to install SDK components, approve installation and review/accept their license terms. Internet access is needed for initial downloads.
   On this machine, JDK 17 is already downloaded at `.tools/jdk17/jdk-17.0.20.1+1` inside the project. You can select **Add JDK** and choose that directory instead of downloading it again. `local.properties` points to the Android SDK installed by Android Studio.
4. Open **Tools → SDK Manager**. Under **SDK Platforms**, install **Android 15 / API 35**. Under **SDK Tools**, install **Android SDK Platform-Tools**, **Android SDK Build-Tools 35.0.0**, and **Android Emulator**. Click **Apply**.
5. Open **Tools → Device Manager**. Click **+ → Create Virtual Device**, select a phone such as **Pixel 6**, and click **Next**.
6. Select an **API 35** system image suitable for your computer (usually **x86_64** on an Intel/AMD Windows PC). Click its download icon if needed, then **Next → Finish**.
7. Start the virtual device with its **▶** button. If startup reports a virtualization problem, enable hardware virtualization in your PC's BIOS/UEFI and follow Android Studio's emulator acceleration instructions.
8. In Android Studio's top toolbar select the **app** run configuration and the emulator. Click the green **▶ Run** button. Wait for installation; the game opens in landscape.
9. Click **New Game** to play. Use the in-game digit buttons, **⌫** to delete, and **✓** to submit. Configure the name, operations, and result range in **Settings**.

The emulator may display the screen sideways if its own orientation is portrait; use its **Rotate** control. The app requests landscape throughout.

## Build and test

From Android Studio's Terminal on Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On macOS/Linux, use `bash ./gradlew` with the same tasks. Set `JAVA_HOME` to JDK 17. Android Studio writes `local.properties` with your Android SDK path; this file is machine-specific and should not be shared.

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Test reports are at `app/build/reports/tests/testDebugUnitTest/index.html`.

## Implementation

- `Game.kt`: independent Kotlin rules, generation, input, deadlines, lives, rounds, and transitions.
- `MainActivity.kt`: Compose screens, preferences, app state, and visual animations.
- `SpaceArt.kt`: native Canvas rocket, astronaut, planets, stars, trophy, and confetti artwork.
- `GameTest.kt`: behavior tests, including exhaustive valid result ranges.
- `GameRules`: all three question time limits, feedback duration, and round celebration duration in one place.

An empty answer cannot be submitted. Input is capped at three digits (the largest supported result is 100). Leading zeros are accepted as numeric input. Rounds wait for **Next Round**; there is no automatic transition. Only remaining lives are shown as hearts. Correct answers show falling confetti and an animated happy face for 1 second; wrong answers and timeouts show an animated sad face and encouragement for 1.6 seconds. Android Back lets settings return without saving and asks before leaving an active game. Saved settings survive relaunch; an interrupted game is retained across activity recreation while the process lives, but a fresh process starts at the main menu.

The mockup guides the colors, layout, rocket route, and large controls. Illustrations are drawn in Compose and work offline at different screen sizes.

## Verification on 2026-10-06

- `:app:assembleDebug`: succeeded; installable debug APK generated.
- `:app:test`: succeeded; all 12 tests passed in both debug and release variants. The generator test covers all 15,453 valid range/operation combinations.
- `:app:lint`: succeeded with zero errors and five non-blocking warnings (three available dependency updates, the specification's fixed landscape orientation, and an optional SharedPreferences KTX convenience).
- APK inspection confirmed the main launcher activity and no internet permission.
- Reviewed the implementation against `SPEC.md`, including defaults, persistence, name formatting, ranges, keypad layout, three rounds, lives, per-question deadlines, feedback, rocket movement, and end states.
- Application startup and answer entry/confirmation were subsequently verified on Android 15 (API 35) and Android 17 (API 37) emulators with the connected UI regression test. Physical-device verification has not been performed.

Android SDK 35, platform tools, Gradle, and JDK 17 were downloaded locally to enable these checks. `.tools`, `.android-sdk`, and `.gradle-user` are ignored tooling directories; they are not app source or runtime dependencies.

## Confirm button regression

The keypad now receives the current phase and answer text as immutable values. Previously it received only the same mutable `Game` instance, allowing Compose to skip its update and leave Confirm disabled after entering digits.

`AnswerButtonTest` checks entering and deleting digits, Confirm becoming enabled/disabled, submitting a correct answer, and the next question becoming playable. Run it on a running emulator with `:app:connectedDebugAndroidTest`. This passed on both API 35 and API 37, alongside `:app:assembleDebug`, `:app:test`, and `:app:lintDebug`. Test-only Espresso 3.7.0 supports the newer Android input APIs.

The UI suite now also checks that wrong answers remove hearts and show a sad face, correct answers show a happy face and confetti, and round 2 starts only after pressing Next Round. All three UI tests passed on each emulator. Unit tests verify that waiting ten minutes on Round Complete does not start the next round or consume lives, and that wrong/timeout feedback lasts at least twice the original duration.

## Visual redesign

Native Compose drawing adds shaded cartoon planets, a glossy rocket, a friendly astronaut, glowing stars, and an atmospheric animated starfield. All screens share rounded bold headings, gradient buttons with press feedback, and shaded cards. The landscape keypad keeps Backspace and Confirm at opposite ends.

Correct answers add sparkle bursts, falling confetti, and smooth rocket movement. Wrong answers use a gentle shake and pink pulse. Round completion has a larger celebration; final victory adds more confetti, orbiting stars, a gold planet, and a trophy. Existing game rules, feedback durations, navigation, settings, and test files were preserved.

After the redesign, `:app:assembleDebug`, `:app:test`, `:app:connectedDebugAndroidTest`, and `:app:lintDebug` passed. The three connected UI tests passed on each API 35 and API 37 emulator. Manual visual review covered menu, settings, correct/wrong feedback, all three rounds, both round completion screens, final victory, and Game Over through normal gameplay. Physical-device testing remains unperformed.
