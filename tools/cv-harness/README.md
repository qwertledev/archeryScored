# CV tuning harness

The on-device detectors in `cv/` (`TargetFaceDetector`, `ArrowHoleDetector`) were written against the
real OpenCV Android API and compile cleanly, but their HSV thresholds and blob-size heuristics have
**not been tuned or validated against real target photos** — there was no photo corpus available while
building this project. Treat auto-detection as unproven until you've run it through this workflow.

## Why a separate Python harness

`opencv-python`'s API is near-identical to the Java/Kotlin bindings used in `cv/` (same underlying
native functions, different language surface), so tuning here translates directly back into
`TargetFaceDetector.kt` / `ArrowHoleDetector.kt`. Iterating in a Jupyter notebook with slider widgets
is far faster than rebuilding and re-flashing the Android app for every threshold tweak.

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
jupyter notebook
```

## Workflow

1. Collect 20-30 real target photos: multiple face types, indoor/outdoor lighting, some with a few
   arrows already in, some with a full end. Put them in `tools/cv-harness/samples/` (gitignored).
2. For each sample, hand-record the ground truth: face type, and pixel coordinates of the true center,
   outer edge, and every arrow hole. Save alongside the photo (e.g. `sample_01.json`).
3. Use `tune_geometry.py` to interactively adjust the HSV ranges in `TargetFaceDetector.hsvRanges`
   until the fitted center/radius match ground truth across the sample set.
4. Use `tune_arrows.py` similarly for the blob-detection thresholds in `ArrowHoleDetector`.
5. Port the tuned constants back into the Kotlin files — the structure is deliberately mirrored so
   this is a copy of a few numbers, not a rewrite.
6. Track precision/recall on the arrow detector specifically (missed holes vs. false positives) —
   that number is what tells you whether auto-detection is worth defaulting to, versus starting users
   on manual calibration until it's proven out.

`tune_geometry.py` and `tune_arrows.py` are intentionally left as thin starting points below, not a
finished tool — build them out once you have real sample photos to point them at.
