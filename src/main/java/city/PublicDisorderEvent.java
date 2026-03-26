package city;

public class PublicDisorderEvent extends CityEvent {
    private final String disorderDescription;

    public PublicDisorderEvent(String streetName, String disorderDescription) {
        super(streetName);
        this.disorderDescription = disorderDescription;
    }

    public String getDisorderDescription() {
        return disorderDescription;
    }
}