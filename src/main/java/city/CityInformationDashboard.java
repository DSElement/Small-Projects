package city;

import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import org.greenrobot.eventbus.Subscribe;

public class CityInformationDashboard {
    private int numberOfSpeedViolations;
    private int numberOfPublicDisorderIncidents;
    private HashMap<String, CongestionLevel> congestionLevelsByStreet;

    public CityInformationDashboard() {
        this.numberOfSpeedViolations = 0;
        this.numberOfPublicDisorderIncidents = 0;
        this.congestionLevelsByStreet = new HashMap<>();
    }

    @Subscribe
    public void onSpeedViolation(SpeedViolationEvent event) {
        numberOfSpeedViolations++;
        System.out.println("Speed violation detected on " + event.getStreetName() + 
                            " at speed " + event.getRecordedSpeed() + 
                            " km/h. License plate: " + event.getPlateNumber());
    }

    @Subscribe
    public void onPublicDisorder(PublicDisorderEvent event) {
        numberOfPublicDisorderIncidents++;
        System.out.println("Public disorder detected at " + event.getStreetName() + 
                            ": " + event.getDisorderDescription());
    }

    @Subscribe
    public void onTrafficCongestion(TrafficCongestionEvent event) {
        congestionLevelsByStreet.put(event.getStreetName(), event.getCongestionLevel());
        System.out.println("Traffic congestion updated on " + event.getStreetName() + 
                            ": " + event.getCongestionLevel());
    }
}