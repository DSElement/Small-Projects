# Predator-Prey Simulation

## Overview
A real-time, graphical predator-prey ecosystem simulation built with Pygame. Prey and predators move autonomously around an 800×600 environment, eating, reproducing, and dying according to energy budgets and behavioral rules. The simulation runs at an accelerated time scale and saves population history plots on exit.

## Features
- **Prey behaviors**: flee from predators, forage for food when hungry, flock with nearby prey (alignment / cohesion / separation), and sprint briefly when a predator is extremely close
- **Predator behaviors**: hunt the nearest visible prey, eat prey and corpses to regain energy, wander when satiated, avoid obstacles
- **Food system**: two food types with different energy values — Berries (common, small) and Jackfruits (rare, larger) — spawn randomly and respawn over time up to a configurable cap
- **Corpse system**: starved prey leave a corpse that decays after 5 seconds and can be scavenged by predators
- **Reproduction**: both species reproduce asexually by pairing with a nearby mate, stopping briefly to mate, then spawning an offspring between them; governed by energy thresholds and cooldown timers
- **Obstacles**: 7 static rocks (circles and rectangles) that block movement; agents steer around them and bounce on collision; food never spawns on or too close to rocks
- **Live HUD**: real-time stats overlay and two mini in-game graphs tracking population and birth rates
- **Runtime controls**: keyboard adjustments for food spawn rate, energy drain rate, and simulation time scale
- **Post-run plots**: population history saved as PNG files via matplotlib on exit

## Technologies Used
- **Python** — simulation logic
- **Pygame** — rendering, input handling, and vector math
- **matplotlib** — post-run population history plots (optional)

## Project Structure
| File | Description |
|------|-------------|
| `predator_prey.py` | Complete simulation: entity classes (`Prey`, `Predator`, `Fruit`, `Rock`, `Corpse`), behavioral logic, rendering, and main loop |
| `population_history_*.png` | Example output plots from three simulation runs |

## How to Run
```bash
pip install pygame matplotlib
python predator_prey.py
```

### Keyboard Controls
| Key | Action |
|-----|--------|
| `Q` / `W` | Decrease / increase food spawn rate |
| `A` / `S` | Decrease / increase energy drain rate |
| `Z` / `X` | Decrease / increase simulation time scale |

Population history plots are saved automatically when the window is closed.

## Notes
- All major simulation parameters (speeds, energy thresholds, reproduction cooldowns, flock weights, vision radii, etc.) are defined as constants at the top of the file and are easy to tune.
- Flocking is disabled when prey are hungry or fleeing, so individual survival needs override collective behavior.
- Predators move faster when low on energy (desperation mechanic), while prey do the opposite — they slow down when energy is low unless sprinting.
- The three included example plots show the characteristic oscillating population dynamics typical of predator-prey systems, with occasional extinction events visible in run 3.