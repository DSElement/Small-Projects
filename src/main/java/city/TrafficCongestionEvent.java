package city;

public class TrafficCongestionEvent extends CityEvent {
    private final CongestionLevel congestionLevel;

    public TrafficCongestionEvent(String streetName, CongestionLevel congestionLevel) {
        super(streetName);
        this.congestionLevel = congestionLevel;
    }

    public CongestionLevel getCongestionLevel() {
        return congestionLevel;
    }
}