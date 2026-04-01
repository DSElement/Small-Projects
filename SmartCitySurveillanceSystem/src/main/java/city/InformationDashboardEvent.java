package city;

public class InformationDashboardEvent extends CityEvent {
    private final CityEventEnum eventType;

    public InformationDashboardEvent(String streetName, CityEventEnum eventType) {
        super(streetName);
        this.eventType = eventType;
    }

    public CityEventEnum getEventType() {
        return eventType;
    }
}