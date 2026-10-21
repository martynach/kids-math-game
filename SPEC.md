# Kids Math Game — Product Specification

## 1. General

Kids Math Game is a simple native Android math game for children.

The application should:
- be implemented in Kotlin using Jetpack Compose,
- work entirely in landscape orientation,
- use a colorful, child-friendly Space Adventure theme,
- remain simple and intuitive for a young child,
- store settings and campaign progress locally,
- not require login, backend services, or Internet access.

## 2. Main Menu

The Main Menu contains:
- Start Game
- Settings
- Exit

The screen should also display a welcome message.

Without a player name:

`Welcome to Kids Math Game!`

With a player name:

`Welcome to Kids Math Game, Karolina!`

The player name is optional.

Selecting Start Game opens the Space Map. If campaign progress already exists, the player continues from the existing campaign state.

## 3. Settings

Settings allow the user to configure:

### Player name
Optional.

### Operations
The user can select:
- Addition
- Subtraction
- Addition and Subtraction

### Result range
The user can configure:
- MIN
- MAX

Allowed values:

`0–100`

MIN must not be greater than MAX.

Default settings:
- player name: empty
- operations: Addition and Subtraction
- MIN = 0
- MAX = 10

Settings must persist locally.

### Appearance and saving

The entire Settings screen uses the same dark navy/purple Space Adventure styling as the Free/Premium cards: rounded panels, subtle borders/glows, readable text and dark input fields. Use the three-panel Space Pass settings mockup as a visual reference, while following this specification for behavior. Keep the screen usable in landscape and scroll content that exceeds the available height.

One `Save Settings` button, outside the scrolling content, saves player name, operations, MIN/MAX, selected plan, Unlimited attempts and custom play-limit values. There is no separate Save play limits button. Plan selection and the unlimited toggle edit the draft; they do not immediately change persisted settings or gameplay rules.

Disable Save when the math range is invalid or when active Premium custom limits are invalid. Invalid inactive custom fields must not prevent saving Free or unlimited Premium; retain the previously saved valid custom value for each invalid field. Preserve valid custom values when toggling Unlimited on/off or selecting another plan.

After saving, open the Space Map and show a short `Settings saved` toast. Do not require dismissal of an additional confirmation dialog. Saving Settings does not start gameplay or consume an attempt.

Leaving Settings with Back (including Android Back) discards all unsaved edits, including the selected plan and play limits. Back returns to the rest screen if Settings was opened there; otherwise it returns to the main menu. Successfully saving always opens the map.

### Reset Progress

Settings also contains:

`Reset Progress`

Selecting it must display a confirmation dialog:

**Reset progress?**

`All completed levels and your current progress will be lost. You'll start your space adventure again from Level 1.`

Buttons:

`Cancel` | `Reset`

Reset should be visually presented as a destructive action.

Reset Progress resets campaign progress only. It must NOT reset:
- player name,
- selected operations,
- MIN/MAX settings,
- selected plan, custom play limits, remaining attempts and cooldown timestamps.

After reset:
- Level 1 is unlocked,
- all later levels are locked.

## 4. Math Question Generation

Addition uses nonnegative operands with result in `MIN..MAX`. Subtraction ignores MIN:
`0 <= right <= left <= MAX`, so results below MIN (including zero) are valid.
Parent settings remain whole numbers in 0–100 with MIN <= MAX.

The raw sampler uses Long triangular counts and binary-search unranking, never a table
of operand pairs. Addition counts `sum(s+1)` for MIN <= s <= MAX; subtraction counts
`(MAX+1)*(MAX+2)/2`. Each ordered expression is uniform within its operation. BOTH
chooses an operation 50/50 before sampling, independently of pool sizes. The pure
sampler and `ArithmeticRange` support at least MAX=1,000,000; the UI range is unchanged.

