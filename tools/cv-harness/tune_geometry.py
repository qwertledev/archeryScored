"""
Starting point for tuning TargetFaceDetector's HSV ring-color ranges against real photos.

Mirrors cv/src/main/kotlin/com/archeryscored/cv/geometry/TargetFaceDetector.kt so tuned values can be
copied straight back. Run against a folder of sample photos and inspect the printed (center, radius)
per ring color, or the saved mask images, to see where the ranges need adjusting.

Usage:
    python tune_geometry.py samples/sample_01.jpg
"""
import sys
from pathlib import Path

import cv2
import numpy as np

# Keep this in sync with TargetFaceDetector.hsvRanges (OpenCV Java/Kotlin uses the same 0-180 hue,
# 0-255 sat/val ranges as opencv-python, so these numbers copy over directly).
HSV_RANGES = {
    "GOLD": [((15, 80, 120), (35, 255, 255))],
    "RED": [((0, 90, 60), (10, 255, 255)), ((170, 90, 60), (180, 255, 255))],
    "BLUE": [((90, 60, 60), (130, 255, 255))],
    "BLACK": [((0, 0, 0), (180, 120, 60))],
    "WHITE": [((0, 0, 170), (180, 60, 255))],
}

WORKING_MAX_DIMENSION = 1200
MIN_CONTOUR_AREA = 200


def detect_ring(hsv: np.ndarray, ranges) -> tuple[tuple[float, float], float] | None:
    mask = np.zeros(hsv.shape[:2], dtype=np.uint8)
    for lower, upper in ranges:
        mask |= cv2.inRange(hsv, np.array(lower), np.array(upper))
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, None)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, None)

    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        return None
    best = max(contours, key=cv2.contourArea)
    if cv2.contourArea(best) < MIN_CONTOUR_AREA:
        return None
    (x, y), radius = cv2.minEnclosingCircle(best)
    return (x, y), radius


def main(image_path: str) -> None:
    image = cv2.imread(image_path)
    if image is None:
        raise SystemExit(f"Could not read {image_path}")

    longest_side = max(image.shape[:2])
    scale = WORKING_MAX_DIMENSION / longest_side if longest_side > WORKING_MAX_DIMENSION else 1.0
    working = cv2.resize(image, None, fx=scale, fy=scale) if scale < 1.0 else image.copy()
    hsv = cv2.cvtColor(working, cv2.COLOR_BGR2HSV)

    for color, ranges in HSV_RANGES.items():
        result = detect_ring(hsv, ranges)
        if result is None:
            print(f"{color:6s}: not found")
            continue
        (x, y), radius = result
        print(f"{color:6s}: center=({x / scale:.1f}, {y / scale:.1f}) radius={radius / scale:.1f}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {Path(__file__).name} <image_path>")
    main(sys.argv[1])
