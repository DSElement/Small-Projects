# Parallel Tic-Tac-Toe with Minimax Search

## Overview
A variable-size Tic-Tac-Toe game implemented in C, where the AI uses a Minimax search algorithm with Alpha-Beta pruning. The project supports optional parallelism via OpenMP (two parallel strategies) and includes a benchmarking pipeline that measures runtime across different board sizes, search depths, and thread counts. A Python script is used to visualize the results as speedup and runtime plots.

## Features
- Configurable board size (N×N, where 2 ≤ N ≤ 15)
- AI opponent using Minimax with Alpha-Beta pruning
- Two parallel versions of the AI move computation (OpenMP)
- Three game modes: Human vs Computer, Computer vs Human, Computer vs Computer
- Benchmark mode: sweeps board sizes, search depths, and thread counts and writes results to CSV
- Python plotting script for speedup and runtime analysis

## Technologies Used
- **C** — game logic, AI search, OpenMP parallelism
- **OpenMP** — shared-memory parallelism (2 strategies)
- **Python** (`pandas`, `matplotlib`) — result analysis and plot generation

## Project Structure
| File | Description |
|------|-------------|
| `utils.h` | Board structure, constants, and function declarations |
| `utils.c` | Core game logic, Minimax AI, parallel move computation |
| `game_xsi0.c` | Entry point: game modes, user interaction, benchmark runner |
| `results.csv` | Output of benchmark runs (board size, depth, threads, runtimes) |
| `plot_speedups.py` | Generates speedup and runtime plots from `results.csv` |
| `color_explained.txt` | Notes on the score sign convention used by the Minimax evaluator |

## How to Build / Run
run start_game.bat

## Notes
- The benchmark routine tests board sizes 3–8, depths 5–11, and thread counts 2/4/8. Large boards (>6) skip higher depths to keep runtimes manageable.
- Two parallel strategies (V1, V2) are compared; average speedup reaches ~2.4–2.7× at 8 threads.
- `MAXDEPTH`, `NUMTHREADS`, and `PARALLELVERSION` defaults are configurable via macros in `utils.h`.