# Small Projects

Focused coursework and personal projects covering concurrency, distributed algorithms, simulation, computer vision, reflection, and data processing. Each directory has its own README and runtime requirements.

| Project | Technology | What the code demonstrates |
| --- | --- | --- |
| [Smart City Surveillance](SmartCitySurveillanceSystem/) | Java 21, Maven, Greenrobot EventBus | Typed publish/subscribe events, concurrent simulated cameras, incident counters, and console reporting. |
| [My Reverse Engineering Tool](MyReverseEngineeringTool/) | Java 21, reflection, JUnit | JAR inspection, class/member and relationship extraction, package filters, text/PlantUML output, and custom renderers. |
| [Tic-Tac-Toe search](TicTacToe_AlphaBetaPruning/) | C, OpenMP, Python plotting | Depth-limited minimax with alpha-beta pruning, sequential and parallel move evaluation, and benchmark collection. |
| [Raft leader-election simulation](RAFT-Consensus-Algorithm/) | C, MPI | Terms, randomized timeouts, majority voting, heartbeats, and simulated failure/recovery. No replicated log or persistent consensus state. |
| [Bike-sharing simulation](Bicycle_Stations_Simulation/) | Python, SimPy, pandas, Matplotlib | Time-dependent demand, repairs, rebalancing, customer outcomes, and PDF reports in a notebook. |
| [Predator-prey simulation](Predator_Prey_Simulation/) | Python, Pygame, Matplotlib | Agent behavior, energy budgets, obstacles, reproduction, interactive parameters, and population plots. |
| [Hand-gesture drawing](Hand-Gesture-Drawing/) | Python, OpenCV, MediaPipe, NumPy | Threaded webcam capture, asynchronous hand landmarks, gesture controls, and smoothed drawing. |
| [Excel merit-sheet merger](Excel%20Merit%20Sheet%20Merger/) | C#, ClosedXML | Template-specific worksheet selection, merged-header reconstruction, and workbook aggregation. Source is present but the `.csproj` is missing; restore a project and its dependency before using the build instructions. |

## Repository contents

Source, manifests, the Java tool's intentional sample JAR, the MediaPipe model asset, benchmark data, diagrams, and selected simulation reports are retained as project evidence. Build directories, Python caches, local environments, editor state, and private merit workbooks are ignored. Ignore rules do not remove files already tracked in Git.

There is no shared build command. Some existing READMEs describe earlier versions: inspect current source before relying on controls, performance figures, or completeness claims. For example, the gesture app currently uses an open palm to erase and three colors.
