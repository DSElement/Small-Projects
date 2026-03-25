package city;

import org.greenrobot.eventbus.EventBus;

public class TrafficCamera {
    private final EventBus eventBus;
    private final CameraType cameraType = CameraType.TRAFFIC_CAMERA;
    private final String streetName;
    private CongestionLevel congestionLevel;

    public TrafficCamera(EventBus eventBus, String streetName, CongestionLevel congestionLevel) {
        this.eventBus = eventBus;
        this.streetName = streetName;
        this.congestionLevel = congestionLevel;
    }

    public void updateCongestionLevel(CongestionLevel newLevel) {
        if (newLevel == null) {
            throw new IllegalArgumentException("Congestion level must not be null");
        }
        if (newLevel == congestionLevel) {
            return; // No change in congestion level
        }

        congestionLevel = newLevel;
        TrafficCongestionEvent event = new TrafficCongestionEvent(cameraType, streetName, congestionLevel);
        eventBus.post(event);
    }
}