Within an attempt, normally issue each ordered expression only once (`3+5` and `5+3`
are distinct). Five independent category rules each permit one occurrence: addition
with zero, subtraction by zero, equal-operand subtraction, addition with one, and
subtraction by one. Matching multiple rules consumes every matching limit, including
both subtraction rules for `0-0` and both addition rules for `0+1`. Rules have an ID,
maximum and predicate; `QuestionLimitTracker` owns counts separately from evaluation.
History stores only issued expressions. New Game instances for Retry, replay and level
entry create fresh history and counts; wrong-answer feedback does not issue a question.

Selection first tries up to 100 uniformly sampled candidates, checks uniqueness,
category limits and the early cap, then applies the probability in section 21.
Fallback stages run in this order:
1. Ignore probabilistic difficulty preference, keeping cap, limits and uniqueness.
2. Relax the cap, keeping limits and uniqueness.
3. Relax category limits, keeping uniqueness.
4. Allow repeats only when every unique expression has been issued.

Math validity and operations never relax. Spaces of at most 20,000 expressions use
an exact indexed scan at each fallback stage without storing the universe. Larger
spaces use 512 shared random probes plus the first history-size+1 indexed questions.
This bounded prefix guarantees an unused expression whenever one exists, even if the
random source repeatedly returns the same candidate. Large-space cap/category
relaxation can occur when probes miss an eligible expression; proving exhaustion of
arbitrary custom predicates would require a full search. Fallback traversal is stable
for a fixed seed and history. Work depends on attempts and issued history, not a
quadratic initialization. `SelectionStage` exposes fallback activation for tests.

Singleton ranges, MIN>0, exhausted overlapping categories, no cap-eligible expressions,
and levels longer than the unique pool all terminate using this hierarchy. In
particular MAX=0 always produces zero-valued arithmetic; MAX=1 and MIN=MAX=20 remain
valid without inventing operands outside the parent range.

## 5. Game Structure

The game is organized as a campaign consisting of Levels.

The initial campaign contains:

`3 levels` (temporary testing configuration; `CampaignConfig.TOTAL_LEVELS`)

The number of levels must be configurable and must not be hardcoded throughout the UI or game logic.

There should be one clear configuration value, for example:

`TOTAL_LEVELS = 30`

Changing the campaign size later to 20, 40, 50, etc. should not require manually rebuilding the map.

Level nodes must be generated programmatically.

## 6. Space Map

The Space Map is the main navigation screen for the campaign.

It represents a long journey through space from one destination to another.

The map must be designed for landscape orientation and scroll horizontally. Use stable irregular planet positions and varied colours, sparse floating scenery, and a pulsing colourful aura with orbiting stars for the current level. Keep locked planets and their locks clear; avoid repeated three-dot decorations beneath every level.

The route may gently move vertically while progressing from left to right.

The visual environment may contain:
- planets,
- moons,
- stars,
- asteroid fields,
- nebulae,
- other space scenery.

Only several level nodes should normally be visible at once, approximately 5–7 depending on screen width.

The player can swipe:
- left to inspect/replay previous levels,
- right to inspect future locked levels.

When the Space Map opens, it should automatically position itself around the highest unlocked level.

### 6.1 Level states

#### Completed level
A completed level:
- is visually marked as completed,
- remains playable,
- may show a star/check or another positive completion indicator.

#### Highest unlocked level
The highest currently unlocked level:
- is larger, pulses and has an orbiting rocket; unlocked planets use vivid colours while locked planets remain muted,
- is playable,
- has the player's rocket positioned at or near it.

#### Locked level
A future locked level:
- displays a lock,
- cannot be started.

Tapping it may trigger subtle visual feedback, but must not start gameplay.

## 7. Campaign Progress

Campaign progress must persist locally across application restarts.

The simplest sufficient representation may be used, for example:

`highestUnlockedLevel`

Initially:

`highestUnlockedLevel = 1`

Completing Level N unlocks Level N+1, unless N is already the final configured level.

Example:

If:

`highestUnlockedLevel = 6`

then Levels 1–6 can be played and Levels 7+ remain locked.

Previously unlocked/completed levels remain replayable.

Replaying an earlier level must never reduce campaign progress.

Completing the final configured level must not attempt to unlock a nonexistent level.

## 8. Level Structure

Each Level is one continuous gameplay session.

