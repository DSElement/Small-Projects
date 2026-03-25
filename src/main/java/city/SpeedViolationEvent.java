package city;

public class SpeedViolationEvent extends CityEvent {
    private double recordedSpeed;
    private String plateNumber;

    public SpeedViolationEvent(CameraType cameraType, String streetName, double recordedSpeed, String plateNumber) {
        super(cameraType, streetName);
        this.recordedSpeed = recordedSpeed;
        this.plateNumber = plateNumber;
    }

    public CameraType getCameraType() {
        return super.getCameraType();
    }

    public String getStreetName() {
        return super.getStreetName();
    }

    public double getRecordedSpeed() {
        return recordedSpeed;
    }

    public String getPlateNumber() {
        return plateNumber;
    }
}