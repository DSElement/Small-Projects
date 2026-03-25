package city;

abstract public class CityEvent {
    private CameraType cameraType; 
    private String streetName;

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