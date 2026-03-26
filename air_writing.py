"""
AIR - WRITING
─────────────────────────────────────────────────────────────────────────────
Draw in the air using only hand gestures — powered by MediaPipe Tasks API.

GESTURES
────────
☝  Index finger up (others down)   →  DRAW   – traces fingertip in chosen color
🖐  Open palm (all 4 fingers up)    →  ERASE  – wipes canvas under fingertip
👌  OK sign (thumb ≈ index tip,     →  COLOR  – cycles Green → Blue → Red → Green
            middle/ring/pinky up)
   Anything else                   →  IDLE   – move freely, nothing drawn

KEYBOARD
────────
C      Clear the entire canvas
ESC    Quit

First run: the script auto-downloads hand_landmarker.task (~8 MB) if missing.

Requirements:
    pip install opencv-python mediapipe numpy
"""

import math
import os
import threading
import time
import urllib.request

import cv2
import mediapipe as mp
import numpy as np
from mediapipe.tasks import python as mp_python
from mediapipe.tasks.python import vision as mp_vision

# ── Model ──────────────────────────────────────────────────────────────────────
MODEL_PATH = "hand_landmarker.task"
MODEL_URL  = (
    "https://storage.googleapis.com/mediapipe-models/"
    "hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
)


def ensure_model() -> None:
    if not os.path.exists(MODEL_PATH):
        print(f"Downloading hand_landmarker.task from Google …")
        urllib.request.urlretrieve(MODEL_URL, MODEL_PATH)
        print("Download complete.")


# ── Tuneable constants ─────────────────────────────────────────────────────────
LINE_THICKNESS  = 5
ERASER_RADIUS   = 60
BAR_HEIGHT      = 58
OK_PX_THRESHOLD = 45          # pixel distance thumb-tip ↔ index-tip for OK
OK_DEBOUNCE     = 30          # frames to ignore OK after one fires
FONT            = cv2.FONT_HERSHEY_SIMPLEX

# Color cycle (BGR)
COLOR_NAMES = ["GREEN", "BLUE", "RED"]
PALETTE = {
    "GREEN": (0,   255,   0),
    "BLUE":  (255,  80,   0),
    "RED":   (0,    0,  255),
}

# Hand skeleton connections (same indices as MediaPipe landmark map)
HAND_CONNECTIONS = [
    (0, 1), (1, 2), (2, 3), (3, 4),          # thumb
    (0, 5), (5, 6), (6, 7), (7, 8),           # index
    (5, 9), (9, 10), (10, 11), (11, 12),      # middle
    (9, 13), (13, 14), (14, 15), (15, 16),    # ring
    (0, 17), (13, 17), (17, 18), (18, 19), (19, 20),  # pinky
]

# ── Geometry helpers ───────────────────────────────────────────────────────────

def lm_px(lm, w: int, h: int) -> tuple:
    """Normalised landmark → pixel coords."""
    return int(lm.x * w), int(lm.y * h)


def dist_px(p1: tuple, p2: tuple) -> float:
    return math.hypot(p1[0] - p2[0], p1[1] - p2[1])


def is_finger_up(lms, tip: int, pip: int) -> bool:
    """True when the tip landmark is above (smaller y) its PIP joint."""
    return lms[tip].y < lms[pip].y


# ── Gesture detector ───────────────────────────────────────────────────────────

def detect_gesture(lms, w: int, h: int) -> str:
    """
    Priority order: OK  >  ERASE  >  DRAW  >  IDLE
    Returns one of: "OK", "ERASE", "DRAW", "IDLE"
    """
    index_up = is_finger_up(lms,  8,  6)
    mid_up   = is_finger_up(lms, 12, 10)
    ring_up  = is_finger_up(lms, 16, 14)
    pinky_up = is_finger_up(lms, 20, 18)

    thumb_tip        = lm_px(lms[4], w, h)
    index_tip        = lm_px(lms[8], w, h)
    thumb_index_dist = dist_px(thumb_tip, index_tip)

    # OK: thumb close to index, other three fingers extended
    if thumb_index_dist < OK_PX_THRESHOLD and mid_up and ring_up and pinky_up:
        return "OK"

    # Open palm: all four fingers extended
    if index_up and mid_up and ring_up and pinky_up:
        return "ERASE"

    # Only index finger extended
    if index_up and not mid_up and not ring_up and not pinky_up:
        return "DRAW"

    return "IDLE"


# ── Skeleton drawing ───────────────────────────────────────────────────────────

def draw_hand_skeleton(frame: np.ndarray, lms, w: int, h: int) -> None:
    """Draw red joint dots and magenta bone lines onto frame."""
    pts = [lm_px(lm, w, h) for lm in lms]

    # Connections
    for a, b in HAND_CONNECTIONS:
        cv2.line(frame, pts[a], pts[b], (255, 0, 220), 2, cv2.LINE_AA)

    # Joints
    for pt in pts:
        cv2.circle(frame, pt, 5, (0, 0, 220), -1, cv2.LINE_AA)


# ── UI bar ─────────────────────────────────────────────────────────────────────
_MODE_BG = {
    "DRAW":  (30, 180,  30),
    "ERASE": (20, 140, 220),
    "IDLE":  (80,  80,  80),
}


