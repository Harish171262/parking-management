import java.util.*;

public class ParkingLot {
    private final List<ParkingSlot> slots = new ArrayList<>();
    private final List<HistoryRecord> history = new ArrayList<>();

    // ✅ FREE slots after 12 hours = overstay = penalty kicks in
    public static final int FREE_HOURS = 12;
    public static final double PENALTY_PER_HOUR = 50.0;

    public ParkingLot() {
        // ── LEVEL 1 ──
        for (int i = 1; i <= 4; i++) slots.add(new ParkingSlot("L1-S" + i, VehicleSize.SMALL,  "LEVEL1"));
        for (int i = 1; i <= 4; i++) slots.add(new ParkingSlot("L1-M" + i, VehicleSize.MEDIUM, "LEVEL1"));
        for (int i = 1; i <= 2; i++) slots.add(new ParkingSlot("L1-L" + i, VehicleSize.LARGE,  "LEVEL1"));

        // ── LEVEL 2 ──
        for (int i = 1; i <= 4; i++) slots.add(new ParkingSlot("L2-S" + i, VehicleSize.SMALL,  "LEVEL2"));
        for (int i = 1; i <= 4; i++) slots.add(new ParkingSlot("L2-M" + i, VehicleSize.MEDIUM, "LEVEL2"));
        for (int i = 1; i <= 2; i++) slots.add(new ParkingSlot("L2-L" + i, VehicleSize.LARGE,  "LEVEL2"));

        // ── BASEMENT ──
        for (int i = 1; i <= 4; i++) slots.add(new ParkingSlot("BM-S" + i, VehicleSize.SMALL,  "BASEMENT"));
        for (int i = 1; i <= 4; i++) slots.add(new ParkingSlot("BM-M" + i, VehicleSize.MEDIUM, "BASEMENT"));
        for (int i = 1; i <= 2; i++) slots.add(new ParkingSlot("BM-L" + i, VehicleSize.LARGE,  "BASEMENT"));
    }

    public List<ParkingSlot> getSlots() { return slots; }

    // Find available slot — you can pick a preferred level!
    public ParkingSlot findAvailableSlot(VehicleSize size, String preferredLevel) {
        // First try preferred level
        for (ParkingSlot s : slots)
            if (s.getSize() == size && !s.isOccupied() &&
                (preferredLevel == null || s.getLevel().equals(preferredLevel))) return s;
        // Fallback: any level
        if (preferredLevel != null)
            for (ParkingSlot s : slots)
                if (s.getSize() == size && !s.isOccupied()) return s;
        return null;
    }

    public ParkingSlot findSlotByPlate(String plate) {
        for (ParkingSlot s : slots)
            if (s.isOccupied() && s.getVehicle().getPlateNumber().equals(plate)) return s;
        return null;
    }

    public boolean isVehicleParked(String plate) { return findSlotByPlate(plate) != null; }
    public void parkVehicle(ParkingSlot slot, Vehicle v) { slot.park(v); }
    public void removeVehicle(ParkingSlot slot) { slot.vacate(); }

    // Calculate fee + penalty
    public double[] calculateFeeAndPenalty(Vehicle vehicle) {
        long millis = System.currentTimeMillis() - vehicle.getEntryTime();
        long hours  = (long) Math.ceil(millis / (1000.0 * 60 * 60));
        if (hours < 1) hours = 1;

        double fee     = hours * vehicle.getSize().getRatePerHour();
        double penalty = 0;
        if (hours > FREE_HOURS) {
            penalty = (hours - FREE_HOURS) * PENALTY_PER_HOUR; // ₹50/hr overstay
        }
        return new double[]{fee, penalty, hours};
    }

    public void recordEntry(String plate, VehicleSize size, String slotId, String level,
                            long entryTime, String ownerName, String ownerPhone, String vehicleType) {
        history.add(new HistoryRecord(plate, size, slotId, level,
                                      entryTime, ownerName, ownerPhone, vehicleType));
    }

    public void recordExit(String plate, long exitTime, double fee,
                           double penalty, String paymentType) {
        for (int i = history.size() - 1; i >= 0; i--) {
            HistoryRecord r = history.get(i);
            if (r.getPlate().equals(plate) && r.isOpen()) {
                r.closeRecord(exitTime, fee, penalty, paymentType);
                return;
            }
        }
    }

    public List<HistoryRecord> getHistory() { return history; }

    // ── JSON builders ──
    public String toJson() {
        StringBuilder sb = new StringBuilder("{\"slots\":[");
        for (int i = 0; i < slots.size(); i++) {
            ParkingSlot s = slots.get(i);
            sb.append("{\"id\":\"").append(s.getId()).append("\",")
              .append("\"size\":\"").append(s.getSize()).append("\",")
              .append("\"level\":\"").append(s.getLevel()).append("\",")
              .append("\"occupied\":").append(s.isOccupied()).append(",")
              .append("\"plate\":")
              .append(s.isOccupied() ? ("\"" + s.getVehicle().getPlateNumber() + "\"") : "null")
              .append("}");
            if (i < slots.size() - 1) sb.append(",");
        }
        return sb.append("]}").toString();
    }

    public String parkedVehiclesToJson() {
        StringBuilder sb = new StringBuilder("{\"vehicles\":[");
        boolean first = true;
        for (ParkingSlot s : slots) {
            if (s.isOccupied()) {
                if (!first) sb.append(",");
                sb.append("{\"plate\":\"").append(s.getVehicle().getPlateNumber()).append("\",")
                  .append("\"slotId\":\"").append(s.getId()).append("\",")
                  .append("\"level\":\"").append(s.getLevel()).append("\"}");
                first = false;
            }
        }
        return sb.append("]}").toString();
    }

    public String historyToJson() {
        StringBuilder sb = new StringBuilder("{\"history\":[");
        for (int i = history.size() - 1; i >= 0; i--) {
            sb.append(history.get(i).toJson());
            if (i > 0) sb.append(",");
        }
        return sb.append("]}").toString();
    }
}