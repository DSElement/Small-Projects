package city;

public class TrafficCongestionEvent extends CityEvent {
    private final CongestionLevel congestionLevel;

    public TrafficCongestionEvent(CameraType cameraType, String streetName, CongestionLevel congestionLevel) {
        super(cameraType, streetName);
        this.congestionLevel = congestionLevel;
    }

    public CongestionLevel getCongestionLevel() {
        return congestionLevel;
    }
}