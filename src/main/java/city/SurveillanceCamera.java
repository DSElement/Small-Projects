package city;

import org.greenrobot.eventbus.EventBus;

public class SurveillanceCamera {
    private final EventBus eventBus;
    private CameraType cameraType = city.CameraType.SURVEILLANCE_CAMERA;
    private String location;

    public SurveillanceCamera(EventBus eventBus, String location) {
        this.eventBus = eventBus;
        this.location = location;
    }

    public void detectSuspiciousActivity(String activityDescription) {
        PublicDisorderEvent event = new PublicDisorderEvent(cameraType,
                                                                location,
                                                                activityDescription);
        eventBus.post(event);
    }
}