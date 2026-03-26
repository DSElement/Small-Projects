package city;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Program{
    private static final Logger logger = LoggerFactory.getLogger(Program.class);

    public static void main(String[] args){
        EventBus eventBus = EventBus.getDefault();
        Random random = new Random();

        CityInformationDashboard dashboard = new CityInformationDashboard();
        eventBus.register(dashboard);

        SpeedCamera speedCamera1 = new SpeedCamera(eventBus, "Main Street", 50);
        SpeedCamera speedCamera2 = new SpeedCamera(eventBus, "Second Avenue", 30);
        SpeedCamera speedCamera3 = new SpeedCamera(eventBus, "Third Boulevard", 40);
        SpeedCamera speedCamera4 = new SpeedCamera(eventBus, "Fourth Road", 60);
        SurveillanceCamera surveillanceCamera1 = new SurveillanceCamera(eventBus, "Central Park");
        SurveillanceCamera surveillanceCamera2 = new SurveillanceCamera(eventBus, "Downtown Square");
        SurveillanceCamera surveillanceCamera3 = new SurveillanceCamera(eventBus, "City Hall");
        TrafficCamera trafficCamera1 = new TrafficCamera(eventBus, "Main Street", CongestionLevel.LOW);
        TrafficCamera trafficCamera2 = new TrafficCamera(eventBus, "Second Avenue", CongestionLevel.MODERATE);
        TrafficCamera trafficCamera3 = new TrafficCamera(eventBus, "Third Boulevard", CongestionLevel.HIGH);
        TrafficCamera trafficCamera4 = new TrafficCamera(eventBus, "Fourth Road", CongestionLevel.VERY_HIGH);

        ThreadFactory namedThreadFactory = new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("camera-" + count.incrementAndGet());
                t.setDaemon(false);
                return t;
            }
        };
        ExecutorService executor = Executors.newFixedThreadPool(11, namedThreadFactory);

        executor.submit(() -> {
            int[] observedSpeeds = {70, 52, 48, 90, 61};
            String[] plates = {"ABC-123", "AA-111", "BB-222", "CC-333", "DD-444"};
            for (int i = 0; i < observedSpeeds.length; i++) {
                speedCamera1.checkForSpeeding(observedSpeeds[i] + random.nextInt(15), plates[i]);
            }
        });

        executor.submit(() -> {
            int[] observedSpeeds = {40, 31, 29, 55, 36};
            String[] plates = {"XYZ-789", "EE-555", "FF-666", "GG-777", "HH-888"};
            for (int i = 0; i < observedSpeeds.length; i++) {
                speedCamera2.checkForSpeeding(observedSpeeds[i] + random.nextInt(15), plates[i]);
            }
        });

        executor.submit(() -> {
            int[] observedSpeeds = {50, 42, 38, 65, 46};
            String[] plates = {"IJK-901", "II-222", "JJ-333", "KK-444", "LL-555"};
            for (int i = 0; i < observedSpeeds.length; i++) {
                speedCamera3.checkForSpeeding(observedSpeeds[i] + random.nextInt(15), plates[i]);
            }
        });

        executor.submit(() -> {
            int[] observedSpeeds = {60, 51, 49, 75, 56};
            String[] plates = {"MNO-678", "MM-777", "NN-888", "OO-999", "PP-000"};
            for (int i = 0; i < observedSpeeds.length; i++) {
                speedCamera4.checkForSpeeding(observedSpeeds[i] + random.nextInt(15), plates[i]);
            }
        });

        executor.submit(() -> {
            String[] incidents = {
                "Group of people loitering",
                "Garbage thrown near bench",
                "Vandalism attempt",
                "Suspicious person detected"
            };
            int eventCount = 2 + random.nextInt(3);
            for (int i = 0; i < eventCount; i++) {
                surveillanceCamera1.detectSuspiciousActivity(incidents[random.nextInt(incidents.length)]);
            }
        });

        executor.submit(() -> {
            String[] incidents = {
                "Public intoxication",
                "Suspicious package left unattended",
                "Altercation detected",
                "Theft in progress"
            };
            int eventCount = 1 + random.nextInt(3);
            for (int i = 0; i < eventCount; i++) {
                surveillanceCamera2.detectSuspiciousActivity(incidents[random.nextInt(incidents.length)]);
            }
        });

        executor.submit(() -> {
            String[] incidents = {
                "Vandalism attempt",
                "Suspicious activity",
                "Unauthorized entry"
            };
            int eventCount = 1 + random.nextInt(2);
            for (int i = 0; i < eventCount; i++) {
                surveillanceCamera3.detectSuspiciousActivity(incidents[random.nextInt(incidents.length)]);
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

        executor.submit(() -> {
            CongestionLevel[] levels = {
                CongestionLevel.LOW,
                CongestionLevel.MODERATE,
                CongestionLevel.HIGH,
                CongestionLevel.VERY_HIGH,
                CongestionLevel.HIGH
            };
            for (CongestionLevel level : levels) {
                trafficCamera2.updateCongestionLevel(level);
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
                trafficCamera3.updateCongestionLevel(level);
            }
        });

        executor.submit(() -> {
            CongestionLevel[] levels = {
                CongestionLevel.LOW,
                CongestionLevel.MODERATE,
                CongestionLevel.HIGH,
                CongestionLevel.VERY_HIGH,
                CongestionLevel.HIGH
            };
            for (CongestionLevel level : levels) {
                trafficCamera4.updateCongestionLevel(level);
            }
        });

        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate within timeout, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            logger.warn("Executor shutdown interrupted");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        dashboard.shutdownLiveReporter();

        eventBus.unregister(dashboard);
        logger.info("Surveillance system shutdown complete");
    }
}