A Level is not divided into artificial halves or multiple rounds.

The child lands on a planet and must collect enough energy to prepare the rocket for the journey to the next destination.

The rocket is charged from one visible, freestanding collection tank beside it, on a small base and connected to the rocket by a hose.

The tank should be visible or partially transparent so that the child can clearly see its energy level increasing.

## 9. Questions Required to Complete a Level

For now, every Level requires:

`4 correct answers` (current testing configuration; any future practice target remains configurable)

to complete.

Neither 4 nor 10 is a permanent rule. `CampaignConfig.DEFAULT_REQUIRED_CORRECT_ANSWERS` currently supplies 4 to all configured levels.

The required number of correct answers must be configurable per level.

For example:

`LevelConfig(level = 1, requiredCorrectAnswers = 10)`

In the current testing configuration, every campaign level uses:

`requiredCorrectAnswers = 4` (temporary testing configuration)

The architecture must allow future configurations such as:
- Level 1 → 5
- Level 2 → 7
- Level 3 → 10
- Level 15 → 12

without changing the fundamental gameplay logic.

The HUD, crystal count, tank filling and completion animation must use the current level configuration. Wrong answers do not count toward the target. The correct-answer target is independent of the Space Pass allowance: four correct answers complete a level, while two Free starts exhaust its allowance.

## 10. Rocket Energy and Crystals

Energy crystals are placed around the rocket / planet environment.

They visually represent the energy required to complete the current Level.

The number/progress representation of crystals must correspond to the configured `requiredCorrectAnswers` for that Level.

After every correct answer:
1. positive feedback is shown,
2. a mechanical arm extends from the rocket,
3. the arm grabs one energy crystal,
4. the crystal is brought toward the rocket,
5. its energy is transferred into the rocket's energy tank,
6. the visible energy level in the tank increases,
7. Level progress increases,
8. the next math question is presented.

Collectible gems are large, with bright turquoise facets and a gold outline. The arm delivers a gem into a colourful converter above the freestanding tank. The visible gem spins and dissolves in the glowing, vibrating chamber, then a visible liquid stream pours through its nozzle into the tank. The tank fill follows the pour. The full feedback sequence lasts 2.6 seconds, with fast confetti, sparkling stars and a gentle rocket wiggle while the collection arm moves slowly.

The animation should be visually satisfying but short enough that repeated math practice remains fluid.

## 11. Lives

Each Level starts with 3 lives:

`❤️ ❤️ ❤️`

A wrong submitted answer removes one life.

A timeout, if timers are used, is treated as a wrong answer.

Already collected crystals and energy are not removed after a single wrong answer.

When the player reaches 0 lives, the current Level attempt ends.

Previously unlocked campaign Levels remain unlocked.

The Game Over state should allow:
- retrying the current Level,
- returning to the Space Map.

## 12. Timer

The previous three-round timer model no longer applies because Levels are no longer divided into three rounds.

Timer behavior for the new campaign model is intentionally not defined yet.

Do not invent new timer rules until they are specified.

The implementation should make future per-level timer configuration possible without unnecessarily overengineering the current version.

## 13. Gameplay Screen

The gameplay screen should contain:
- a compact HUD,
- remaining lives,
- current Level,
- progress,
- timer if enabled,
- large math expression,
- rocket/planet gameplay area,
- custom numeric keypad.

The rocket remains on the current planet while the child solves questions.

The central progression metaphor is charging the rocket with energy crystals, not moving the rocket a fraction of the distance after every answer.

The rocket only performs its major flight animation after the Level has been completed.

## 14. Answer Display

Initially:

`2 + 7 = ?`

After entering `7`:

`2 + 7 = 7`

For multi-digit answers, digits appear incrementally.

The answer is displayed directly in the expression.

There is no separate answer input field.

## 15. Custom Keypad

The custom keypad must remain in one horizontal row in landscape orientation.

Preferred layout:

`[⌫]   [0][1][2][3][4][5][6][7][8][9]   [✓]`

Backspace is placed far from Confirm to reduce accidental submission.

