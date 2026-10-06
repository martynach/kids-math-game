# AGENTS.md

## Goal
Build the Android application described in `SPEC.md`.

Treat `SPEC.md` as the source of truth for product behavior and `ui-mockup.png` as the visual reference.

## Technology
- Kotlin
- Native Android
- Jetpack Compose
- Stable Android/Jetpack APIs
- Landscape orientation

## General engineering principles
- Keep the implementation as simple as possible.
- Prefer readable, idiomatic Kotlin.
- Follow YAGNI.
- Do not introduce architecture or abstractions for hypothetical future requirements.
- Prefer Android/Jetpack functionality over third-party dependencies.
- Avoid unnecessary layers, interfaces, repositories, dependency-injection frameworks, generic abstractions, and boilerplate.
- Use the simplest architecture that keeps UI state and game logic understandable and testable.

## Game logic
Keep core game rules and question generation separate from visual Compose code where practical.

The implementation must follow all constraints from `SPEC.md`, especially:
- result range rules,
- operand range rules,
- lives,
- round progression,
- timers,
- correct/wrong answer behavior.

Do not silently invent behavior when the specification is genuinely ambiguous. Choose the simplest reasonable implementation only for minor implementation details; for product-level ambiguity, ask.

## UI
- Follow `ui-mockup.png` for the overall visual direction.
- Preserve the space-adventure theme.
- Keep controls large and child-friendly.
- The in-game keypad must fit in one horizontal row on a typical landscape phone.
- Backspace and Confirm must be spatially separated to reduce accidental confirmation.
- Do not use the Android system keyboard during gameplay.
- Prefer simple Jetpack Compose animations over complex animation frameworks or game engines.

## Testing
Tests should focus on behavior that can actually break the game.

In particular, thoroughly test the math-question generator:
- result always within MIN..MAX,
- every operand within 0..MAX,
- subtraction never produces a negative result,
- selected operation settings are respected,
- invalid range combinations are handled,
- immediate identical question repetition is avoided where possible.

Also test:
- life loss,
- round progression,
- life reset between rounds,
- game completion,
- timer/timeout behavior where practical.

Avoid tests that merely verify framework or trivial UI implementation details.

## Working style
- Make small, focused code units.
- Keep constants such as round time limits in one obvious location.
- Prefer clear names over comments explaining complicated code.
- Do not overengineer.
- Before considering the task complete, build the project and run relevant tests.
- Fix compilation and test failures caused by your changes.
