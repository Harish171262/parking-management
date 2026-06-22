# 🏎️ Park Ops - Automated Parking Management System

Park Ops is a high-performance, full-stack monolith parking management solution. It features an interactive, real-time frontend dashboard with live canvas animations, a multi-level slot allocation engine, a dynamic billing/tariff processor, and an asynchronous Java server backed by a scalable MongoDB layer.

The entire application is containerized using Docker and deployed as a unified full-stack service on Render.

---

## 🚀 Key Features

* **Real-Time Layout Animation:** Live visual tracking of vehicle entries and exits on **Level 1** via dynamic CSS transitions and asset translation animations.
* **Multi-Level Allocation Tracking:** Automated tracking across multiple zones (`LEVEL1`, `LEVEL2`, `BASEMENT`) with auto-updating utility bar indicators and level occupancy summaries.
* **Dynamic Tariff & Penalty Processor:** Automatically tracks connection session times, processes elapsed hours, computes base vehicle type rates, and assesses overstay penalties.
* **Role-Based Views:** Client-side route blocking and administrative state customization using persistent `localStorage` session handling (`👑 Admin` vs `👤 User`).
* **Instant UPI Payment Generation:** Integrates server-side string parameters to generate real-time QR codes for cashless UPI transactions.
* **Audit-Trail Logging:** Persistent storage and live chronological rendering of operations history logs for entry/exit events.

---

## 🛠️ Tech Stack

* **Frontend:** Vanilla HTML5, CSS3 (Flexbox/Grid, Custom Keyframe Transitions), JavaScript (ES6, Asynchronous Fetch Architecture).
* **Backend:** Java (OpenJDK 17), Custom HTTP Routing Engine, MongoDB Java Driver Engine (v3.12.14 Unified Uber-JAR).
* **Database:** MongoDB (Cloud Aggregations via MongoDB Atlas).
* **DevOps & Infrastructure:** Docker (Multi-stage compilation), Linux Containerization, Render Cloud Hosting Platform.

---


live demo:

https://parking-management-1-fb35.onrender.com/


## 📁 Repository Structure

```text
├── public/                # Static UI/UX Web Assets
│   ├── index.html         # Main Operational Control Panel
│   ├── login.html         # Portal Authentication View
│   ├── script.js          # DOM Manipulation & Async API Broker
│   └── style.css          # Structural Grids, Layouts, & Animations
├── ParkingServer.java     # Primary Multi-Threaded Java HTTP/API Server
├── MongoDBHelper.java     # Database Connectivity Layer & Aggregate Query Specs
├── Dockerfile             # Monolith Container Engine Definition Build Configuration
└── README.md              # Documentation Asset