The function buttons may be slightly larger than digit buttons.

Preferred visual distinction:
- Confirm: green
- Backspace: visually distinct, e.g. orange

`⌫` removes the last entered digit.

It does not submit the answer and does not remove a life.

Only `✓` submits the answer.

No confirmation dialog is shown after pressing `✓`.

## 16. Correct Answer

After a correct answer:
- show positive feedback for 2.6 seconds, matching wrong-answer feedback; the crystal pulses before the arm slowly collects and transfers it, and the happy face remains visible throughout,
- animate the mechanical arm collecting a crystal,
- transfer the crystal/energy into the rocket,
- increase the visible energy level,
- update progress,
- show the next question.

Optional message:

`Great job!`

or:

`Great job, Karolina!`

Do not make the per-question animation so long that it interrupts the rhythm of the game.

## 17. Wrong Answer

After a wrong answer:
- remove one life,
- keep current correct-answer progress,
- do not collect a crystal,
- do not increase rocket energy,
- show friendly feedback (the sad face remains visible for 2.6 seconds),
- clear the input and repeat the same question if lives remain, without consuming category limits or history again; only a correct answer advances.

Do not use:
- explosions,
- frightening effects,
- overly negative feedback.

A subtle shake/error pulse is acceptable.

### Leaving an active attempt

The gameplay Map button or Android Back pauses an active attempt and opens `Return to Space Map?`. `Keep Playing` resumes the same question, lives, energy and remaining feedback duration, without consuming another attempt. Confirming `Space Map` discards the active attempt's energy and returns to the map; unlocked campaign progress and the consumed Space Pass attempt remain unchanged. Game Over and completion can return to the map directly.

Active question, lives and energy are in-memory session state; an interrupted attempt is not restored after process death. Campaign progress, settings and consumed attempts/cooldown timestamps remain persisted.

## 18. Level Completion

When the player reaches the configured number of correct answers:
- the rocket's energy tank becomes completely full,
- display a message such as `Rocket ready!`.

Then perform an approximately ten-second Level completion sequence: drain the collection tank through a visible fuel hose into the rocket, launch, fly a celebratory loop, and depart with confetti and firework-like starbursts.

The sequence should clearly show:
1. the rocket's engines beginning to glow,
2. a small pre-launch shake,
3. engine flame,
4. the rocket lifting off from the planet,
5. the rocket visibly flying away/upward toward the next destination.

This rocket flight should be one of the most noticeable animations in the game.

The celebration may also include:
- confetti,
- stars,
- sparkles,
- a trophy,
- a happy/smiley element.

Example text:

`Great job, Karolina!`

`Level 8 completed!`

After the celebration, return to the Space Map.

If the completed Level was the highest unlocked Level, unlock the next one.

The rocket should then appear at the new highest unlocked Level on the map.

## 19. Replaying Completed Levels

Previously unlocked Levels can be replayed.

Completing an older Level again:
- shows the normal gameplay and completion celebration,
- must not reduce or incorrectly change `highestUnlockedLevel`,
- must not unlock unrelated Levels.

Campaign progression only advances when appropriate.

## 20. Final Campaign Level

The final configured Level is the end of the current campaign.

It must not unlock a nonexistent Level.

A larger campaign-completion celebration may be used after completing the final Level.

The exact final-campaign celebration can be refined later.

## 21. Difficulty Progression

Difficulty is evaluated independently of random generation, history and limits:
`clamp(75*magnitude - simplicityPenalty + 12*carryBorrowCount, 0, 100)`.
Magnitude is result/MAX for addition and left/MAX for subtraction; at MAX=0 it is zero.
Zero addition, subtraction by zero and equal subtraction use a single 25-point penalty.
Otherwise addition with one or subtraction by one uses 30 points. Penalties never stack;
zero/equal conditions precede one. Decimal carries/borrows are counted position by
position, including propagated transfers (999+1 has three carries; 1000-1 three borrows).
All weights live in `DifficultyWeights`.

