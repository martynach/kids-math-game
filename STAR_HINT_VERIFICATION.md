# Star Counting Hint feedback — verification

Date: 2026-10-21. Checkout: `C:\Users\chomi\workspace\KidsMathGame\KidsMathGame1`.

## Approved behavior

- Automatic levels are the first `ceil(CampaignConfig.levels.size / 3)` levels;
  the rest are interactive. The existing one-level testing configuration is preserved.
- Returning a star leaves its slot empty. Other stars keep their chosen slots,
  stable IDs and lit/dimmed states. No compaction is introduced.
- The question star uses a 76 dp target, a 60 dp gold drawing, scaling 1.0–1.12
  and a small shimmer. It sits left of the expression, on a row separate from Map.
- Shared idle timeout: `HINT_IDLE_TIMEOUT_MS = 8_000L`. Each stage allows three
  cycles of three 900 ms movements. There is another full idle timeout between cycles.
  Interaction immediately cancels and restarts idle time, without refunding cycles.
- Opening tutorials reset per question, including identical repeated questions.
  Extraction and dimming milestones/budgets reset only on a new hint session.
  Returning all stars or relighting them cannot restore a completed tutorial stage.
- Tutorial rendering never changes the model, captures input, persists state,
  grades answers or consumes attempts. Pause, background and screen disposal cancel it.

## Changed files

Production: `MainActivity.kt`, `StarHintScreen.kt`, `StarHintState.kt`, new
`HintTutorial.kt` and `HintTutorialUi.kt` (all under
`app/src/main/java/com/example/kidsmath/`).

Unit tests: `StarHintStateTest.kt`, `QuestionGeneratorTest.kt`, new
`HintTutorialTest.kt` under `app/src/test/java/com/example/kidsmath/`.

Device tests: `StarHintFlowTest.kt`, `AnswerButtonTest.kt`, `FinaleFlowTest.kt`,
`GeneratorFlowTest.kt`, `SpacePassFlowTest.kt` under
`app/src/androidTest/java/com/example/kidsmath/`. Existing tests are adapted to
the configured single-level campaign; pure model/hint tests explicitly supply
larger campaign sizes instead of changing production configuration.

Documentation: `README.md`, `SPEC.md`, `AGENTS.md`, this report.
Pre-existing edits in `Game.kt`, `GameTest.kt` and the campaign-size part of
`SPEC.md` were preserved.

## Automated acceptance checklist

- [x] Mode boundaries for campaigns of 1, 2, 3, 5, 6, 12, 15 and 30 levels.
- [x] No finger at seven seconds; first cycle at eight seconds; three movements;
  disappearance; another eight idle seconds before another cycle; three-cycle cap.
- [x] Touch cancellation and idle restart without refunding the cycle budget.
- [x] Empty interactive grid shows extraction; first extraction disables it for
  the session; returning every star does not restore it; reopening resets it.
- [x] Subtraction extraction precedes dimming; dim tutorial requires a lit,
  non-returning star; the first real dimming disables it even after relighting.
- [x] Extraction/dimming budgets are independent. Automatic mode has no finger.
- [x] Finger never creates or dims a star and allows actual touches through it.
- [x] Gameplay star is available only for MAX <= 20; star/Map bounds do not overlap;
  hint entry, pause, map and answer feedback hide the opening tutorial.
- [x] New question resets the opening budget, including repeated `0 + 0`.
- [x] Dimming, relighting and returning dimmed stars preserve other stars and slots;
  a drag does not trigger a tap; cancelled/short/wrong-bag gestures preserve state.
- [x] Existing grid geometry, capacity, automatic timelines, labels, release-point
  travel, rapid edits, X/Back, input preservation and pass isolation.
- [x] Unit regression coverage: generator ranges, selected operations, uniqueness,
  category limits/fallback; lives, energy, campaign progression/persistence/reset,
  configurable correct-answer targets, Space Pass and finale rules.

## Final outcomes

- PASS: debug/release builds, main and test-source compilation, all 47 unit tests
  in each build variant, lint, and all 40 emulator tests (zero failures/skips).
  Evidence: `work/star-hint-verification/verified.log` and Gradle reports under
  `app/build/reports/`. Lint reports zero errors and five existing warnings
  (fixed landscape orientation and SharedPreferences KTX suggestions).
- PASS: screenshots inspected for the larger isolated question star, one-row
  keypad and small tutorial glove. Emulator tests exercise extraction, dimming,
  relighting, dimmed-star returns, cancellation and cycle limits with controlled time.
- PASS: deliverable debug APK reinstalled, all four original preference files
  restored and compared, then MAIN activity cold-launched outside instrumentation
  (`am start -W`: Status ok). The normal application is left installed and running.
- Scope limitation: after the user requested finishing, no additional real-time
  manual walkthrough was run. The full repeated-cycle and interactive-flow checks
  above are automated; this report does not claim a separate complete manual DoD.
