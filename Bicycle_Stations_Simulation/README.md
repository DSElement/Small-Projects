# Bike-Sharing System Simulation

## Overview
A discrete-event simulation of a multi-station bike-sharing system. The simulation models realistic urban bike-sharing dynamics including time-varying demand, bike maintenance, station rebalancing, and customer satisfaction tracking, all over a configurable multi-day period.

## Features
- **Multiple stations** with individual capacities and initial bike counts (Cathedral, Center, Dorms, P700, UPT)
- **Time slot-specific trip probabilities** that vary across periods of the day and recur daily
- **Bike maintenance pipeline**: broken bikes are sent to a repair service, with configurable repair probability and a scrapping chance for bikes damaged beyond repair
- **Periodic rebalancing**: bikes are redistributed among stations at a configurable interval based on demand weights derived from the probability matrix
- **Customer satisfaction tracking**: logs both failed "take" requests (no bikes available) and failed "return" requests (station full)
- **Comprehensive data collection**: per-station bike counts, bikes under maintenance, and customer satisfaction events, all sampled over time
- **Automated PDF report generation** with heatmaps, typical-day profiles, station KPIs (stockout %, overflow %), and satisfaction summary

## Technologies Used
- **Python** — simulation logic and data analysis
- **SimPy** — discrete-event simulation framework
- **pandas / matplotlib** — data processing and visualization
- **Jupyter Notebook** — interactive development and experimentation

## How to Run
Configure parameters at the top of the simulation cell (duration, rebalancing interval, breakdown chance, probability matrix version, etc.), then run all cells to execute the simulation and generate plots.

## Notes
- The repair process uses a probabilistic loop: each 30-minute repair attempt succeeds with 90% probability or permanently scraps the bike with 0.1% probability.
- Rebalancing targets a distribution derived from the active probability matrix, so desired station quotas shift dynamically with time slot changes.