`ProgressionPolicy` uses progress `(level-1)/(totalLevels-1)`, or zero for one level.
The target is `8+84*progress`. For progress < 0.2, the hard cap is
`30+70*(progress/0.2)`; thereafter it is 100. First levels therefore favor easy
questions regardless of campaign length. Required correct answers, lives, settings,
and UI are unchanged. A single-level campaign deterministically uses target 8/cap 30.

For an eligible candidate, `AcceptancePolicy` accepts with probability
`0.025+0.975*exp(-0.5*((difficulty-target)/19)^2)`. This retains variation; neither
individual questions nor every individual campaign must increase monotonically.
Policy parameters and retry/probe budgets are named configuration values. Difficulty
scores are calculated on demand, never precomputed for the whole space.

## 22. Optional Player Name

The same base message should be used regardless of whether the player name exists.

Examples:

`Great job!`

`Great job, Karolina!`

The name should only be appended when available.

Do not create semantically different messages solely based on whether a name was entered.

## 23. Visual Theme

The entire application uses a Space Adventure theme.

Visual direction:
- deep navy/blue space backgrounds,
- stars,
- colorful planets,
- rocket,
- energy crystals,
- visible rocket energy tank,
- soft glows,
- rounded buttons/cards,
- large child-friendly controls,
- clear typography.

The application should feel colorful, polished and playful without becoming cluttered.

Existing UI mockups are visual references.

If an older mockup conflicts with the current `SPEC.md`, the specification takes precedence.

## 24. Animation Guidelines

Use native Jetpack Compose animations where practical.

Examples:
- translate,
- scale,
- rotate,
- fade,
- shake,
- glow,
- simple particles/confetti.

Important animations now include:
- mechanical arm extending from the rocket,
- arm grabbing a crystal,
- crystal moving into the rocket,
- energy tank gradually filling,
- rocket engine startup,
- rocket launch,
- rocket flight after Level completion.

Correct answers should feel rewarding.

Wrong answers should use gentle feedback.

Level completion should be noticeably more celebratory than an individual correct answer.

Do not introduce a game engine unless there is a clear technical need.

## 25. Scope

The current version does not require:
- user accounts,
- backend,
- cloud synchronization,
- advertisements,
- payments,
- online rankings,
- social features,
- a generic game engine.

The main goal is a polished local game with:
- a persistent campaign,
- a horizontally scrollable Space Map,
- replayable and unlockable Levels,
- configurable Level length,
- crystal collection,
- visible rocket-energy progression,
- a satisfying rocket launch after completing each Level.

## 26. Space Pass — development/testing

### Separate configuration values

Free is the default plan. `SpacePassConfig` centralizes:
- Free allowance: exactly **2 game attempts**.
- Free rest: **5 minutes**, retaining the short testing cooldown.
- Premium custom defaults: **5 attempts** and **1 hour** of rest.
- Maximum custom rest: **8760 whole hours**.

These values are separate from `CampaignConfig.DEFAULT_REQUIRED_CORRECT_ANSWERS` (currently **4**). Do not scatter them through UI or gameplay code. The Free cooldown is triggered by allowance exhaustion, never by a calendar-day boundary.

### When attempts are consumed

Charge and durably store one attempt only when an unlocked level actually starts. Selecting a playable map node starts gameplay; map browsing and locked nodes do not. Retry and replay of an unlocked level use the same start check and each consume one attempt under the active limited plan. Compose recomposition must not consume another attempt.

Completing a level, reaching Game Over, leaving gameplay or killing the app does not refund attempts or charge an additional attempt. Starting the final available attempt stores an epoch cooldown-end timestamp. That active attempt may finish normally; subsequent starts are blocked until expiry. At expiry restore the full active plan allowance and clear its timestamp, including after restart and while the app stays open.

### Development plan selection

Settings contains Free and Premium cards, identifies the selected draft plan and offers `Switch to Premium` / `Switch to Free`. These are development/testing controls, clearly labelled as a local mock with no payment. Selecting a plan takes effect only after the shared `Save Settings` action described in section 3.

