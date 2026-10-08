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
- future timer parameters.

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
- Reset Progress must not erase player name or math settings.
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
- result is always within `MIN..MAX`,
- every operand is within `0..MAX`,
- subtraction never produces a negative result,
- selected operations are respected,
- invalid ranges are handled,
- immediate identical repetition is avoided where possible.

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

Avoid tests that merely verify framework behavior or trivial UI implementation details.

## Working style
- Make small, focused code units.
- Prefer clear names.
- Do not overengineer.
- Do not alter working behavior unless required by the task or `SPEC.md`.
- Before considering work complete, build the project and run relevant tests.
- Fix compilation/test failures caused by changes.
- When an emulator is available, run the application and verify affected flows where practical.
