package city;

public class TrafficCongestionEvent extends CityEvent {
    private final CongestionLevelEnum congestionLevel;

    public TrafficCongestionEvent(String streetName, CongestionLevelEnum congestionLevel) {
        super(streetName);
        this.congestionLevel = congestionLevel;
    }

    public CongestionLevelEnum getCongestionLevel() {
        return congestionLevel;
    }
}