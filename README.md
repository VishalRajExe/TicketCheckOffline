# TicketCheck Offline

A simple, 100% offline QR ticketing and verification system built for Android.

> **Note:** This is a simple initial version of the project. I will work more on this later to add more features and improvements.

---

## 💡 The Idea & Motivation

When hosting a show or event on ticketing platforms like **BookMyShow**, they typically charge around **10–15% commission** on every single ticket sold.

Instead of paying high commissions to third-party platforms, this project is built as a simple, self-hosted ticketing and verification system:

* **Generate unique QR codes** for every ticket purchased.
* **Share QR codes digitally** with customers (via WhatsApp, Email, etc.), providing digital tickets similar to BookMyShow.
* **Scan & verify QR codes** at the venue using a mobile device camera.
* **Instant verification status**:
  * 🟢 **Valid** — Ticket exists and hasn't been used yet (automatically marked as **Used** upon scanning).
  * 🔴 **Already Used** — Ticket was scanned previously (prevents double entry or reuse).
  * 🔴 **Invalid** — Ticket code does not exist in the record.
* **Prevent duplicate entry**: Once a ticket has been scanned, it is marked as **"Used"**, and any subsequent scan attempt will reject it.
* **Maintain full local records**: Keep track of all tickets, scan history, and verification statuses completely offline.

---

## ✨ Key Features

- 🎟️ **Ticket Generation & Management:** Create single or bulk ticket codes with custom prefixes.
- 📱 **QR Code Creation & Sharing:** Generate QR codes on-device and share directly with attendees.
- 📷 **Instant Offline QR Scanner:** Fast camera scanning using on-device ML Kit barcode recognition.
- 🔒 **Prevent Duplicate Entry:** Atomic status transitions ensure a QR code cannot be scanned twice.
- 📶 **100% Offline & Private:** Zero internet dependencies; works seamlessly in Airplane Mode. All data stays local in a SQLite Room database.
- 💾 **Data Backup & Restore:** Export and import event data as JSON for safe storage.

---

## 🛠️ Project Structure

```
app/src/main/java/com/ticketcheck/offline/
├── data/
│   ├── dao/             # Room DAOs (atomic ticket status updates)
│   ├── database/        # SQLite Room Database & converters
│   ├── entities/        # TicketEntity, EventEntity, ScanHistoryEntity
│   └── repository/      # Central verification logic (processScan)
├── domain/models/       # ScanOutcome, DashboardStats
├── qr/                  # QrCodeGenerator (ZXing)
├── scanner/             # QrAnalyzer (CameraX + ML Kit)
├── utils/               # CsvImporter, BackupManager, SettingsStore
└── ui/                  # Jetpack Compose UI (Screens, Theme, Navigation)
```

---

## 🚀 Quick Setup & Usage

1. **Open in Android Studio**: Open the `TicketCheckOffline` project folder in Android Studio.
2. **Build & Run**: Connect an Android device with USB Debugging enabled and click **Run (▶)**.
3. **Create Event**: Set up your show/event details and ticket prefix (e.g., `SHOW2026`).
4. **Generate Tickets**: Create tickets or generate demo tickets in **Manage Tickets / Settings**.
5. **Share QR Codes**: Select a ticket, load its QR code, and share it digitally.
6. **Scan at Venue**: Open **Scan Ticket**, grant camera permission, and scan incoming attendees' QR codes.

---

## 🔮 Future Roadmap

- Multi-device scanner sync for larger venue gates.
- Custom ticket PDF design generator.
- Scan analytics & entry rate metrics.

---

*Built with Kotlin, Jetpack Compose, Material 3, Room, CameraX, ZXing & ML Kit.*
