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

## Navigation flow

`Home` → `New Session` (pick round/distance/face) → `Session` (per-session dashboard: total score,
grouping/progression charts, the end-by-end list, and a **Capture end** button) → `Capture` (camera,
reached only by explicitly tapping Capture end — it no longer opens automatically) → `Review`
(correct detected/tapped arrows, **Save end**) → back to `Session`.

A session with no `endedAt` timestamp is "in progress" and can always be reopened from `Home` to add
more ends; tapping **Finish session** on the Session screen (with a confirmation dialog) sets that
timestamp and hides the Capture/Finish actions from then on, leaving the session as a read-only record.

On `Capture`, an end's photo can come from either the camera or the system Photo Picker (an "upload"
button next to the shutter FAB, and the only option shown if camera permission isn't granted) — both
paths converge on the same `createEnd`/auto-detect/Review pipeline. Uploaded images are decoded and
re-encoded as an upright JPEG (EXIF orientation applied) before saving, since Bitmap/OpenCV don't
honor EXIF rotation on their own and calibration assumes pixel coordinates match what's displayed.

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

- Manual calibration is a two-tap flow (tap center, tap edge) rather than draggable handles — chosen
  for lower gesture-conflict risk given this was built without on-device testing.
- Multi-spot faces (3-spot) use the same single-circle detection/calibration as single-spot faces;
  the UI doesn't yet distinguish separate spots within one end.
- No charting library was added; the grouping and progression charts are hand-rolled Compose `Canvas`,
  which was already the intended design (see the plan this was built from) since neither is a
  standard chart type a library would provide directly.
