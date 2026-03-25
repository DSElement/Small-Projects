package city;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Program{
    public static void main(String[] args){
        EventBus eventBus = EventBus.getDefault();

        CityInformationDashboard dashboard = new CityInformationDashboard();
        eventBus.register(dashboard);

        SpeedCamera speedCamera1 = new SpeedCamera(eventBus, "Main Street", 50);
        SpeedCamera speedCamera2 = new SpeedCamera(eventBus, "Second Avenue", 30);
        SurveillanceCamera surveillanceCamera1 = new SurveillanceCamera(eventBus, "Central Park");
        TrafficCamera trafficCamera1 = new TrafficCamera(eventBus, "Main Street", CongestionLevel.LOW);

        ExecutorService executor = Executors.newFixedThreadPool(4);

        executor.submit(() -> {
            int[] observedSpeeds = {70, 52, 48, 90, 61};
            String[] plates = {"ABC-123", "AA-111", "BB-222", "CC-333", "DD-444"};
            for (int i = 0; i < observedSpeeds.length; i++) {
                speedCamera1.checkForSpeeding(observedSpeeds[i], plates[i]);
            }
        });

        executor.submit(() -> {
            int[] observedSpeeds = {40, 31, 29, 55, 36};
            String[] plates = {"XYZ-789", "EE-555", "FF-666", "GG-777", "HH-888"};
            for (int i = 0; i < observedSpeeds.length; i++) {
                speedCamera2.checkForSpeeding(observedSpeeds[i], plates[i]);
            }
        });

        executor.submit(() -> {
            String[] incidents = {
                "Group of people loitering",
                "Garbage thrown near bench",
                "Vandalism attempt"
            };
            for (String incident : incidents) {
                surveillanceCamera1.detectSuspiciousActivity(incident);
            }
        });

        executor.submit(() -> {
            CongestionLevel[] levels = {
                CongestionLevel.MODERATE,
                CongestionLevel.HIGH,
                CongestionLevel.VERY_HIGH,
                CongestionLevel.HIGH,
                CongestionLevel.MODERATE
            };
            for (CongestionLevel level : levels) {
                trafficCamera1.updateCongestionLevel(level);
            }
        });

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println("--- Dashboard Summary ---");
        System.out.println("Speed fines: " + dashboard.getNumberOfSpeedViolations());
        System.out.println("Public disorder fines: " + dashboard.getNumberOfPublicDisorderIncidents());
        System.out.println("Current congestion map: " + dashboard.getCongestionLevelsByStreet());
        System.out.println("Most dangerous neighborhoods: " + dashboard.getMostDangerousNeighborhoods(3));

        eventBus.unregister(dashboard);
    }
}

