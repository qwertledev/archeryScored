# ArcheryScored

An Android app for scoring archery practice sessions from photos: photograph the target after each
end, get an automatic (best-effort) score and arrow-position reading with a manual correction step,
and see a full session summary — total score, score progression, and a grouping chart of every arrow
shot overlaid on the target face.

## Status

Built and **verified compiling end-to-end** (`./gradlew :app:assembleDebug` succeeds, producing a
real debug APK) in an environment with no Android emulator/device available, so the manual-flow UI
and CV pipeline have not been exercised on-device yet. Before relying on this:

- **Manual flow (calibrate → tap arrows → score → summary) is the reliable path.** It has no CV
  dependency and should work as soon as you run the app.
- **Automatic detection (`cv/` module) is unproven.** It's real OpenCV code, compiles against the
  actual `org.opencv:opencv` Android artifact, and is wired into the capture flow — but its HSV
  thresholds and blob-size heuristics were written from domain knowledge, not tuned against real
  target photos (none were available while building this). See `tools/cv-harness/README.md` for the
  tuning workflow before trusting it. Every auto-detected point is editable/deletable on the Review
  screen, and if geometry detection fails or returns low confidence, the app transparently falls back
  to the two-tap manual calibration flow — auto-detection is never a hard dependency.
- Field-face (6-ring) and indoor 3-spot ring geometry (`core-model/.../TargetFaces.kt`) are
  **approximated, not verified** against the official World Archery rulebook.
- No launcher icon polish beyond a simple vector target mark; no app-level tests beyond the
  `core-model` scoring-math unit tests.
- The Room schema uses `fallbackToDestructiveMigration()` (pre-1.0, no real user data to preserve
  yet) — installing a new debug build over an older one after a schema change wipes local session
  data rather than crashing. Worth replacing with a real migration once this has real users.
- The manual-entry palette (Miss/1-9/10/X) is derived from the session's `RingConfig`, so it already
  adapts to any face's max score and whether it has an X-ring - but only the four standard WA sizes
  are actually selectable from New Session today, so it's only been exercised against those.

## Navigation flow

`Home` → `New Session` (pick round/distance/face) → `Session` (per-session dashboard: total score,
grouping/progression charts, the end-by-end list, and an **Add end** button) → `Add End` (no camera/
permission touched yet). This screen always shows all the ways to record an end at once, no picking
a mode first:

- **Take a picture** → `Capture` (camera only) → `Review` (correct detected/tapped arrows, **Save end**)
- **Use a picture** → system Photo Picker, straight into the same auto-detect/`Review` pipeline
- **Tap on a target** → `Diagram Entry` (a blank rendered target face, tap where each arrow landed) →
  saves straight back to `Session` - no photo, but a real position, so it plots on the grouping chart
- **Quick score entry** - a Miss-through-X palette embedded directly on the `Add End` screen itself
  (not a separate destination), for when a photo/diagram is overkill and you just want the number in

All routes converge back on `Session`. Quick/numeric entries have no photo and no arrow position -
they count toward the total score and the progression chart, but are skipped by the grouping chart
(which plots position, not just score) since there's no position to plot. Diagram-tap ends *do* have
a position (relative to the diagram's own fixed circle) and do show up there.

Every end is capped at `MAX_ARROWS_PER_END` (3, in `core-model/.../EndRules.kt`), enforced the same
way everywhere an arrow gets added - the quick-entry palette disables once full, Diagram Entry and
Review's tap-to-add both stop accepting new taps, and CV auto-detection keeps only the top 3 results
by confidence if it finds more (false positives being far likelier than a genuine fourth arrow).

On `Review`, the calibration circle is never a separate "step" - it's always visible from the moment
the photo loads (auto-detected, previously saved, or a computed default centered on the photo), with
two large icon handles: move (center) and resize (edge). Dragging either live-rescoring every placed
arrow, since score depends on distance from center relative to radius. "Reset circle" snaps back to
the computed default. Arrow marks themselves are large filled circles (not just text) and render
offset to the left of the actual touch point while being placed/dragged, specifically so the mark is
never hidden under the finger placing it - the offset position is what's recorded, not the raw touch.

Target faces (`Diagram Entry` and the grouping chart) draw a boundary stroke between every individual
scoring ring, not just between the 5 fill colors - the 10 and 9 rings both being GOLD, for instance,
would otherwise blend into one solid blob with no visible seam between them. `Diagram Entry` also
labels each band with its score number (`app/ui/common/TargetFaceDrawing.kt:drawTargetFace`).

A session with no `endedAt` timestamp is "in progress" and can always be reopened from `Home` to add
more ends; tapping **Finish session** on the Session screen (with a confirmation dialog) sets that
timestamp and hides the Add end/Finish actions from then on, leaving the session as a read-only record.

Uploaded images are decoded and re-encoded as an upright JPEG, downsampled to a bounded resolution
(EXIF orientation applied) before saving, since Bitmap/OpenCV don't honor EXIF rotation on their own,
calibration assumes pixel coordinates match what's displayed, and a naive full-resolution decode of an
arbitrary gallery photo (48MP+ on modern phones) was crashing the app outright via an OS-level
low-memory kill - not something a try/catch can intercept.

## Project structure

```
core-model/   pure Kotlin - RingConfig, TargetFaceType, ScoreCalculator, GroupStats (unit tested)
core-data/    Android library - Room schema, repository, app-private photo storage
cv/           Android library - OpenCV-based target/arrow detection (best-effort, see Status)
app/          the Android app - Compose UI, navigation, CameraX, Hilt DI
tools/cv-harness/   Python/opencv-python harness for tuning the cv/ module against real photos
```

## Building

Requires JDK 17 and the Android SDK (compileSdk 35, build-tools 35.0.0). Easiest path is opening
the project root in Android Studio (Ladybug or newer) and letting it sync — it will fetch anything
missing automatically.

From the command line:

```bash
./gradlew :app:assembleDebug
```

`local.properties` (gitignored) must contain `sdk.dir=<path to your Android SDK>`.

Run the pure-Kotlin scoring tests (no Android SDK needed):

```bash
./gradlew :core-model:test
```

## App architecture

MVVM: Compose screens read `StateFlow` from Hilt `ViewModel`s; a repository (`core-data`) wraps Room
+ photo file storage; CV/scoring logic are plain suspend/pure functions, never called directly from
Composables. Photos live in app-private internal storage (`filesDir/photos/...`) — no runtime storage
permission needed, never visible in the device Gallery. Arrow points are stored **normalized**
(relative to that end's calibrated center/radius) specifically so the session-wide grouping chart can
overlay arrows from different photos without reconciling per-photo resolution.

See `core-model/.../RingConfig.kt` and `TargetFaces.kt` for the ring-ratio config that drives both
scoring math and the CV color-search sequence — adding a target face type is new config there, not
new detection code.

## Known simplifications vs. a v2

- Multi-spot faces (3-spot) use the same single-circle detection/calibration as single-spot faces;
  the UI doesn't yet distinguish separate spots within one end.
- No charting library was added; the grouping and progression charts are hand-rolled Compose `Canvas`,
  which was already the intended design (see the plan this was built from) since neither is a
  standard chart type a library would provide directly.
