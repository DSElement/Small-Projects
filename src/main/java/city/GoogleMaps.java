package city;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleMaps extends NavigationApp {
    private static final Logger logger = LoggerFactory.getLogger(GoogleMaps.class);

    public GoogleMaps() {
        super("Google Maps");
    }

    public GoogleMaps(boolean debugMode) {
        super("Google Maps", debugMode);
    }

    @Override
    public void update(TrafficCongestionEvent event) {
        if (debugMode) {
            logger.info("Google Maps updated: {} - Congestion level: {}", event.getStreetName(), event.getCongestionLevel());
        }
    }
}
