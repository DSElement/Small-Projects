package city;

import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CityInformationDashboard {
    private static final Logger logger = LoggerFactory.getLogger(CityInformationDashboard.class);
    private static final int LIVE_REPORT_FREQUENCY = 3;

    private final TrafficPolice trafficPolice;
    private final LocalPolice localPolice;
    private AtomicInteger liveReportCounter;
    private AtomicInteger lastReportedCount;
    private final Map<String, CongestionLevelEnum> congestionLevelsByStreet;
    private final Map<String, AtomicInteger> issuesByStreet;
    private final ExecutorService liveReporter;

    public CityInformationDashboard(TrafficPolice trafficPolice, LocalPolice localPolice) {
        this(trafficPolice, localPolice, false);
    }

    public CityInformationDashboard(TrafficPolice trafficPolice, LocalPolice localPolice, boolean debugMode) {
        this.trafficPolice = trafficPolice;
        this.localPolice = localPolice;
        this.liveReportCounter = new AtomicInteger(0);
        this.lastReportedCount = new AtomicInteger(0);
        this.congestionLevelsByStreet = new ConcurrentHashMap<>();
        this.issuesByStreet = new ConcurrentHashMap<>();
        this.liveReporter = Executors.newSingleThreadExecutor();
    }

    @Subscribe
    public void onInformationDashboardEvent(InformationDashboardEvent event) {
        if (event == null) {
            logger.warn("Received null InformationDashboardEvent");
            return;
        }
        
        if (event.getEventType() == null) {
            logger.warn("Received InformationDashboardEvent with null event type");
            return;
        }

        if (event.getEventType() == CityEventEnum.TRAFFIC_CONGESTION) {
            return; // Congestion events are handled by navigation apps, not counted as issues
        } else if (event.getEventType() == CityEventEnum.PUBLIC_DISORDER) {
            issuesByStreet.computeIfAbsent(event.getStreetName(), k -> new AtomicInteger(0)).incrementAndGet();
        } else if (event.getEventType() == CityEventEnum.SPEEDING_VIOLATION) {
            issuesByStreet.computeIfAbsent(event.getStreetName(), k -> new AtomicInteger(0)).incrementAndGet();
        } else {
            logger.warn("Received InformationDashboardEvent with unknown event type: {}", event.getEventType());
            return;
        }

        synchronized (this) {
            int count = liveReportCounter.incrementAndGet();
            int lastCount = lastReportedCount.get();
            if (count - lastCount >= LIVE_REPORT_FREQUENCY) {
                lastReportedCount.set(count);
                int disorderCount = localPolice.getDisorderIncidentCount();
                int speedingCount = trafficPolice.getSpeedingViolationCount();
                liveReporter.submit(() -> {
                    logger.info("Live report: {} public disorder incidents, {} speeding violations",
                                disorderCount, speedingCount);

                    if (count % (LIVE_REPORT_FREQUENCY * 3) == 0) {
                        List<String> dangerousNeighborhoods = getMostDangerousNeighborhoods(3);
                        logger.info("Most dangerous neighborhoods: {}", dangerousNeighborhoods);
                    }
                });
            }
        }
    }

    public int getNumberOfSpeedingFines() {
        return trafficPolice.getSpeedingViolationCount();
    }

    public int getNumberOfPublicDisorderFines() {
        return localPolice.getDisorderIncidentCount();
    }

    public Map<String, CongestionLevelEnum> getCongestionLevelsByStreet() {
        return Map.copyOf(congestionLevelsByStreet);
    }

    public List<String> getMostDangerousNeighborhoods(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<Map.Entry<String, AtomicInteger>> sorted = new ArrayList<>(issuesByStreet.entrySet());
        sorted.sort(Comparator.comparingInt((Map.Entry<String, AtomicInteger> entry) -> entry.getValue().get())
                              .reversed());

        List<String> result = new ArrayList<>();
        int count = Math.min(limit, sorted.size());
        for (int i = 0; i < count; i++) {
            result.add(sorted.get(i).getKey());
        }
        return result;
    }

    public void shutdownLiveReporter() {
        liveReporter.shutdown();
        try {
            if (!liveReporter.awaitTermination(5, TimeUnit.SECONDS)) {
                liveReporter.shutdownNow();
            }
        } catch (InterruptedException ex) {
            liveReporter.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}