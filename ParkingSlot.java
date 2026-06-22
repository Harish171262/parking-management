public class ParkingSlot {
    private final String id;
    private final VehicleSize size;
    private final String level;   // NEW: "LEVEL1", "LEVEL2", "BASEMENT"
    private boolean occupied;
    private Vehicle vehicle;

    public ParkingSlot(String id, VehicleSize size, String level) {
        this.id = id;
        this.size = size;
        this.level = level;
        this.occupied = false;
    }

    public String getId()       { return id; }
    public VehicleSize getSize(){ return size; }
    public String getLevel()    { return level; }  // NEW getter
    public boolean isOccupied() { return occupied; }
    public Vehicle getVehicle() { return vehicle; }

    public void park(Vehicle v) {
        this.vehicle = v;
        this.occupied = true;
    }

    public void vacate() {
        this.vehicle = null;
        this.occupied = false;
    }
}