Persist the selected plan across restarts. Switching plans preserves campaign progress, name and math settings. Free and Premium custom usage/cooldowns are independent: returning to Free uses its saved allowance and any unexpired Free cooldown. Saved unlimited Premium ignores both limited-plan restrictions. Returning to Premium preserves its previously configured unlimited/custom selection; do not force Unlimited back on each time.

No real billing, subscriptions, purchase verification, Google Play integration, login or network services are implemented. Keep this mock easy to replace or disable when monetization is introduced.

### Premium-only PLAY LIMITS

Show this section only when Premium is selected in the Settings draft. Use a rounded dark space-themed panel and the same typography, spacing and controls as the plan cards.

`Unlimited attempts` is ON by default. Unlimited play consumes no attempts and applies no cooldown. Show `Your child can play without limits.` and disable the custom fields visually and functionally.

When Unlimited is OFF, show `Custom limit` and enable:
- `Attempts before rest`: a positive whole number representable as a Kotlin Int.
- `Rest duration (hours)`: a whole number from 1 to `SpacePassConfig.MAX_REST_HOURS` (currently 8760).

Reject blank, non-integer, zero, negative and overflowing active custom values. Save custom values locally and restore them on reopening Settings or restarting the app. Toggling Unlimited or switching plans preserves saved custom values and usage.

For a saved Premium limit of 5 attempts / 1 hour, five starts are allowed; the fifth starts the one-hour cooldown. The sixth start is blocked. After expiry another five attempts become available. Successful completion does not replenish the allowance.

When saving changes:
- A changed custom attempt allowance starts a fresh Premium cycle with the new full allowance and clears its old custom cooldown.
- Changing rest duration alone preserves remaining attempts and an already running cooldown; the new duration applies to the next exhaustion.
- Toggling Unlimited alone preserves the custom allowance, usage and cooldown. An existing cooldown may expire normally while another plan or Unlimited is active.
- Saving unchanged settings does not reset usage or extend cooldowns.

### Persistence and reset isolation

Persist Free remaining attempts and cooldown, Premium remaining attempts and cooldown, Premium custom values, unlimited selection and selected plan in local Space Pass storage. Commit consumption before navigating into gameplay, so restarting cannot refund an attempt. Use persistent epoch timestamps rather than only an in-memory countdown.

Reset Progress changes only campaign progress, including final-level completion. It does not reset Space Pass or mathematical/player settings.

### No attempts left

Blocked starts open the existing calm rest screen: sleeping astronaut, parked rocket, Zzz, `Great space training today!`, `Come back later for more missions!`, remaining cooldown, `Back to map` and a parent-oriented Settings hint.

The countdown uses the active plan's timestamp and updates while the app is open. Display minutes/seconds for short rests and hours/minutes for longer rests. On expiry show that missions are available again and allow return to the map without restarting. Never interrupt an already running attempt.

## 27. Behavioral verification

Focused automated tests should cover question ranges/operations/repetition, configurable energy and lives, campaign unlocking/replay/final-level boundaries, persistence and reset isolation. Space Pass tests should cover two Free starts, five Premium custom starts followed by a blocked sixth, exact expiry, independent plan usage, Unlimited toggling, invalid limits and restart persistence.

Settings UI regression tests should save the whole form, return to the map, reopen Settings and successfully edit/save custom limits again. Verify that Back discards draft changes, inactive controls are disabled, and saving or recomposition does not consume attempts. Build and run relevant tests after implementation changes; use an available emulator for affected flows.

## 28. Star counting hints

The opening gold question star has a 76 dp touch target on a separate sky row from
Map, left of the expression. Its drawing gently scales 1.0–1.12 and shimmers without
changing layout. Touch targets must not overlap.

Visual tutorial fingers share HINT_IDLE_TIMEOUT_MS = 8,000 ms. Each stage permits
at most three cycles, each with three 900 ms movements, then disappears. A new cycle
waits another eight seconds after the previous cycle ends. Real interaction cancels
the finger and restarts idle time without refunding spent cycles. A held gesture,
pause or backgrounded activity cannot display a finger. Overlays consume no input
and never click, drag, add or dim actual stars. Leaving composition cancels jobs.

