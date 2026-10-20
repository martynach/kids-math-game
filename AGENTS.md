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
Tests should focus on behavior that can actually break the game.

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
- Before considering work complete, build the project and run relevant tests.
- Fix compilation/test failures caused by changes.
- When an emulator is available, run the application and verify affected flows where practical.

## Final device verification

After connectedDebugAndroidTest, reinstall the deliverable APK and launch its MAIN activity outside instrumentation. Verify menu, map, level entry, incorrect-answer retry, correct-answer advancement, and a cold start. Inspect startup diagnostics and the displayed UI. Leave the normal application installed and launchable after all automated checks; passing instrumentation alone does not complete this verification.

Verification must use the same checkout the user opens in Android Studio. A successful build or launch from a separate clone does not verify the user's project. Resolve conflicting declarations and compile both main and test source sets in that checkout before reporting completion.

## Star hints

- Preserve the one-row keypad and reuse normal answer grading/feedback.
- Hints are temporary UI sessions, never persisted game/campaign/pass state.
- MAX <= 20 controls availability; levels <= 10 demonstrate and levels >= 11 allow gestures.
- Keep the 5x4 row-major grid, fixed sacks, stable star IDs and atomic 20-star capacity.
- Dismiss and cancel the hint before confirming an answer; X/Back must preserve input.
- Verify addition/subtraction, dragging/return/dimming, mode boundary 10/11, rapid edits,
  and normal app launch visually on the emulator after all automated tests.
- Show group outlines and numeric count badges only in automatic addition.
- Subtraction stars fade to gray and lose their glow; never draw a red cross.

- Interactive stars settle from their release point into the nearest free slot. Preserve chosen slots when other stars move or return; never restart a released star at its sack.