def render_bar(img: np.ndarray, mode: str, color_name: str) -> None:
    """Informational top bar — reflects state only, not interactive."""
    h, w = img.shape[:2]

    overlay = img.copy()
    cv2.rectangle(overlay, (0, 0), (w, BAR_HEIGHT), (20, 20, 20), -1)
    cv2.addWeighted(overlay, 0.80, img, 0.20, 0, img)

    ty = BAR_HEIGHT // 2 + 9

    # MODE badge
    mode_bg = _MODE_BG.get(mode, (80, 80, 80))
    cv2.rectangle(img, (8, 6), (130, BAR_HEIGHT - 6), mode_bg, -1)
    (tw, _), _ = cv2.getTextSize(mode, FONT, 0.72, 2)
    cv2.putText(img, mode, (8 + (122 - tw) // 2, ty),
                FONT, 0.72, (0, 0, 0), 2, cv2.LINE_AA)

    # COLOR badge
    col_bg = PALETTE.get(color_name, (200, 200, 200))
    cv2.rectangle(img, (140, 6), (270, BAR_HEIGHT - 6), col_bg, -1)
    (tw, _), _ = cv2.getTextSize(color_name, FONT, 0.68, 2)
    cv2.putText(img, color_name, (140 + (130 - tw) // 2, ty),
                FONT, 0.68, (0, 0, 0), 2, cv2.LINE_AA)

    # Hint text
    hints = "INDEX=Draw   PALM=Erase   OK=Cycle Color   C=Clear   ESC=Quit"
    cv2.putText(img, hints, (282, ty), FONT, 0.38, (160, 160, 160), 1, cv2.LINE_AA)


# ── Main ───────────────────────────────────────────────────────────────────────

def main() -> None:
    ensure_model()

    # ── Shared state for async callback ─────────────────────────────────────
    latest_result      = None
    result_lock        = threading.Lock()

    def on_result(result, _output_image, _timestamp_ms):
        nonlocal latest_result
        with result_lock:
            latest_result = result

    # ── Build HandLandmarker (LIVE_STREAM mode) ──────────────────────────────
    base_options = mp_python.BaseOptions(model_asset_path=MODEL_PATH)
    options = mp_vision.HandLandmarkerOptions(
        base_options=base_options,
        running_mode=mp_vision.RunningMode.LIVE_STREAM,
        num_hands=1,
        min_hand_detection_confidence=0.6,
        min_hand_presence_confidence=0.6,
        min_tracking_confidence=0.6,
        result_callback=on_result,
    )
    landmarker = mp_vision.HandLandmarker.create_from_options(options)

    # ── App state ────────────────────────────────────────────────────────────
    color_idx  = 0
    color_name = COLOR_NAMES[color_idx]
    draw_color = PALETTE[color_name]
    mode       = "IDLE"
    prev_pt    = None
    canvas     = None
    ok_cool    = 0             # debounce counter

    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        raise RuntimeError("Could not open webcam — check your camera index.")

    start_time = time.monotonic()

    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                continue

            frame = cv2.flip(frame, 1)
            h, w  = frame.shape[:2]

            if canvas is None:
                canvas = np.zeros((h, w, 3), dtype=np.uint8)

            # ── Send frame to MediaPipe (async) ──────────────────────────────
            timestamp_ms = int((time.monotonic() - start_time) * 1000)
            mp_image     = mp.Image(
                image_format=mp.ImageFormat.SRGB,
                data=cv2.cvtColor(frame, cv2.COLOR_BGR2RGB),
            )
            landmarker.detect_async(mp_image, timestamp_ms)

            # ── Read latest result (may lag one frame — perfectly fine) ───────
            with result_lock:
                current_result = latest_result

            gesture   = "IDLE"
            fingertip = None

            if current_result and current_result.hand_landmarks:
                lms = current_result.hand_landmarks[0]   # first hand

                draw_hand_skeleton(frame, lms, w, h)

                gesture   = detect_gesture(lms, w, h)
                fingertip = lm_px(lms[8], w, h)

            # ── Gesture logic ────────────────────────────────────────────────
            if ok_cool > 0:
                ok_cool -= 1

            if gesture == "OK" and ok_cool == 0:
                color_idx  = (color_idx + 1) % len(COLOR_NAMES)
                color_name = COLOR_NAMES[color_idx]
                draw_color = PALETTE[color_name]
                ok_cool    = OK_DEBOUNCE
                mode       = "IDLE"
                prev_pt    = None

            elif gesture == "DRAW" and fingertip:
                mode = "DRAW"
                if prev_pt:
                    cv2.line(canvas, prev_pt, fingertip, draw_color, LINE_THICKNESS)
                prev_pt = fingertip

            elif gesture == "ERASE" and fingertip:
                mode    = "ERASE"
                prev_pt = None
                cv2.circle(canvas, fingertip, ERASER_RADIUS, (0, 0, 0), -1)

            else:
                mode    = "IDLE"
                prev_pt = None

            # ── Composite canvas onto live feed ───────────────────────────────
            gray       = cv2.cvtColor(canvas, cv2.COLOR_BGR2GRAY)
            _, mask    = cv2.threshold(gray, 10, 255, cv2.THRESH_BINARY)
            mask_inv   = cv2.bitwise_not(mask)
            frame      = cv2.add(
                cv2.bitwise_and(frame, frame, mask=mask_inv),
                cv2.bitwise_and(canvas, canvas, mask=mask),
            )

            # ── UI ────────────────────────────────────────────────────────────
            render_bar(frame, mode, color_name)

            if mode == "ERASE" and fingertip:
                cv2.circle(frame, fingertip, ERASER_RADIUS, (100, 100, 100), 2)

            cv2.imshow("AIR - WRITING", frame)

            key = cv2.waitKey(1) & 0xFF
            if key == 27:                         # ESC → quit
                break
            elif key in (ord("c"), ord("C")):     # C → clear
                canvas  = np.zeros((h, w, 3), dtype=np.uint8)
                prev_pt = None

    finally:
        cap.release()
        landmarker.close()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    main()