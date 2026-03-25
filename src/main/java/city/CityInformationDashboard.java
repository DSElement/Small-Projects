package city;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CityInformationDashboard {
    private final AtomicInteger numberOfSpeedViolations;
    private final AtomicInteger numberOfPublicDisorderIncidents;
    private final Map<String, CongestionLevel> congestionLevelsByStreet;
    private final Map<String, AtomicInteger> issuesByStreet;

    public CityInformationDashboard() {
        this.numberOfSpeedViolations = new AtomicInteger(0);
        this.numberOfPublicDisorderIncidents = new AtomicInteger(0);
        this.congestionLevelsByStreet = new ConcurrentHashMap<>();
        this.issuesByStreet = new ConcurrentHashMap<>();
    }

    @Subscribe
    public void onSpeedViolation(SpeedViolationEvent event) {
        numberOfSpeedViolations.incrementAndGet();
        issuesByStreet.computeIfAbsent(event.getStreetName(), street -> new AtomicInteger(0)).incrementAndGet();
        System.out.println("Speed violation detected on " + event.getStreetName() + 
                            " at speed " + event.getRecordedSpeed() + 
                            " km/h. License plate: " + event.getPlateNumber());
    }

    @Subscribe
    public void onPublicDisorder(PublicDisorderEvent event) {
        numberOfPublicDisorderIncidents.incrementAndGet();
        issuesByStreet.computeIfAbsent(event.getStreetName(), street -> new AtomicInteger(0)).incrementAndGet();
        System.out.println("Public disorder detected at " + event.getStreetName() + 
                            ": " + event.getDisorderDescription());
    }

    @Subscribe
    public void onTrafficCongestion(TrafficCongestionEvent event) {
        congestionLevelsByStreet.put(event.getStreetName(), event.getCongestionLevel());
        issuesByStreet.computeIfAbsent(event.getStreetName(), street -> new AtomicInteger(0)).incrementAndGet();
        System.out.println("Traffic congestion updated on " + event.getStreetName() + 
                            ": " + event.getCongestionLevel());
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
}