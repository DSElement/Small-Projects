# Raft Consensus Algorithm over MPI

## Overview
A single-file C implementation of the [Raft consensus algorithm](https://raft.github.io/) using MPI for inter-process communication. Each MPI process acts as an independent Raft node. The simulation runs for 12 seconds and includes leader election, heartbeat propagation, and a crash/restart fault injection scenario to observe re-election behavior.

## Features
- **Leader election**: nodes start as followers with randomized election timeouts (150–300 ms); the first to time out becomes a candidate and broadcasts `RequestVote` messages
- **Heartbeats**: the elected leader periodically sends heartbeats (every 50 ms) to suppress follower timeouts
- **Term management**: stale nodes that receive a higher term automatically step down to follower
- **Fault injection**: the current leader automatically crashes after holding leadership for 2 seconds (up to 3 times), then restarts after 8 seconds, triggering a new election
- **Non-blocking I/O**: uses `MPI_Isend` / `MPI_Iprobe` to avoid blocking on message sends while polling for incoming messages

## Technologies Used
- **C** — core implementation
- **MPI** (`msmpi` / `OpenMPI`) — distributed message passing between nodes

## Project Structure
| File | Description |
|------|-------------|
| `raft_mpi.c` | Complete implementation: node state machine, message handlers, election logic, heartbeat loop, and fault injection |

## How to Build / Run
```bash
# Windows (MS-MPI)
gcc -g raft_mpi.c -I "c:\Program Files (x86)\Microsoft SDKs\MPI\Include" \
    -L "c:\Program Files (x86)\Microsoft SDKs\MPI\Lib\x64" -lmsmpi -o raft_mpi.exe

# Linux (OpenMPI)
mpicc -g raft_mpi.c -o raft_mpi

# Run with 7 nodes (2–7 supported)
mpiexec -n 7 raft_mpi.exe
```

## Notes
- This is a simplified Raft implementation: it covers leader election and heartbeats but does not implement log replication or persistence.
- The fault injection is hardcoded — the leader crashes after 2 seconds of holding the role and restarts 8 seconds into the simulation. Up to 3 crash cycles are simulated.
- Randomized seeds per process (`rank * 1337 + time`) ensure election timeouts are spread out and reduce the chance of split votes.
- The simulation runs for a fixed 12-second wall-clock window, after which all nodes call `MPI_Finalize`.