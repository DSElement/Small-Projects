package city;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleMaps extends NavigationApp {
    private static final Logger logger = LoggerFactory.getLogger(GoogleMaps.class);

    public GoogleMaps(EventBus eventBus) {
        super(eventBus, "Google Maps");
    }

    public GoogleMaps(EventBus eventBus, boolean debugMode) {
        super(eventBus, "Google Maps", debugMode);
    }

    @Override
    public void update(TrafficCongestionEvent event) {
        if (debugMode) {
            logger.info("Google Maps updated: {} - Congestion level: {}", event.getStreetName(), event.getCongestionLevel());
        }
    }
}
