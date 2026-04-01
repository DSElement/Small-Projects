# Smart City Surveillance System

An event-driven architecture simulation of a city surveillance system using Greenrobot EventBus with concurrent cameras and real-time event processing.

## Overview

The system simulates three types of surveillance cameras that publish events to subscribers:

| Camera Type | Event Type | Publishers | Subscribers |
|---|---|---|---|
| **Speed Cameras** | SpeedViolationEvent | 4 cameras | Traffic Police |
| **Traffic Cameras** | TrafficCongestionEvent | 4 cameras | Navigation Apps (Waze, Google Maps, Apple Maps) |
| **Surveillance Cameras** | PublicDisorderEvent | 3 cameras | Local Police |

All cameras run concurrently in separate threads and publish events through an EventBus for subscribers to handle.

## Technology Stack

- **Java 21** + Maven
- **Greenrobot EventBus 3.3.1** - lightweight pub/sub event bus
- **SLF4J + Logback** - logging
- **ExecutorService** - concurrent thread management

## Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/smart-city-surveillance-1.0-SNAPSHOT.jar
```

## Project Structure

```
src/main/java/city/
├── Program.java                 # Main entry point
├── CityEvent.java              # Base event class
├── SpeedCamera.java            # Speed violation publisher
├── TrafficCamera.java          # Traffic congestion publisher
├── SurveillanceCamera.java     # Public disorder publisher
├── TrafficPolice.java          # Speed event subscriber
├── LocalPolice.java            # Disorder event subscriber
├── Waze.java, GoogleMaps.java, AppleMaps.java  # Traffic subscribers
└── CityInformationDashboard.java  # All-events subscriber
```

## How It Works

1. Creates 11 cameras across the city (4 speed, 4 traffic, 3 surveillance)
2. Spawns 11 concurrent threads, one per camera
3. Each camera publishes simulated events to the EventBus
4. Subscribers receive relevant events and process them
5. Dashboard aggregates statistics (fines issued, dangerous neighborhoods)

## Features

✅ Event-driven architecture with loose coupling
✅ Multi-threaded concurrent camera operations (bonus requirement)
✅ Real-time event processing
✅ Scalable - add new subscribers without modifying existing code

See `sequence_diagram_speed_camera.png` for detailed event flow sequence diagram.
