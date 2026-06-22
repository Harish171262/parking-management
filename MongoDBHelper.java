import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBHelper {

    // Default fallback local configuration
    private static final String DEFAULT_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "parking_management";

    private static MongoClient mongoClient;
    private static MongoDatabase database;

    // 🔌 Connect to MongoDB (called once when server starts)
    public static void connect() {
        try {
            // 1. Attempt to fetch the environment variable configured on Render
            String envUri = System.getenv("MONGODB_URI");
            String connectionString;

            if (envUri != null && !envUri.trim().isEmpty()) {
                connectionString = envUri;
                System.out.println("🌐 Production Cloud Database URI found. Initializing connection handshake...");
            } else {
                connectionString = DEFAULT_URI;
                System.out.println("💻 No production URI found. Falling back to Local Environment...");
            }

            // 2. Instantiate connection client with selected target location
            mongoClient = new MongoClient(new MongoClientURI(connectionString));
            database = mongoClient.getDatabase(DB_NAME);
            System.out.println("✅ MongoDB Connected! Active Database: " + DB_NAME);
        } catch (Exception e) {
            System.out.println("❌ MongoDB connection failed: " + e.getMessage());
        }
    }

    // 💾 Save a parking record when vehicle ENTERS
    public static void saveEntryRecord(String plate, String ownerName, String ownerPhone,
                                       String vehicleType, String size, String slotId,
                                       String level, long entryTime) {
        try {
            MongoCollection<Document> col = database.getCollection("parking_records");
            Document doc = new Document("plate", plate)
                .append("ownerName",   ownerName)
                .append("ownerPhone",  ownerPhone)
                .append("vehicleType", vehicleType)
                .append("size",        size)
                .append("slotId",      slotId)
                .append("level",       level)
                .append("entryTime",   entryTime)
                .append("exitTime",    0L)
                .append("fee",         0.0)
                .append("penalty",     0.0)
                .append("paymentType", "PENDING")
                .append("paymentStatus", "PENDING")
                .append("status",      "PARKED");
            col.insertOne(doc);
            System.out.println("💾 Entry saved to MongoDB for: " + plate);
        } catch (Exception e) {
            System.out.println("❌ MongoDB save error: " + e.getMessage());
        }
    }

    // 🔄 Update the record when vehicle EXITS
    public static void updateExitRecord(String plate, long exitTime, double fee,
                                        double penalty, String paymentType) {
        try {
            MongoCollection<Document> col = database.getCollection("parking_records");
            Document filter = new Document("plate", plate).append("status", "PARKED");
            Document update = new Document("$set", new Document()
                .append("exitTime",      exitTime)
                .append("fee",           fee)
                .append("penalty",       penalty)
                .append("paymentType",   paymentType)
                .append("paymentStatus", "PAID")
                .append("status",        "EXITED"));
            col.updateOne(filter, update);
            System.out.println("🔄 Exit record updated in MongoDB for: " + plate);
        } catch (Exception e) {
            System.out.println("❌ MongoDB update error: " + e.getMessage());
        }
    }

    // 👤 Save login user (admin or customer)
    public static boolean saveUser(String username, String password, String role) {
        try {
            MongoCollection<Document> col = database.getCollection("users");
            // Check if username already exists
            Document existing = col.find(new Document("username", username)).first();
            if (existing != null) return false; // username taken
            col.insertOne(new Document("username", username)
                .append("password", password) // In real apps, hash this!
                .append("role", role));
            return true;
        } catch (Exception e) {
            System.out.println("❌ Save user error: " + e.getMessage());
            return false;
        }
    }

    // 🔑 Verify login
    public static String verifyLogin(String username, String password) {
        try {
            MongoCollection<Document> col = database.getCollection("users");
            Document user = col.find(new Document("username", username)
                .append("password", password)).first();
            if (user != null) return user.getString("role"); // "admin" or "customer"
            return null;
        } catch (Exception e) {
            System.out.println("❌ Login check error: " + e.getMessage());
            return null;
        }
    }

    // 🔒 Close connection (optional, called on shutdown)
    public static void close() {
        if (mongoClient != null) mongoClient.close();
    }
}