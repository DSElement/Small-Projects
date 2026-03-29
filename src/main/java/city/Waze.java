package city;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Waze extends NavigationApp {
    private static final Logger logger = LoggerFactory.getLogger(Waze.class);

    public Waze(EventBus eventBus) {
        super(eventBus, "Waze");
    }

    public Waze(EventBus eventBus, boolean debugMode) {
        super(eventBus, "Waze", debugMode);
    }

    @Override
    public void update(TrafficCongestionEvent event) {
        if (debugMode) {
            logger.info("Waze updated: {} - Congestion level: {}", event.getStreetName(), event.getCongestionLevel());
        }
    }
}
