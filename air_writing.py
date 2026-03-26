"""
AIR - WRITING  (v2 — upgraded)
────────────────────────────────────────────────────────────────────────────────
Draw in the air using only hand gestures.

UPGRADES vs v1
──────────────
• 1080p capture (1920×1080)
• Threaded camera capture — decouples I/O from the main loop for maximum FPS
• EMA (Exponential Moving Average) fingertip filter — kills jitter
• Spline / curve drawing — buffers smoothed points and renders them as a
  continuous cv2.polylines curve instead of choppy line segments

GESTURES
────────
☝  Index finger up (others down)   →  DRAW   – smooth curve in chosen color
🖐  Open palm (all 4 fingers up)    →  ERASE  – wipes canvas under fingertip
👌  OK sign (thumb ≈ index tip,     →  COLOR  – cycles Green → Blue → Red → …
            middle/ring/pinky up)
   Anything else                   →  IDLE   – move freely, nothing drawn

KEYBOARD
────────
C      Clear the entire canvas
ESC    Quit

First run: auto-downloads hand_landmarker.task (~8 MB) if not present.

Requirements:
    pip install opencv-python mediapipe numpy
"""

import math
import os
import queue
import threading
import time
import urllib.request
import ctypes

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
WINDOW_TITLE = "AIR - WRITING"


def ensure_model() -> None:
    if not os.path.exists(MODEL_PATH):
        print("Downloading hand_landmarker.task …")
        urllib.request.urlretrieve(MODEL_URL, MODEL_PATH)
        print("Download complete.")


def get_screen_size() -> tuple[int, int]:
    """Return primary monitor size in pixels with a safe fallback."""
    try:
        user32 = ctypes.windll.user32
        user32.SetProcessDPIAware()
        return int(user32.GetSystemMetrics(0)), int(user32.GetSystemMetrics(1))
    except Exception:
        return 1366, 768


# ── Tuneable constants ─────────────────────────────────────────────────────────
CAM_W           = 1920
CAM_H           = 1080
CAM_FPS         = 30

LINE_THICKNESS  = 6            # curve stroke width
ERASER_RADIUS   = 80           # larger eraser for 1080p
BAR_HEIGHT      = 64

OK_PX_THRESHOLD = 55           # thumb-index distance (px) to recognise OK
OK_DEBOUNCE     = 30           # frames to ignore OK after one fires

EMA_ALPHA       = 0.40         # EMA smoothing: 0 = max smooth, 1 = no smooth
SPLINE_BUFFER   = 12           # number of recent points kept for curve fitting

FONT            = cv2.FONT_HERSHEY_SIMPLEX

COLOR_NAMES = ["GREEN", "BLUE", "RED"]
PALETTE = {
    "GREEN": (0,   255,   0),
    "BLUE":  (255,  80,   0),
    "RED":   (0,    0,  255),
}

HAND_CONNECTIONS = [
    (0, 1), (1, 2), (2, 3), (3, 4),
    (0, 5), (5, 6), (6, 7), (7, 8),
    (5, 9), (9, 10), (10, 11), (11, 12),
    (9, 13), (13, 14), (14, 15), (15, 16),
    (0, 17), (13, 17), (17, 18), (18, 19), (19, 20),
]

# ══════════════════════════════════════════════════════════════════════════════
# Threaded camera capture
# ══════════════════════════════════════════════════════════════════════════════

class CameraThread(threading.Thread):
    """
    Reads frames from the webcam in a dedicated thread and exposes the latest
    one via `read()`.  The main loop never blocks on camera I/O this way.
    """

    def __init__(self, cam_index: int = 0):
        super().__init__(daemon=True)
        self._cap = cv2.VideoCapture(cam_index)
        self._cap.set(cv2.CAP_PROP_FRAME_WIDTH,  CAM_W)
        self._cap.set(cv2.CAP_PROP_FRAME_HEIGHT, CAM_H)
        self._cap.set(cv2.CAP_PROP_FPS,          CAM_FPS)

        if not self._cap.isOpened():
            raise RuntimeError("Cannot open webcam — check camera index.")

        self._frame: np.ndarray | None = None
        self._lock  = threading.Lock()
        self._stop  = threading.Event()

    def run(self) -> None:
        while not self._stop.is_set():
            ret, frame = self._cap.read()
            if ret:
                with self._lock:
                    self._frame = frame

    def read(self) -> np.ndarray | None:
        with self._lock:
            return None if self._frame is None else self._frame.copy()

    def stop(self) -> None:
        self._stop.set()
        self._cap.release()


