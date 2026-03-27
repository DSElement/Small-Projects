package city;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Waze extends NavigationApp {
    private static final Logger logger = LoggerFactory.getLogger(Waze.class);

    public Waze() {
        super("Waze");
    }

    public Waze(boolean debugMode) {
        super("Waze", debugMode);
    }

    @Override
    public void update(TrafficCongestionEvent event) {
        if (debugMode) {
            logger.info("Waze updated: {} - Congestion level: {}", event.getStreetName(), event.getCongestionLevel());
        }
    }
}
