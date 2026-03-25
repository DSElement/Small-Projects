package city;

public class PublicDisorderEvent extends CityEvent {
    private String disorderDescription;

    public PublicDisorderEvent(CameraType cameraType, String streetName, String disorderDescription) {
        super(cameraType, streetName);
        this.disorderDescription = disorderDescription;
    }

    public CameraType getCameraType() {
        return super.getCameraType();
    }

    public String getStreetName() {
        return super.getStreetName();
    }

    public String getDisorderDescription() {
        return disorderDescription;
    }
}