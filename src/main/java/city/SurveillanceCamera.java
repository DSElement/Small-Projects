package city;

import org.greenrobot.eventbus.EventBus;

public class SurveillanceCamera {
    private final EventBus eventBus;
    private final String location;

    public SurveillanceCamera(EventBus eventBus, String location) {
        this.eventBus = eventBus;
        this.location = location;
    }

    public void detectSuspiciousActivity(String activityDescription) {
        if (activityDescription == null || activityDescription.isBlank()) {
            throw new IllegalArgumentException("Activity description must not be blank");
        }
        PublicDisorderEvent event = new PublicDisorderEvent(location,
                                                            activityDescription);
        eventBus.post(event);
    }
}