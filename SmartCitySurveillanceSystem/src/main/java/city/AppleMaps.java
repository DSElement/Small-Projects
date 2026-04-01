package city;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppleMaps extends NavigationApp {
    private static final Logger logger = LoggerFactory.getLogger(AppleMaps.class);

    public AppleMaps(EventBus eventBus) {
        super(eventBus, "Apple Maps");
    }

    public AppleMaps(EventBus eventBus, boolean debugMode) {
        super(eventBus, "Apple Maps", debugMode);
    }

    @Override
    public void update(TrafficCongestionEvent event) {
        if (debugMode) {
            logger.info("Apple Maps updated: {} - Congestion level: {}", event.getStreetName(), event.getCongestionLevel());
        }
    }
}
