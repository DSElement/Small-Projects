# Air Writing - Hand Gesture Drawing

A Python application that lets you draw in the air using hand gestures detected via webcam.

## Features

- **Hand Tracking**: Real-time hand landmark detection using MediaPipe with live skeleton overlay
- **Gesture-Based Controls**:
  - **Index Finger Pointing**: Activates drawing mode (writes with current color)
  - **OK Sign** (thumb + index pinched): Cycles through colors
  - **Peace Sign** (index + middle extended): Activates eraser mode
  - **Palm Open**: Idle mode (no drawing)
- **Dynamic Color Changing**: Cycle through 5 colors (GREEN, BLUE, ORANGE, CYAN, MAGENTA) via OK gesture
- **Live Info Display**: Shows current mode, color, line thickness, and eraser size

## Installation

```bash
pip install -r requirements.txt
```

**Note:** The first time you run the app, it will download the MediaPipe hand detection model (~200MB). This happens automatically.

## Usage

```bash
python air_writing.py
```

### Controls

- **Index Finger Pointing**: Draw on canvas with current color
- **OK Sign Gesture** (thumb + index touching): Change to next color
- **Peace Sign Gesture** (index + middle extended): Erase
- **Palm Open**: Idle state (no drawing)
- **`+` key**: Increase line thickness
- **`-` key**: Decrease line thickness
- **`C` key**: Clear entire canvas
- **`ESC` key**: Exit application

## How It Works

1. The application captures video from your webcam
2. MediaPipe detects hand landmarks in real-time (21 points per hand)
3. Different hand gestures trigger different modes:
   - Drawing with the index finger pointing
   - Erasing with the peace sign
   - Changing colors with the OK sign
4. All drawings are composited over the webcam feed with 70% transparency
5. A status bar shows current mode and settings

## Gesture Guide

- **Drawing**: Point only your index finger (other fingers curled)
- **Color Change**: Make an "OK" sign (thumb + index touching, other fingers extended apart)
- **Eraser**: Make a "peace" sign (index + middle extended, ring + pinky curled)
- **Idle**: Open palm with most fingers extended

## Requirements

- Python 3.7+
- Webcam
- ~200MB disk space (for MediaPipe model)

