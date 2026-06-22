public enum VehicleSize {
    SMALL(10.0),   // Bikes
    MEDIUM(20.0),  // Cars
    LARGE(40.0);   // Buses & Trucks

    private final double ratePerHour;

    VehicleSize(double rate) {
        this.ratePerHour = rate;
    }

    public double getRatePerHour() {
        return ratePerHour;
    }
}