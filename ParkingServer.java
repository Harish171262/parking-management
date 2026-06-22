import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.*;

public class ParkingServer {

    private static final ParkingLot parkingLot = new ParkingLot();

    // Simple session store: token -> role
    private static final Map<String, String> sessions = new HashMap<>();

    public static void main(String[] args) throws IOException {
        // ✅ Connect to MongoDB first!
        MongoDBHelper.connect();

        // ✅ Create default admin user (only first time)
        MongoDBHelper.saveUser("admin", "admin123", "admin");
        MongoDBHelper.saveUser("customer", "cust123", "customer");

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);




        server.createContext("/api/status",   ParkingServer::handleStatus);
        server.createContext("/api/entry",    ParkingServer::handleEntry);
        server.createContext("/api/bill",     ParkingServer::handleBill);
        server.createContext("/api/exit",     ParkingServer::handleExit);
        server.createContext("/api/parked",   ParkingServer::handleParked);
        server.createContext("/api/history",  ParkingServer::handleHistory);
        server.createContext("/api/login",    ParkingServer::handleLogin);
        server.createContext("/api/register", ParkingServer::handleRegister);
        server.createContext("/",             ParkingServer::handleStaticFiles);

        server.start();
        System.out.println("🚀 Server started! Open http://localhost:8080 in your browser.");
    }

    // ── API Handlers ──

    private static void handleStatus(HttpExchange e) throws IOException {
        sendJson(e, parkingLot.toJson());
    }

    private static void handleParked(HttpExchange e) throws IOException {
        sendJson(e, parkingLot.parkedVehiclesToJson());
    }

    private static void handleHistory(HttpExchange e) throws IOException {
        sendJson(e, parkingLot.historyToJson());
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, String> params = readFormBody(exchange);
        String username = params.getOrDefault("username", "").trim();
        String password = params.getOrDefault("password", "").trim();

        String role = MongoDBHelper.verifyLogin(username, password);
        if (role == null) {
            sendJson(exchange, "{\"success\":false,\"message\":\"Wrong username or password!\"}");
            return;
        }
        // Create a simple token
        String token = username + "-" + System.currentTimeMillis();
        sessions.put(token, role);
        sendJson(exchange, "{\"success\":true,\"role\":\"" + role + "\",\"token\":\"" + token + "\"}");
    }

    private static void handleRegister(HttpExchange exchange) throws IOException {
        Map<String, String> params = readFormBody(exchange);
        String username = params.getOrDefault("username", "").trim();
        String password = params.getOrDefault("password", "").trim();
        String role     = params.getOrDefault("role", "customer").trim();

        if (username.isEmpty() || password.isEmpty()) {
            sendJson(exchange, "{\"success\":false,\"message\":\"Username and password required!\"}");
            return;
        }
        boolean ok = MongoDBHelper.saveUser(username, password, role);
        if (ok) sendJson(exchange, "{\"success\":true,\"message\":\"Account created! Please login.\"}");
        else    sendJson(exchange, "{\"success\":false,\"message\":\"Username already taken!\"}");
    }

    private static void handleEntry(HttpExchange exchange) throws IOException {
        Map<String, String> params = readFormBody(exchange);

        String plate       = params.getOrDefault("plate",       "").trim().toUpperCase();
        String sizeStr     = params.getOrDefault("size",        "SMALL");
        String ownerName   = params.getOrDefault("ownerName",   "Unknown");
        String ownerPhone  = params.getOrDefault("ownerPhone",  "N/A");
        String vehicleType = params.getOrDefault("vehicleType", "car");
        String level       = params.getOrDefault("level",       null);

        if (plate.isEmpty()) {
            sendJson(exchange, "{\"success\":false,\"message\":\"Vehicle number is required.\"}");
            return;
        }
        if (parkingLot.isVehicleParked(plate)) {
            sendJson(exchange, "{\"success\":false,\"message\":\"This vehicle is already parked.\"}");
            return;
        }

        VehicleSize size = VehicleSize.valueOf(sizeStr);
        ParkingSlot slot = parkingLot.findAvailableSlot(size, level);
        if (slot == null) {
            sendJson(exchange, "{\"success\":false,\"message\":\"No " + size + " slot available.\"}");
            return;
        }

        Vehicle vehicle = new Vehicle(plate, size, ownerName, ownerPhone, vehicleType);
        parkingLot.parkVehicle(slot, vehicle);
        parkingLot.recordEntry(plate, size, slot.getId(), slot.getLevel(),
                               vehicle.getEntryTime(), ownerName, ownerPhone, vehicleType);

        // 💾 Save to MongoDB
        MongoDBHelper.saveEntryRecord(plate, ownerName, ownerPhone, vehicleType,
                                      size.name(), slot.getId(), slot.getLevel(), vehicle.getEntryTime());

        sendJson(exchange, "{\"success\":true,\"message\":\"Parked at slot " + slot.getId()
                + " on " + slot.getLevel() + "\",\"slotId\":\"" + slot.getId()
                + "\",\"level\":\"" + slot.getLevel() + "\"}");
    }

    private static void handleBill(HttpExchange exchange) throws IOException {
        Map<String, String> params = readQueryParams(exchange);
        String plate = params.getOrDefault("plate", "").trim().toUpperCase();

        ParkingSlot slot = parkingLot.findSlotByPlate(plate);
        if (slot == null) {
            sendJson(exchange, "{\"success\":false,\"message\":\"No parked vehicle found.\"}");
            return;
        }
        double[] calc = parkingLot.calculateFeeAndPenalty(slot.getVehicle());
        double fee     = calc[0];
        double penalty = calc[1];
        long   hours   = (long) calc[2];
        double total   = fee + penalty;

        sendJson(exchange, String.format(
            "{\"success\":true,\"slotId\":\"%s\",\"level\":\"%s\","
            + "\"hours\":%d,\"fee\":%.2f,\"penalty\":%.2f,\"total\":%.2f}",
            slot.getId(), slot.getLevel(), hours, fee, penalty, total));
    }

    private static void handleExit(HttpExchange exchange) throws IOException {
        Map<String, String> params = readFormBody(exchange);
        String plate       = params.getOrDefault("plate",       "").trim().toUpperCase();
        String paymentType = params.getOrDefault("paymentType", "UPI");

        ParkingSlot slot = parkingLot.findSlotByPlate(plate);
        if (slot == null) {
            sendJson(exchange, "{\"success\":false,\"message\":\"No parked vehicle found.\"}");
            return;
        }

        double[] calc  = parkingLot.calculateFeeAndPenalty(slot.getVehicle());
        double fee     = calc[0];
        double penalty = calc[1];
        long   hours   = (long) calc[2];
        double total   = fee + penalty;
        String slotId  = slot.getId();
        String level   = slot.getLevel();
        long   now     = System.currentTimeMillis();

        parkingLot.removeVehicle(slot);
        parkingLot.recordExit(plate, now, fee, penalty, paymentType);

        // 🔄 Update MongoDB
        MongoDBHelper.updateExitRecord(plate, now, fee, penalty, paymentType);

        sendJson(exchange, String.format(
            "{\"success\":true,\"message\":\"Exited slot %s on %s\","
            + "\"slotId\":\"%s\",\"level\":\"%s\","
            + "\"hours\":%d,\"fee\":%.2f,\"penalty\":%.2f,\"total\":%.2f}",
            slotId, level, slotId, level, hours, fee, penalty, total));
    }

    // ── Helpers ──

    private static Map<String, String> readFormBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes());
        return parseParams(body);
    }

    private static Map<String, String> readQueryParams(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        return query == null ? new HashMap<>() : parseParams(query);
    }

    private static Map<String, String> parseParams(String raw) {
        Map<String, String> result = new HashMap<>();
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            try { result.put(kv[0], kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : ""); }
            catch (Exception ignored) {}
        }
        return result;
    }

    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private static void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/login.html"; // 👈 Redirect to login first!

        File file = new File("public" + path);
        if (!file.exists()) {
            String msg = "404 Not Found";
            exchange.sendResponseHeaders(404, msg.length());
            exchange.getResponseBody().write(msg.getBytes());
            exchange.getResponseBody().close();
            return;
        }
        String contentType = "text/plain";
        if (path.endsWith(".html")) contentType = "text/html";
        else if (path.endsWith(".css")) contentType = "text/css";
        else if (path.endsWith(".js"))  contentType = "application/javascript";

        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}