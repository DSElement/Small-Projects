package city;

import org.greenrobot.eventbus.EventBus;

public class SpeedCamera {
    private final EventBus eventBus;
    private final CameraType cameraType = CameraType.SPEED_CAMERA;
    private final String streetName;
    private final int speedLimit;

    public SpeedCamera(EventBus eventBus, String streetName, int speedLimit) {
        this.eventBus = eventBus;
        this.streetName = streetName;
        this.speedLimit = speedLimit;
    }

    public void checkForSpeeding(int vehicleSpeed, String licensePlate) {
        if (vehicleSpeed < 0) {
            throw new IllegalArgumentException("Vehicle speed cannot be negative");
        }
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("License plate must not be blank");
        }
        if (vehicleSpeed <= (speedLimit + 5)){
            return;
        }

        SpeedViolationEvent event = new SpeedViolationEvent(cameraType, 
                                                            streetName,
                                                            vehicleSpeed,
                                                            licensePlate);
        eventBus.post(event);
    }
}