# ══════════════════════════════════════════════════════════════════════════════
# EMA fingertip smoother
# ══════════════════════════════════════════════════════════════════════════════

class EMASmoother:
    """Exponential moving average on (x, y) pixel coordinates."""

    def __init__(self, alpha: float = EMA_ALPHA):
        self.alpha = alpha
        self._x: float | None = None
        self._y: float | None = None

    def update(self, pt: tuple) -> tuple:
        x, y = pt
        if self._x is None or self._y is None:
            self._x, self._y = float(x), float(y)
        else:
            self._x = self.alpha * x + (1 - self.alpha) * self._x
            self._y = self.alpha * y + (1 - self.alpha) * self._y
        assert self._x is not None and self._y is not None
        return int(round(self._x)), int(round(self._y))

    def reset(self) -> None:
        self._x = self._y = None


# ══════════════════════════════════════════════════════════════════════════════
# Geometry helpers
# ══════════════════════════════════════════════════════════════════════════════

def lm_px(lm, w: int, h: int) -> tuple:
    return int(lm.x * w), int(lm.y * h)


def dist_px(p1: tuple, p2: tuple) -> float:
    return math.hypot(p1[0] - p2[0], p1[1] - p2[1])


def is_finger_up(lms, tip: int, pip: int) -> bool:
    return lms[tip].y < lms[pip].y


# ══════════════════════════════════════════════════════════════════════════════
# Gesture detector
# ══════════════════════════════════════════════════════════════════════════════

def detect_gesture(lms, w: int, h: int) -> str:
    index_up = is_finger_up(lms,  8,  6)
    mid_up   = is_finger_up(lms, 12, 10)
    ring_up  = is_finger_up(lms, 16, 14)
    pinky_up = is_finger_up(lms, 20, 18)

    thumb_tip        = lm_px(lms[4], w, h)
    index_tip        = lm_px(lms[8], w, h)
    thumb_index_dist = dist_px(thumb_tip, index_tip)

    if thumb_index_dist < OK_PX_THRESHOLD and mid_up and ring_up and pinky_up:
        return "OK"
    if index_up and mid_up and ring_up and pinky_up:
        return "ERASE"
    if index_up and not mid_up and not ring_up and not pinky_up:
        return "DRAW"
    return "IDLE"


# ══════════════════════════════════════════════════════════════════════════════
# Skeleton drawing
# ══════════════════════════════════════════════════════════════════════════════

def draw_hand_skeleton(frame: np.ndarray, lms, w: int, h: int) -> None:
    pts = [lm_px(lm, w, h) for lm in lms]
    for a, b in HAND_CONNECTIONS:
        cv2.line(frame, pts[a], pts[b], (255, 0, 220), 2, cv2.LINE_AA)
    for pt in pts:
        cv2.circle(frame, pt, 6, (0, 0, 220), -1, cv2.LINE_AA)


# ══════════════════════════════════════════════════════════════════════════════
# Spline / curve drawing
# ══════════════════════════════════════════════════════════════════════════════

def draw_curve(canvas: np.ndarray,
               pts: list,
               color: tuple,
               thickness: int) -> None:
    """
    Draw a smooth polyline through `pts` using cv2.polylines.
    With enough buffered points this approximates a spline.
    """
    if len(pts) < 2:
        return
    arr = np.array(pts, dtype=np.int32).reshape((-1, 1, 2))
    cv2.polylines(canvas, [arr], isClosed=False, color=color,
                  thickness=thickness, lineType=cv2.LINE_AA)


# ══════════════════════════════════════════════════════════════════════════════
# UI bar
# ══════════════════════════════════════════════════════════════════════════════

_MODE_BG = {
    "DRAW":  (30, 180,  30),
    "ERASE": (20, 140, 220),
    "IDLE":  (80,  80,  80),
}


