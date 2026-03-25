package city;

public class PublicDisorderEvent extends CityEvent {
    private final String disorderDescription;

    public PublicDisorderEvent(CameraType cameraType, String streetName, String disorderDescription) {
        super(cameraType, streetName);
        this.disorderDescription = disorderDescription;
    }

    public String getDisorderDescription() {
        return disorderDescription;
    }
}