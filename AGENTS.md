# AGENTS.md

## Goal
Build and maintain the Android application described in `SPEC.md`.

`SPEC.md` is the source of truth for product behavior.

Existing UI mockups are visual references. If a mockup conflicts with `SPEC.md`, follow `SPEC.md`.

## Technology
- Kotlin
- Native Android
- Jetpack Compose
- Stable Android/Jetpack APIs
- Landscape orientation

## Engineering principles
- Keep the implementation simple, readable and idiomatic.
- Follow YAGNI.
- Avoid unnecessary layers, interfaces, repositories, dependency-injection frameworks, generic abstractions and boilerplate.
- Prefer Android/Jetpack functionality over third-party dependencies.
- Use the simplest architecture that keeps UI state, campaign state, persistence and game logic understandable and testable.
- Preserve existing working behavior when extending the application.
- Do not rewrite stable parts of the application without a concrete reason.

## Configuration
Important game values must not be scattered as magic numbers.

It must be straightforward to change:
- total campaign level count,
- required correct answers per level,
- future per-level difficulty parameters,
- future timer parameters,
- Free attempts/cooldown and Premium custom defaults/rest bounds.

A simple `LevelConfig`-style model is appropriate.

Do not build an unnecessarily generic level engine.

The goal is simply to avoid assumptions such as:
- exactly 30 levels,
- exactly 10 correct answers.

## Campaign and persistence
Campaign progress must persist locally across application restarts.

Store only the state actually required.

At minimum, the application must reliably know the highest unlocked level.

Rules:
- Level 1 is initially available.
- Completing the highest unlocked level unlocks the next configured level.
- Previously unlocked levels remain replayable.
- Replaying an earlier level never reduces progress.
- Locked levels cannot start.
- Reset Progress resets campaign progress only.
- Reset Progress must not erase player name, math settings, Space Pass plan, limits, usage or cooldowns.
- Completing the final configured level must never unlock a nonexistent level.

## Game logic
Keep core game rules and question generation separate from visual Compose code where practical.

Follow `SPEC.md`, especially for:
- result and operand ranges,
- lives,
- question progression,
- level completion,
- campaign unlocking,
- crystal/energy progression,
- persistent progress,
- reset behavior.

Do not silently invent product behavior when the specification is genuinely ambiguous.

## UI
- Preserve the Space Adventure visual direction.
- Entire application remains landscape.
- Controls must be large and child-friendly.
- Space Map scrolls horizontally.
- Generate level nodes programmatically from configuration.
- On map entry, position the viewport around the highest unlocked level where practical.
- Completed, current/unlocked and locked levels must be visually distinct.
- Previously unlocked levels remain replayable.
- The gameplay keypad must fit in one horizontal row on a typical landscape phone.
- Backspace and Confirm must be spatially separated.
- Do not use the Android system keyboard during gameplay.

## Settings and Space Pass
- Use SPEC.md sections 3 and 26 as the source of truth for saving and plan transitions.
- Keep one Save Settings action for the whole form; plan and limit controls edit a draft until saved.
- Saving opens the Space Map with a short confirmation; Back discards unsaved changes.
- Keep Settings scrollable in landscape and preserve its dark Space Adventure styling.
- Consume attempts once when unlocked gameplay starts, including Retry and replay.
- Persist usage and cooldown-end timestamps before gameplay; do not interrupt the last allowed attempt.
- Keep Free and Premium custom usage independent; Unlimited Premium bypasses restrictions.
- Keep the development mock labelled and free of real billing integrations.

## Animation
Prefer lightweight native Jetpack Compose drawing and animation APIs.

Important effects:
- crystal collection,
- mechanical arm extension/retraction,
- crystal movement into the tank,
- tank filling,
- rocket launch,
- correct-answer feedback,
- gentle wrong-answer feedback,
- level-completion celebration,
- subtle button feedback.

Repeated animations must remain short enough that math practice stays fluid.

Avoid game engines and complex animation frameworks unless clearly necessary.

## Testing
Tests should focus on behavior that can actually break the game. The checklists below define coverage requirements: in default verification, run the checks relevant to the change and its direct dependencies; in full DoD, execute every listed check. Product and engineering requirements always apply, regardless of verification mode.

### Question generator
Test that:
- addition results are within `MIN..MAX`; subtraction ignores MIN and has `0 <= b <= a <= MAX`,
- every operand is within `0..MAX`,
- subtraction never produces a negative result,
- selected operations are respected,
- invalid ranges are handled,
- per-attempt uniqueness and all five category limits hold before controlled fallback; wrong answers retain the same question without recording it again.

### Campaign
Test that:
- initial state unlocks Level 1,
- completing a level unlocks the next level,
- replaying an earlier level does not reduce progress,
- locked levels cannot start,
- progress survives persistence/reload,
- Reset Progress returns campaign progress to Level 1,
- Reset Progress preserves unrelated settings,
- completing the final configured level does not unlock an invalid level.

### Gameplay
Test that:
- correct answers increase crystal/energy progress,
- wrong answers do not,
- wrong answers remove a life,
- already collected energy remains after a wrong answer,
- reaching the configured number of correct answers completes the level,
- logic does not assume `requiredCorrectAnswers` is always 10.

### Settings and play limits
Test that:
- saving the full form persists math, player, plan and limits and opens the map,
- reopening Settings allows another successful limit edit/save,
- Back discards all unsaved edits and inactive limit controls are disabled,
- Free and custom Premium exhaustion, expiry and restart persistence work,
- plan switching and Unlimited preserve independent usage and custom values,
- active invalid limits prevent saving,
- saving unchanged values and recomposition never consume or refund attempts.