During an active question the finger suggests tapping the opening star only when
the hint is available and closed, outside feedback and transitions. Each new question
resets this budget. Interactive hint sessions start with a bag-to-grid demonstration;
the first successful extraction permanently disables it for that session. Subtraction
then independently suggests tapping a lit, non-returning star, until the first real
dimming. Relighting or returning all stars never re-enables a completed tutorial stage.
Without a lit star, the dim tutorial is suspended. Automatic mode has no tutorial finger.
Every new opening resets these temporary milestones and stage budgets.

Mode selection uses one rule over CampaignConfig.levels.size: ceil(total / 3)
automatic levels, with the rest interactive (1/2/3 total: one automatic; 5/6: two;
12: four; 15: five; 30: ten). Returning any star, including a dimmed one, leaves an
empty slot; all other stars retain their chosen positions and light states.

During PLAYING, a gently scaling gold star with a question mark is visible exactly
when settings.max <= 20. It opens a full-screen counting view with the current
expression, the existing single-row keypad and Close hint (X). The ordinary rocket,
crystals and feedback scene are absent while the hint is open. Existing cosmic
background art remains visible. Hint Back closes just the hint; ordinary gameplay
Back retains its existing pause/leave confirmation behavior.

Each opening creates a new temporary StarHintState. No hint state is persisted or
stored in Game, campaign, Space Pass or settings. X/Back preserve the current question,
entered digits, lives, energy and pass usage. The hint cannot generate or grade
questions. Confirm first disposes the hint and its gestures/animations, renders the
ordinary game, then invokes the existing confirm method once on the next frame.
The same success/failure, lives, crystals and completion effects are reused.

The grid has 5 columns and 4 rows. Stable row-major slots and uniform-sized stars
never depend on color or count. Interactive stars own stable, independently chosen slots. Yellow and pink bags remain fixed at the sides;
subtraction has only a yellow bag. Only automatic addition shows glowing group
outlines and numeric labels, including bag counts during travel. All subtraction
hints and all interactive hints omit group outlines and numeric count badges.
Empty slots have faint dashed star outlines.

The first ceil(CampaignConfig.levels.size / 3) levels (at least one) automatically demonstrate: one yellow star per left operand, then one
pink star per right addition operand; movement from sack to slot takes 420 ms with
new starts every 500 ms. Subtraction first draws the left operand, pauses, then
dims the first right-operand stars at 500 ms intervals. Their gold fill fades to
soft gray and their glow disappears over 220 ms, retaining the stars and their slots.
A short touch pulse demonstrates tapping; no red crosses are drawn.
Zero operands are supported. Demonstrations run on the Compose frame clock and
are cancelled when their screen leaves composition.

In all remaining levels, start empty. Drag from either sack into the grid to add one star per
completed gesture, irrespective of operand values. It settles into the nearest free
slot to the release point (ties use row-major order). The dragged star continues
from its release position, with no disappearance or second flight from the sack.
Existing stars can be dragged to another nearest free slot, preserving identity and light.
At most 20 stars total, including stars currently returning to sacks, are reserved.
The 21st gesture does nothing. Drag a star to its matching-color sack to return it
with animation, leaving every other star in its chosen slot. Wrong bags, interrupted and invalid drops return
the star to its slot. Subtraction stars toggle between lit gold and dim gray on tap;
dragging never counts as a tap. Stable IDs preserve dimmed states during moves and returns.
Interactive hints never show group outlines or numeric count badges, even when
colors alternate. Addition star taps do not alter the counting model.

StarHintState.kt contains pure interaction rules and the automatic timeline;
StarHintArt.kt contains scalable bags, stars, grid geometry and group outlines;
StarHintScreen.kt owns transient Compose state, frame-clock animation and gestures.
MainActivity.kt reuses its existing keypad and integrates dismissal/confirmation.
Supported visualization data is validated before opening; recoverable timeline
errors are logged and close only the hint. Coroutine cancellation is propagated.

