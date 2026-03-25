package city;

import org.greenrobot.eventbus.EventBus;

public class SpeedCamera {
    private final EventBus eventBus;
    private CameraType cameraType = city.CameraType.SPEED_CAMERA;
    private String streetName;
    private int speedLimit;

    public SpeedCamera(EventBus eventBus, String streetName, int speedLimit) {
        this.eventBus = eventBus;
        this.streetName = streetName;
        this.speedLimit = speedLimit;
    }

    public void checkForSpeeding(int vehicleSpeed, String licensePlate) {
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