Avoid tests that merely verify framework behavior or trivial UI implementation details.

## Working style
- Make small, focused code units.
- Prefer clear names.
- Do not overengineer.
- Do not alter working behavior unless required by the task or `SPEC.md`.
- Follow the verification modes below. Default verification applies unless the user explicitly requests full DoD.
- Fix compilation/test failures caused by changes.

## Default verification

- Verify the same checkout the user opens in Android Studio. A build or launch from a separate clone does not verify the user's project. Resolve conflicting declarations in that checkout.
- After code, resource, dependency or build-configuration changes, build the final debug APK (normally `:app:assembleDebug`). Code inspection, Compose previews and passing tests do not replace a successful application build.
- Run existing tests for changed behavior and direct dependencies. Compile affected test source sets. Add tests for meaningful logic or a reproduced bug; cosmetic changes do not require new tests.
- When an emulator or device is available, install the final debug APK and launch its MAIN activity outside instrumentation. Confirm startup and inspect the displayed UI and startup diagnostics. For UI or interaction changes, also verify the affected screen or flow.
- If no emulator or device is available, report the successful build and explicitly state that installation/startup and visual checks were not verified. Never present an unperformed check as passed.
- Do not routinely run release builds, the entire connected test suite or a complete manual game walkthrough. Broaden checks when shared infrastructure, dependencies, build configuration or observed failures indicate wider regression risk.
- During iteration, use the narrowest checks that diagnose the problem. Verify the final changes; reuse successful results only if subsequent changes cannot affect them. Do not repeat passed checks without a concrete reason.
- Preserve user progress/settings. Restore any user data changed temporarily for verification. After automated checks, leave the final normal application installed and launchable when a device is available.
- Documentation-only changes, including this file, require review of the text/diff for correctness and conflicting instructions; they do not require an application build or emulator run.
- Finish when the requested behavior and required checks are satisfied. Do not add unsolicited improvements. Briefly report what passed, material checks not performed and any blockers; do not claim completion with a failed build or a known startup failure.

## Full DoD - explicit request only

- Requests such as "pelny DoD", "pełny DoD", "pełna weryfikacja", "sprawdz absolutnie wszystko", "sprawdź absolutnie wszystko" or equivalent explicit wording activate full verification. A routine request to check a change does not activate full DoD.
- Before execution, collect every verification requirement in this file into one checklist, including Testing, Final device verification, Star hints and Campaign finale. Check the applicable product requirements in this file and `SPEC.md`, rather than limiting coverage to the latest change.
- Build debug and release, compile main and test source sets, and run all existing unit and instrumentation tests. Execute every listed behavioral and manual/visual check, including settings/play limits, campaign/persistence, star hints and the campaign finale, regardless of the size of the latest change.
- An automated test does not replace an explicitly required manual or visual check. If a listed requirement lacks an automated test, verify it manually or implement a meaningful test; do not silently omit it.
- Track each checklist item as passed, failed or blocked, with evidence (test result or observed behavior) and a reason for any failure/blocker. Do not skip checks merely because the changed area is small.
- Full DoD is complete only when every required item has been executed and passed on the final changes. If any item fails or is blocked, explicitly report full verification as incomplete and identify the remaining items. Never claim that all checks passed based only on a partial run.
- Preserve user data and perform Final device verification after automated checks. Provide the completed checklist or a reference to it in the final report. Full DoD confirms the defined checks; it is not a guarantee that no undiscovered bugs exist.

## Final device verification (full DoD)

After connectedDebugAndroidTest, reinstall the deliverable APK and launch its MAIN activity outside instrumentation. Verify menu, map, level entry, incorrect-answer retry, correct-answer advancement, and a cold start. Inspect startup diagnostics and the displayed UI. Leave the normal application installed and launchable after all automated checks; passing instrumentation alone does not complete this verification.

Verification must use the same checkout the user opens in Android Studio. A successful build or launch from a separate clone does not verify the user's project. Resolve conflicting declarations and compile both main and test source sets in that checkout before reporting completion.

## Star hints

- Preserve the one-row keypad and reuse normal answer grading/feedback.
- Hints are temporary UI sessions, never persisted game/campaign/pass state.
- MAX <= 20 controls availability; the first ceil(configured campaign size / 3) levels demonstrate and the remaining levels allow gestures.
- Keep the 5x4 row-major grid, fixed sacks, stable star IDs and atomic 20-star capacity.
- Dismiss and cancel the hint before confirming an answer; X/Back must preserve input.
- In full DoD, verify addition/subtraction, dragging/return/dimming, configured automatic/interactive mode boundary, rapid edits,
  and normal app launch visually on the emulator after all automated tests.
- Show group outlines and numeric count badges only in automatic addition.
- Subtraction stars fade to gray and lose their glow; never draw a red cross.

- Interactive stars settle from their release point into the nearest free slot. Preserve chosen slots when other stars move or return; never restart a released star at its sack.
- Tutorial fingers share an 8-second idle timeout, at most three movements per cycle and three cycles per stage. Touch cancels without refunding cycles. Never consume input or change counting/game state from a tutorial.
- Interactive extraction and subtraction-dimming milestones last for the hint session; returning/relighting stars never re-enables a completed stage. Automatic hints omit tutorial fingers.

## Campaign finale
- Preserve the existing launch before the final-level story; earlier levels return normally.
- Keep completion durable independently of reward playback. Skip opens victory, which waits indefinitely for map navigation.
- Reuse the existing rocket and preserve sprite alpha/aspect ratio. Never add attempts or levels for the story.
- In full DoD, verify the full ending, skip, replay and interruption on an emulator; reinstall and cold-launch after connected tests.