Unit tests cover ranges, mode boundaries, all grid sizes, timelines, count limits,
overlapping rapid edits, color order, return validation, stable IDs, dimming and
fresh sessions. Device tests use controlled Compose time for animations and gestures,
plus real AppState/keypad checks for availability, X/Back, input preservation, pass
isolation and existing feedback after confirmation in both modes. All prior tests
remain part of regression verification. Final acceptance includes ordinary app
installation/launch and visual/device checks in the same Android Studio checkout.

Interactive gesture tolerance: bag pickup/return targets and the grid drop target extend 0.65 grid cell beyond their visible bounds, clipped to the hint surface. A completed drag must move at least 24 dp after recognition to add or return a star. Taps never add/return stars; tapping a subtraction star still toggles its light. Cancelled or invalid drags preserve the model.

## 29. Final campaign completion story

After the final configured level reaches its configured correct-answer target, persist
campaign completion immediately. Finish existing crystal feedback, tank-to-rocket fuel
transfer, lift-off, celebration loop and departure unchanged. Only the map return is
replaced by the automatic finale. Earlier levels keep their normal completion flow.

FinaleScene contains the configurable 40-second story: JOURNEY 5s; CASTLE 4s; MEETING
4s; GIFT 6s; RESCUE 6s; BOARDING 5s; EARTH 5s; HOME 5s. Show the same game rocket,
a crowned princess in a locked tower, a friendly dragon holding the key, astronaut
landing and offering a magical star, voluntary key exchange, unlocked door and
princess greeting, boarding and goodbye, then blue-green Earth and a safe grassy
landing. Use gentle movement, pose changes, hearts, sparkles and confetti without
violence, abrupt flashing, audio dependency or network access.

The princess uses `finale_princess_sad.png` throughout captivity. Opening the door
at RESCUE progress 0.32 switches her to the existing happy pose, including her last
balcony appearance. Captive movement is a slow subtle sway; joy follows release.
The guardian uses `finale_dragon_grumpy.png` before noticing the star. In GIFT the
star is visible immediately and moves toward him from progress 0.08. At 0.18 he
notices it and dissolves to the happy pose over 0.06 (360ms). Existing reception
and jumping at 0.44, key exchange at 0.54–0.91, moving aside into RESCUE and friendly
goodbye retain their order. No attack or combat is added.
Character rendering removes transparent margins and pose-sheet captions once,
preserves alpha/aspect ratio and anchors visible silhouettes at bottom center.
Astronaut visible height is slightly greater than the princess during rescue,
boarding, landing and victory, independent of PNG margins. Original smiling
resources remain available alongside the two new supplied PNGs.

Use native Compose frame timing and 650ms scene dissolves. Preserve aspect ratios,
transparency and cropped caption-free sprite regions. The original reference rockets
must not replace the gameplay rocket. Avoid duplicate baked-in scenery. Asset origins
are resources/final_animation/kids_math_finale_assets (25); imported assets are
app/src/main/res/drawable-nodpi/finale_*.png. Use native Earth landing scenery to avoid
the different rocket baked into the provided landing illustration.

At the end show MISSION COMPLETE!, You Saved the Princess!, All <configured total>
Levels Completed!, and Back to Space Map. Victory has no timeout. Skip and Android
Back during the story open victory; Back from victory returns to the map. The finale
is not another level and never consumes a Space Pass attempt.

Campaign completion and the reward checkpoint are independent stores. Persist the
pending reward at completion, checkpoint playback each second and on interruption,
and exclude time spent backgrounded. Activity recreation retains playback through
AppState. Process restart resumes the saved checkpoint, including victory; interruption
during the original launch may resume directly at the beginning of the story. Explicit
return to the map clears the pending reward, so ordinary map visits never replay it.
Replaying the final level can earn the ending again. Launch callbacks are guarded
against stale games and duplicate transitions. Disposal cancels the frame coroutine.

Verify normal/final completion and launch ordering, configured duration boundaries,
indefinite victory, skip and Back, map return, persisted final completion and reward,
replay, limited-pass consumption, background/recreation and cold-start restoration.
Inspect the real full sequence on an emulator after installing the deliverable APK.
