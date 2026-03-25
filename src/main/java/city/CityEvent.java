package city;

public abstract class CityEvent {
    private final CameraType cameraType;
    private final String streetName;

    public CityEvent(CameraType cameraType, String streetName) {
        this.cameraType = cameraType;
        this.streetName = streetName;
    }

    public CameraType getCameraType() {
        return cameraType;
    }

    public String getStreetName() {
        return streetName;
    }
}