package city;

import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CityInformationDashboard {
    private static final Logger logger = LoggerFactory.getLogger(CityInformationDashboard.class);
    private static final int LIVE_REPORT_FREQUENCY = 3;
    private static final int LIVE_DANGEROUS_STREETS_LIMIT = 3;

    private final AtomicInteger numberOfSpeedViolations;
    private final AtomicInteger numberOfPublicDisorderIncidents;
    private final AtomicInteger seriousReportsCounter;
    private final Map<String, CongestionLevel> congestionLevelsByStreet;
    private final Map<String, AtomicInteger> issuesByStreet;
    private final Map<String, AtomicInteger> seriousIssuesByStreet;
    private final ExecutorService liveReporter;
    private final Object liveReportTriggerLock;
    private final Random random;

    public CityInformationDashboard() {
        this.numberOfSpeedViolations = new AtomicInteger(0);
        this.numberOfPublicDisorderIncidents = new AtomicInteger(0);
        this.seriousReportsCounter = new AtomicInteger(0);
        this.congestionLevelsByStreet = new ConcurrentHashMap<>();
        this.issuesByStreet = new ConcurrentHashMap<>();
        this.seriousIssuesByStreet = new ConcurrentHashMap<>();
        this.liveReporter = Executors.newSingleThreadExecutor();
        this.liveReportTriggerLock = new Object();
        this.random = new Random();
    }

    @Subscribe
    public void onSpeedViolation(SpeedViolationEvent event) {
        if (event == null) {
            logger.warn("Received null SpeedViolationEvent");
            return;
        }
        numberOfSpeedViolations.incrementAndGet();
        issuesByStreet.computeIfAbsent(event.getStreetName(), street -> new AtomicInteger(0)).incrementAndGet();
        registerSeriousIssueAndMaybeReport(event.getStreetName());
        logger.info("Speed violation detected on {} at speed {} km/h. License plate: {}",
                   event.getStreetName(), event.getRecordedSpeed(), event.getPlateNumber());
    }

    @Subscribe
    public void onPublicDisorder(PublicDisorderEvent event) {
        if (event == null) {
            logger.warn("Received null PublicDisorderEvent");
            return;
        }
        numberOfPublicDisorderIncidents.incrementAndGet();
        issuesByStreet.computeIfAbsent(event.getStreetName(), street -> new AtomicInteger(0)).incrementAndGet();
        registerSeriousIssueAndMaybeReport(event.getStreetName());
        logger.info("Public disorder detected at {}: {}", event.getStreetName(), event.getDisorderDescription());
    }

    @Subscribe
    public void onTrafficCongestion(TrafficCongestionEvent event) {
        if (event == null) {
            logger.warn("Received null TrafficCongestionEvent");
            return;
        }
        congestionLevelsByStreet.put(event.getStreetName(), event.getCongestionLevel());
        issuesByStreet.computeIfAbsent(event.getStreetName(), street -> new AtomicInteger(0)).incrementAndGet();
        logger.info("Traffic congestion updated on {}: {}", event.getStreetName(), event.getCongestionLevel());
    }

    public int getNumberOfSpeedViolations() {
        return numberOfSpeedViolations.get();
    }

    public int getNumberOfPublicDisorderIncidents() {
        return numberOfPublicDisorderIncidents.get();
    }

    public Map<String, CongestionLevel> getCongestionLevelsByStreet() {
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

    private void registerSeriousIssueAndMaybeReport(String streetName) {
        synchronized (liveReportTriggerLock) {
            seriousIssuesByStreet
                .computeIfAbsent(streetName, ignored -> new AtomicInteger(0))
                .incrementAndGet();

            int reportNumber = seriousReportsCounter.incrementAndGet();
            if (reportNumber % LIVE_REPORT_FREQUENCY != 0) {
                return;
            }

            liveReporter.submit(() -> {
                List<String> dangerousStreets = getMostDangerousStreetsBySeriousReports(LIVE_DANGEROUS_STREETS_LIMIT);
                if (dangerousStreets.isEmpty()) {
                    return;
                }
                logger.info("[Live dangerous streets | status at report #{}] {}", reportNumber, dangerousStreets);
            });
        }
    }

    private List<String> getMostDangerousStreetsBySeriousReports(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        synchronized (liveReportTriggerLock) {
            List<Map.Entry<String, AtomicInteger>> sorted = new ArrayList<>(seriousIssuesByStreet.entrySet());
            sorted.sort(Comparator.comparingInt((Map.Entry<String, AtomicInteger> entry) -> entry.getValue().get())
                                  .reversed());

            List<String> result = new ArrayList<>();
            int count = Math.min(limit, sorted.size());
            for (int i = 0; i < count; i++) {
                result.add(sorted.get(i).getKey());
            }

            Collections.shuffle(result, random);
            return result;
        }
    }
}