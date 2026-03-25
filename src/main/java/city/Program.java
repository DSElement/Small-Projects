package city;

import org.greenrobot.eventbus.EventBus;

public class Program{
    public static void main(String[] args){
        EventBus eventBus = EventBus.getDefault();

        CityInformationDashboard dashboard = new CityInformationDashboard();
        eventBus.register(dashboard);

        SpeedCamera speedCamera1 = new SpeedCamera(eventBus, "Main Street", 50);
        SpeedCamera speedCamera2 = new SpeedCamera(eventBus, "Second Avenue", 30);
        SurveillanceCamera surveillanceCamera1 = new SurveillanceCamera(eventBus, "Central Park");
        TrafficCamera trafficCamera1 = new TrafficCamera(eventBus, "Main Street", CongestionLevel.Low);

        // Simulate some events
        speedCamera1.checkForSpeeding(70, "ABC-123");
        speedCamera2.checkForSpeeding(40, "XYZ-789");
        surveillanceCamera1.detectSuspiciousActivity("Group of people loitering");
        trafficCamera1.updateCongestionLevel(CongestionLevel.High);
    }
}

