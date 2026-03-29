package city;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TrafficPolice {
    private static final Logger logger = LoggerFactory.getLogger(TrafficPolice.class);

    private final EventBus eventBus;
    private final AtomicInteger speedingViolationCounter;
    private final List<SpeedViolationEvent> speedingViolations;

    public TrafficPolice(EventBus eventBus) {
        this.eventBus = eventBus;
        this.speedingViolationCounter = new AtomicInteger(0);
        this.speedingViolations = Collections.synchronizedList(new ArrayList<>());
    }

    @Subscribe
    public void onSpeedViolation(SpeedViolationEvent event) {
        if (event == null) {
            logger.warn("Received null SpeedViolationEvent");
            return;
        }
        speedingViolations.add(event);
        int count = speedingViolationCounter.incrementAndGet();
        logger.debug("Speed violation recorded on {} at {} km/h, plate: {}. Total violations: {}",
                     event.getStreetName(), event.getRecordedSpeed(), event.getPlateNumber(), count);

        eventBus.post(new InformationDashboardEvent(event.getStreetName(), CityEventEnum.SPEEDING_VIOLATION));
    }

    public int getSpeedingViolationCount() {
        return speedingViolationCounter.get();
    }

    public List<SpeedViolationEvent> getSpeedingViolations() {
        return new ArrayList<>(speedingViolations);
    }
}
