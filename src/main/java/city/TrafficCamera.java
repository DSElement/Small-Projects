package city;

import org.greenrobot.eventbus.EventBus;

public class TrafficCamera {
    private final EventBus eventBus;
    private final String streetName;
    private CongestionLevelEnum congestionLevel;

    public TrafficCamera(EventBus eventBus, String streetName, CongestionLevelEnum congestionLevel) {
        this.eventBus = eventBus;
        this.streetName = streetName;
        this.congestionLevel = congestionLevel;
    }

    public void updateCongestionLevel(CongestionLevelEnum newLevel) {
        if (newLevel == null) {
            throw new IllegalArgumentException("Congestion level must not be null");
        }
        if (newLevel == congestionLevel) {
            return; // No change in congestion level
        }

        congestionLevel = newLevel;
        TrafficCongestionEvent event = new TrafficCongestionEvent(streetName, congestionLevel);
        eventBus.post(event);
    }
}