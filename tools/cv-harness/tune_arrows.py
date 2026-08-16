"""
Starting point for tuning ArrowHoleDetector's blob thresholds against real photos.

Mirrors cv/src/main/kotlin/com/archeryscored/cv/arrows/ArrowHoleDetector.kt. Takes the geometry
printed by tune_geometry.py for the same photo (center, radius, face diameter) and reports detected
arrow-hole candidates so precision/recall against hand-labeled ground truth can be measured.

Usage:
    python tune_arrows.py samples/sample_01.jpg --center 512 480 --radius 300 --face-cm 122
"""
import argparse
import math

import cv2
import numpy as np

MIN_ARROW_SHAFT_MM = 5.0
MAX_ARROW_SHAFT_MM = 9.5
MIN_CIRCULARITY = 0.5
FACE_MASK_SHRINK_FACTOR = 0.97


def detect_arrows(image, center, radius_px, face_diameter_cm):
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (3, 3), 0)
    thresh = cv2.adaptiveThreshold(
        blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY_INV, 25, 5
    )

    face_mask = np.zeros_like(thresh)
    cv2.circle(face_mask, (int(center[0]), int(center[1])), int(radius_px * FACE_MASK_SHRINK_FACTOR), 255, -1)
    thresh = cv2.bitwise_and(thresh, face_mask)

    pixels_per_cm = radius_px / (face_diameter_cm / 2)
    min_radius_px = pixels_per_cm * (MIN_ARROW_SHAFT_MM / 10) / 2
    max_radius_px = pixels_per_cm * (MAX_ARROW_SHAFT_MM / 10) / 2 * 1.6
    min_area = math.pi * min_radius_px**2
    max_area = math.pi * max_radius_px**2

    contours, _ = cv2.findContours(thresh, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
    candidates = []
    for contour in contours:
        area = cv2.contourArea(contour)
        if area < min_area or area > max_area:
            continue
        perimeter = cv2.arcLength(contour, True)
        if perimeter <= 0:
            continue
        circularity = 4 * math.pi * area / (perimeter**2)
        if circularity < MIN_CIRCULARITY:
            continue
        m = cv2.moments(contour)
        if m["m00"] == 0:
            continue
        candidates.append((m["m10"] / m["m00"], m["m01"] / m["m00"], circularity))

    return candidates


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("image")
    parser.add_argument("--center", nargs=2, type=float, required=True)
    parser.add_argument("--radius", type=float, required=True)
    parser.add_argument("--face-cm", type=float, required=True)
    args = parser.parse_args()

    image = cv2.imread(args.image)
    if image is None:
        raise SystemExit(f"Could not read {args.image}")

    candidates = detect_arrows(image, tuple(args.center), args.radius, args.face_cm)
    print(f"{len(candidates)} candidate(s):")
    for x, y, circularity in candidates:
        print(f"  ({x:.1f}, {y:.1f}) circularity={circularity:.2f}")


if __name__ == "__main__":
    main()