def render_bar(img: np.ndarray, mode: str, color_name: str, fps: float) -> None:
    h, w = img.shape[:2]
    overlay = img.copy()
    cv2.rectangle(overlay, (0, 0), (w, BAR_HEIGHT), (20, 20, 20), -1)
    cv2.addWeighted(overlay, 0.80, img, 0.20, 0, img)

    ty = BAR_HEIGHT // 2 + 10

    # MODE badge
    mode_bg = _MODE_BG.get(mode, (80, 80, 80))
    cv2.rectangle(img, (8, 6), (145, BAR_HEIGHT - 6), mode_bg, -1)
    (tw, _), _ = cv2.getTextSize(mode, FONT, 0.80, 2)
    cv2.putText(img, mode, (8 + (137 - tw) // 2, ty),
                FONT, 0.80, (0, 0, 0), 2, cv2.LINE_AA)

    # COLOR badge
    col_bg = PALETTE.get(color_name, (200, 200, 200))
    cv2.rectangle(img, (155, 6), (305, BAR_HEIGHT - 6), col_bg, -1)
    (tw, _), _ = cv2.getTextSize(color_name, FONT, 0.75, 2)
    cv2.putText(img, color_name, (155 + (150 - tw) // 2, ty),
                FONT, 0.75, (0, 0, 0), 2, cv2.LINE_AA)

    # Hints
    hints = "INDEX=Draw   PALM=Erase   OK=Cycle Color   C=Clear   ESC=Quit"
    cv2.putText(img, hints, (318, ty), FONT, 0.42, (160, 160, 160), 1, cv2.LINE_AA)

    # FPS counter (top-right)
    fps_txt = f"{fps:.0f} FPS"
    (fw, _), _ = cv2.getTextSize(fps_txt, FONT, 0.52, 1)
    cv2.putText(img, fps_txt, (w - fw - 12, ty),
                FONT, 0.52, (0, 220, 100), 1, cv2.LINE_AA)


# ══════════════════════════════════════════════════════════════════════════════
# Main
# ══════════════════════════════════════════════════════════════════════════════

def main() -> None:
    ensure_model()
    cv2.namedWindow(WINDOW_TITLE, cv2.WINDOW_NORMAL)

    # ── Shared state for async MediaPipe callback ────────────────────────────
    latest_result = None
    result_lock   = threading.Lock()

    def on_result(result, _output_image, _timestamp_ms):
        nonlocal latest_result
        with result_lock:
            latest_result = result

    # ── HandLandmarker in LIVE_STREAM mode ───────────────────────────────────
    base_options = mp_python.BaseOptions(model_asset_path=MODEL_PATH)
    options = mp_vision.HandLandmarkerOptions(
        base_options=base_options,
        running_mode=mp_vision.RunningMode.LIVE_STREAM,
        num_hands=1,
        min_hand_detection_confidence=0.65,
        min_hand_presence_confidence=0.65,
        min_tracking_confidence=0.65,
        result_callback=on_result,
    )
    landmarker = mp_vision.HandLandmarker.create_from_options(options)

    # ── Threaded camera ──────────────────────────────────────────────────────
    cam = CameraThread(cam_index=0)
    cam.start()
    time.sleep(0.5)          # let the camera thread warm up

    # ── App state ────────────────────────────────────────────────────────────
    color_idx    = 0
    color_name   = COLOR_NAMES[color_idx]
    draw_color   = PALETTE[color_name]
    mode         = "IDLE"
    canvas       = None
    ok_cool      = 0

    smoother     = EMASmoother(alpha=EMA_ALPHA)
    curve_buffer : list = []   # recent smoothed points for the current stroke

    # FPS tracking
    fps_time     = time.monotonic()
    fps_counter  = 0
    fps_display  = 0.0

    start_time   = time.monotonic()
    screen_w, screen_h = get_screen_size()
    display_size: tuple[int, int] | None = None

    try:
        while True:
            frame = cam.read()
            if frame is None:
                continue

            frame = cv2.flip(frame, 1)
            h, w  = frame.shape[:2]

            if display_size is None:
                max_w = max(320, screen_w - 80)
                max_h = max(240, screen_h - 140)
                scale = min(1.0, max_w / w, max_h / h)
                display_size = (max(1, int(w * scale)), max(1, int(h * scale)))
                cv2.resizeWindow(WINDOW_TITLE, display_size[0], display_size[1])

            if canvas is None:
                canvas = np.zeros((h, w, 3), dtype=np.uint8)

            # ── FPS counter ──────────────────────────────────────────────────
            fps_counter += 1
            now          = time.monotonic()
            if now - fps_time >= 1.0:
                fps_display = fps_counter / (now - fps_time)
                fps_counter = 0
                fps_time    = now

            # ── Send to MediaPipe async ──────────────────────────────────────
            timestamp_ms = int((now - start_time) * 1000)
            mp_image     = mp.Image(
                image_format=mp.ImageFormat.SRGB,
                data=cv2.cvtColor(frame, cv2.COLOR_BGR2RGB),
            )
            landmarker.detect_async(mp_image, timestamp_ms)

            # ── Consume latest result ────────────────────────────────────────
            with result_lock:
                current_result = latest_result

            gesture          = "IDLE"
            raw_fingertip    = None
            smooth_fingertip = None

            if current_result and current_result.hand_landmarks:
                lms = current_result.hand_landmarks[0]

                draw_hand_skeleton(frame, lms, w, h)

                gesture       = detect_gesture(lms, w, h)
                raw_fingertip = lm_px(lms[8], w, h)

                # EMA smoothing
                smooth_fingertip = smoother.update(raw_fingertip)

            # ── Gesture logic ────────────────────────────────────────────────
            if ok_cool > 0:
                ok_cool -= 1

            if gesture == "OK" and ok_cool == 0:
                color_idx  = (color_idx + 1) % len(COLOR_NAMES)
                color_name = COLOR_NAMES[color_idx]
                draw_color = PALETTE[color_name]
                ok_cool    = OK_DEBOUNCE
                mode       = "IDLE"
                smoother.reset()
                curve_buffer.clear()

            elif gesture == "DRAW" and smooth_fingertip:
                mode = "DRAW"

                # Accumulate points for the current stroke
                curve_buffer.append(smooth_fingertip)
                if len(curve_buffer) > SPLINE_BUFFER:
                    curve_buffer.pop(0)

                # Draw the curve through the buffered window
                draw_curve(canvas, curve_buffer, draw_color, LINE_THICKNESS)

            elif gesture == "ERASE" and smooth_fingertip:
                mode = "ERASE"
                smoother.reset()
                curve_buffer.clear()
                cv2.circle(canvas, smooth_fingertip, ERASER_RADIUS, (0, 0, 0), -1)

            else:
                mode = "IDLE"
                smoother.reset()
                curve_buffer.clear()

            # ── Composite canvas onto live feed ───────────────────────────────
            gray     = cv2.cvtColor(canvas, cv2.COLOR_BGR2GRAY)
            _, mask  = cv2.threshold(gray, 10, 255, cv2.THRESH_BINARY)
            mask_inv = cv2.bitwise_not(mask)
            frame    = cv2.add(
                cv2.bitwise_and(frame, frame, mask=mask_inv),
                cv2.bitwise_and(canvas, canvas, mask=mask),
            )

            # ── UI ────────────────────────────────────────────────────────────
            render_bar(frame, mode, color_name, fps_display)

            if mode == "ERASE" and smooth_fingertip:
                cv2.circle(frame, smooth_fingertip, ERASER_RADIUS,
                           (100, 100, 100), 2, cv2.LINE_AA)

            show_frame = frame
            if display_size and display_size != (w, h):
                show_frame = cv2.resize(frame, display_size, interpolation=cv2.INTER_AREA)

            cv2.imshow(WINDOW_TITLE, show_frame)

            key = cv2.waitKey(1) & 0xFF
            if key == 27:
                break
            elif key in (ord("c"), ord("C")):
                canvas = np.zeros((h, w, 3), dtype=np.uint8)
                smoother.reset()
                curve_buffer.clear()

    finally:
        cam.stop()
        landmarker.close()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    main()