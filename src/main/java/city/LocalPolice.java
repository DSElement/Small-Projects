package city;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalPolice {
    private static final Logger logger = LoggerFactory.getLogger(LocalPolice.class);

    private final EventBus eventBus;
    private final AtomicInteger disorderIncidentCounter;
    private final List<PublicDisorderEvent> disorderIncidents;

    public LocalPolice(EventBus eventBus) {
        this.eventBus = eventBus;
        this.disorderIncidentCounter = new AtomicInteger(0);
        this.disorderIncidents = Collections.synchronizedList(new ArrayList<>());
    }

    @Subscribe
    public void onPublicDisorder(PublicDisorderEvent event) {
        if (event == null) {
            logger.warn("Received null PublicDisorderEvent");
            return;
        }
        disorderIncidents.add(event);
        int count = disorderIncidentCounter.incrementAndGet();
        logger.debug("Public disorder incident recorded at {}: {}. Total incidents: {}",
                     event.getStreetName(), event.getDisorderDescription(), count);

        eventBus.post(new InformationDashboardEvent(event.getStreetName(), CityEventEnum.PUBLIC_DISORDER));
    }

    public int getDisorderIncidentCount() {
        return disorderIncidentCounter.get();
    }

    public List<PublicDisorderEvent> getDisorderIncidents() {
        return new ArrayList<>(disorderIncidents);
    }
}
