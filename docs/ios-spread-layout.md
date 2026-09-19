# iOS: spread layout (iPhone Duo, iPad)

How the iOS app lays out on wide scenes — an open iPhone Duo and iPad — and what it does
with the Duo's hinge. Code lives in `apps/ios/UNESKit/Sources/UNESKit/DesignSystem/Spread/`.

Everything here was measured on the **simulator** (Xcode 27.1 beta 1, iOS 27.1 `24A94401`,
2026-09-18). No hardware existed yet. Re-measure on a newer toolchain before trusting a number.

## The idea

A layout never asks which device it is on. It reads what the scene is handed.

- **`PageLayout`** — `.stack` or `.spread`, from the scene's width and horizontal size class
  (`regular` and ≥ 700pt). Resolved once in `RootView`, published as `\.pageLayout`.
- **`SpreadStack`** — every tab's navigation shell. On `.stack` it is the `NavigationStack` the
  phone always had. On `.spread` it is a `NavigationSplitView`: the tab's root stays on the
  leading page, and the trailing page takes every push. List tabs show `SpreadHint` there
  until something is opened; Hoje and Eu show an overview page instead.
- **`StackState.open(_:)`** — pushes made from a tab's *root* replace the path instead of
  appending. On a stack the root is only tappable with an empty path, so nothing changes; on
  a spread the root stays on screen beside the open page. Pushes made from a pushed page
  still `append`.
- **`DeviceFold`** — the only file naming iOS 27.1 API (`reservedRegions`). It turns the fold
  into a plain value in `\.deviceFold`: a `.spine` (between two pages) or a `.waist` (across
  one page), bent or flat. Nothing else needs an availability check.
- **`FacingPages`** / **`pageColumn()`** — two-page and one-page compositions for screens
  that are not navigation shells (onboarding). They end the leading page on the spine.

## What each posture gets

| Posture | Scene | Layout |
|---|---|---|
| Folded (outer display) | 466 × 678, compact | `.stack` — the phone app, unchanged |
| Open, landscape | 951 × 669, regular | `.spread` — list ≈ 400pt, detail takes the rest |
| Open, landscape, half-open ("book") | same, fold active | `.spread` — the system moves the divider onto the fold (475pt) and keeps both pages 20pt off it via safe-area insets. No app code |
| Open, held upright | 669 × 951, regular | `.stack` — too narrow for two pages |
| Upright, half-open ("laptop") | same, fold active | `.stack` — one uniform scroll across the waist fold, by choice (a top/bottom split of Hoje was built and dropped) |
| iPad | regular, ≥ 700pt | `.spread` |

The system owns the bars. With the status bar down the trailing edge (folded, and open in
landscape) it moves the tab bar and the trailing page's toolbar into that strip by itself;
the leading page keeps a horizontal bar. Toolbar items need a title **and** a symbol to be
eligible — text-only and custom-view items stay horizontal.

Onboarding: Welcome, the intro pager and Sync are two-page compositions (`FacingPages` /
`pageColumn`); Login and Ready stay full width by choice.

## Measured, worth knowing

- The fold is a `.division` reserved region: x 455–495 of 951 open (spine at 475), y 455–495
  of 951 upright. Its `frame` **includes** its 20pt margins. It is `isActive` only while the
  hinge is bent and still reported (inactive) when flat.
- `NavigationSplitView` snaps to a spine fold when bent, but ignores a waist fold entirely
  and shrinks its sidebar to 250pt at 669pt wide — hence `.stack` when upright.
- `ArrangementView(.split)` handles both axes (side by side, or top/bottom around a waist),
  but is iOS 27.1-only; unused for now.
- Reserved regions are reported in the measuring view's own space. `RootView` respects the
  safe area, so upright its origin is y = 82 and the waist reads 373, not 455 — `DeviceFold`
  converts to global. Landscape hides this mistake (top inset 0).
- Replacing the page at a depth (`open`: `[A]` → `[B]`) makes SwiftUI update the old page
  **in place**, so the new page's `.task` never runs and a detail that loads on appear spins
  forever. `SpreadStack` gives every pushed page `.id(ObjectIdentifier(store))`. The preview
  fixtures return one canned discipline detail for any id, so check this with a recording.
- A fixed sidebar width is honoured, so the pages *can* be pinned to the spine when flat. We
  chose the narrower list instead: list smaller than detail when flat, equal pages when bent
  (what Notes and Mail do).
- Running the whole app on fixtures, no credentials or network:
  `SIMCTL_CHILD_SWIFT_DEPENDENCIES_CONTEXT=preview xcrun simctl launch <udid> dev.forcetower.unes.ios`.
  The session is in-memory, so each launch starts at onboarding; deeplinks are inert.

## Known issue

Folding the Duo **while a page is open on a spread** leaves that page with an empty toolbar
capsule and a Back button that ignores taps, until the tab is left and re-entered. It happens
when `SpreadStack` swaps the split view for a freshly built stack whose path is already
populated; a stack that survives the fold is fine, and the main thread is idle. Rebuilding
the shell 600ms after the change did not help. Unresolved as of iOS 27.1 beta 1.

## Not verified

Real hardware. Split View multitasking halves. Right-to-left. Dynamic Type in the strip.
The tent posture. iPad on this branch (layout rules are unit-tested; screens not yet walked).
`ScheduleGridView` (the week-grid Horário) still uses a full-width stack.
