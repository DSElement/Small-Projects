package city;

public class TrafficCongestionEvent extends CityEvent {
    private CongestionLevel congestionLevel;

    public TrafficCongestionEvent(CameraType cameraType, String streetName, CongestionLevel congestionLevel) {
        super(cameraType, streetName);
        this.congestionLevel = congestionLevel;
    }

    public CameraType getCameraType() {
        return super.getCameraType();
    }

    public String getStreetName() {
        return super.getStreetName();
    }

    public CongestionLevel getCongestionLevel() {
        return congestionLevel;
    }
}