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
9. Click **Start Game**, then select an unlocked planet on the **Space Map**. Use the in-game digit buttons, **⌫** to delete, and **✓** to submit. Configure the name, operations, and result range in **Settings**.

The emulator may display the screen sideways if its own orientation is portrait; use its **Rotate** control. The app requests landscape throughout.

## Build and test

From Android Studio's Terminal on Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On macOS/Linux, use `bash ./gradlew` with the same tasks. Set `JAVA_HOME` to JDK 17. Android Studio writes `local.properties` with your Android SDK path; this file is machine-specific and should not be shared.

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Test reports are at `app/build/reports/tests/testDebugUnitTest/index.html`.

## Implementation

- `Game.kt`: pure Kotlin question generation, input, lives, feedback, level completion, `LevelConfig`, and campaign unlocking.
- `MainActivity.kt`: typed `Screen` navigation, Compose screens, settings, map, crystal collection, tank filling, rocket launch, and Game Over.
- `CampaignStore.kt`: separate `campaign` SharedPreferences storing the highest unlocked level and final-level completion.
- `SpaceArt.kt`: native Canvas rocket, crystals, astronaut, planets, stars, and confetti.
- `GameTest.kt`: generator constraints, input/pause, lives/energy, configurable level lengths, unlocking/replay, and final-level boundaries.
- `AnswerButtonTest.kt`: real keypad, map, ten-answer completion, replay, persistence/reset, and retry/navigation flows.

## Campaign flow

**Start Game → Space Map → selected level → Level Complete / launch → Space Map**.

`CampaignConfig.TOTAL_LEVELS` controls campaign size (initially 30). `DEFAULT_REQUIRED_CORRECT_ANSWERS` is temporarily 4 for faster user testing (set it back to 10 for the full practice length); change entries in `CampaignConfig.levels` to supply different `LevelConfig.requiredCorrectAnswers` values per level. Completion, crystals, tank energy, and the HUD use that configuration. Levels have no question timer and no rounds.

The rocket stays landed while questions are answered. A correct answer animates the mechanical arm reaching a crystal, grasping it, returning it to the tank, transferring energy, and retracting. A wrong answer removes one of three lives while preserving energy. Game Over offers Retry and Back to Map. A full tank triggers engine startup and a visible upward flight, then a celebration and return to the map. Only Exit in the main menu finishes the activity.

Completing the highest unlocked level unlocks exactly the next configured level. Earlier levels remain replayable; replay never reduces or advances unrelated progress. Final-level completion is recorded without unlocking an extra level. Settings and progress persist independently; Reset Progress requires confirmation and preserves name, operations, and MIN/MAX. Settings Back discards unsaved edits. The keypad stays in one landscape row and uses no system keyboard.

Version `2.0-campaign` (`versionCode = 2`) identifies this campaign build. To replace an older installed build without clearing settings/progress:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run device tests with `./gradlew.bat connectedDebugAndroidTest` while an emulator is running. These tests change campaign/settings test data on their target devices.

## Black or corrupted screen in the emulator

If the emulator shows a black screen or repeated/striped graphics without an application crash, stop it and select **Software** graphics in Device Manager's advanced AVD settings, then use **Cold Boot**. The Medium Phone API 37 AVD on this machine is configured with `hw.gpu.mode=software`; automatic host GPU rendering produced corrupted frames. Cold boot preserves installed applications, campaign progress, and settings; wiping device data is unnecessary.
