package city;

public class SpeedViolationEvent extends CityEvent {
    private final double recordedSpeed;
    private final String plateNumber;

    public SpeedViolationEvent(String streetName, double recordedSpeed, String plateNumber) {
        super(streetName);
        this.recordedSpeed = recordedSpeed;
        this.plateNumber = plateNumber;
    }

    public double getRecordedSpeed() {
        return recordedSpeed;
    }

    public String getPlateNumber() {
        return plateNumber;
    }
}