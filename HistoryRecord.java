import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class HistoryRecord {
    private final String plate;
    private final VehicleSize size;
    private final String slotId;
    private final String level;         // NEW
    private final long entryTime;
    private long exitTime;
    private double fee;
    private double penalty;             // NEW: overstay penalty
    private final String ownerName;     // NEW
    private final String ownerPhone;    // NEW
    private final String vehicleType;   // NEW
    private String paymentType;         // NEW: "UPI", "CASH", "CARD"
    private String paymentStatus;       // NEW: "PAID", "PENDING"

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.systemDefault());

    public HistoryRecord(String plate, VehicleSize size, String slotId, String level,
                         long entryTime, String ownerName, String ownerPhone, String vehicleType) {
        this.plate = plate;
        this.size = size;
        this.slotId = slotId;
        this.level = level;
        this.entryTime = entryTime;
        this.ownerName = ownerName;
        this.ownerPhone = ownerPhone;
        this.vehicleType = vehicleType;
        this.paymentType = "PENDING";
        this.paymentStatus = "PENDING";
    }

    public String getPlate()  { return plate; }
    public boolean isOpen()   { return exitTime == 0; }
    public String getLevel()  { return level; }

    public void closeRecord(long exitTime, double fee, double penalty, String paymentType) {
        this.exitTime = exitTime;
        this.fee = fee;
        this.penalty = penalty;
        this.paymentType = paymentType;
        this.paymentStatus = "PAID";
    }

    public String toJson() {
        String entryFormatted = FMT.format(Instant.ofEpochMilli(entryTime));
        String exitFormatted  = exitTime > 0 ? FMT.format(Instant.ofEpochMilli(exitTime)) : "";
        return "{"
            + "\"plate\":\""         + plate         + "\","
            + "\"size\":\""          + size           + "\","
            + "\"slotId\":\""        + slotId         + "\","
            + "\"level\":\""         + level          + "\","
            + "\"ownerName\":\""     + ownerName      + "\","
            + "\"ownerPhone\":\""    + ownerPhone     + "\","
            + "\"vehicleType\":\""   + vehicleType    + "\","
            + "\"entryTime\":"       + entryTime      + ","
            + "\"exitTime\":"        + exitTime       + ","
            + "\"entryFormatted\":\"" + entryFormatted + "\","
            + "\"exitFormatted\":\""  + exitFormatted  + "\","
            + "\"fee\":"             + fee            + ","
            + "\"penalty\":"         + penalty        + ","
            + "\"paymentType\":\""   + paymentType    + "\","
            + "\"paymentStatus\":\"" + paymentStatus  + "\""
            + "}";
    }
}