package city;

public abstract class CityEvent {
    private final String streetName;

    public CityEvent(String streetName) {
        this.streetName = streetName;
    }

    public String getStreetName() {
        return streetName;
    }
}