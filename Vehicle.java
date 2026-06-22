public class Vehicle {
    private final String plateNumber;
    private final VehicleSize size;
    private final long entryTime;
    private final String ownerName;   // NEW: Owner's name
    private final String ownerPhone;  // NEW: Owner's phone number
    private final String vehicleType; // NEW: car / bike / bus / truck

    public Vehicle(String plateNumber, VehicleSize size,
                   String ownerName, String ownerPhone, String vehicleType) {
        this.plateNumber = plateNumber;
        this.size = size;
        this.ownerName = ownerName;
        this.ownerPhone = ownerPhone;
        this.vehicleType = vehicleType;
        this.entryTime = System.currentTimeMillis();
    }

    public String getPlateNumber() { return plateNumber; }
    public VehicleSize getSize()   { return size; }
    public long getEntryTime()     { return entryTime; }
    public String getOwnerName()   { return ownerName; }
    public String getOwnerPhone()  { return ownerPhone; }
    public String getVehicleType() { return vehicleType; }
}