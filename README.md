# TicketCheck Offline

A 100% offline Android app for scanning and verifying event tickets via QR code.
No server. No cloud database. No API. No login. No internet dependency at any
point after installation — the app works fully in Airplane Mode.

Built with Kotlin, Jetpack Compose, Material 3, Room, CameraX, and ML Kit's
on-device (bundled) barcode scanner.

---

## 1. What this app does

1. Before the event, you generate ticket codes (e.g. `SISH01`...`SISH100`) and
   a QR code for each one, and share those QR codes with customers.
2. At the venue, you open the **Scanner** screen on a single phone.
3. Customer shows their QR code → you point the camera at it.
4. The app looks the code up in its local database and instantly shows:
   - 🟢 **VALID** — ticket exists and hasn't been used → marked USED automatically.
   - 🔴 **ALREADY USED** — this code was already scanned once.
   - 🔴 **INVALID** — this code doesn't exist in your ticket list.
5. The scanner automatically returns to scanning mode ~1.5 seconds later.

Everything — tickets, statuses, scan history, event info — lives in a local
Room (SQLite) database file on the phone. `BACKUP DATA` exports it all to a
JSON file you can share/save; `RESTORE BACKUP` reads it back in.

## 2. Project structure

```
app/src/main/java/com/ticketcheck/offline/
  data/
    entities/       Room entities: TicketEntity, EventEntity, ScanHistoryEntity
    dao/             Room DAOs, including the atomic "mark used if valid" query
    database/        AppDatabase (Room) + type converters
    repository/      TicketRepository - the ONLY place that touches the DAOs;
                      contains the core scan() verification logic
  domain/models/     ScanOutcome (Valid/AlreadyUsed/Invalid), DashboardStats
  qr/                QrCodeGenerator (ZXing) + the QR payload abstraction
  scanner/           QrAnalyzer - CameraX ImageAnalysis.Analyzer using ML Kit
  utils/             CsvImporter, BackupManager, FeedbackHelper (sound/vibration),
                      SettingsStore (SharedPreferences)
  ui/
    theme/            Compose Material 3 theme
    navigation/       NavGraph + Routes
    screens/          One package per screen, each with a Screen.kt + ViewModel.kt:
                       home, scanner, manage, ticketlist, ticketdetail, qrgen,
                       history, backup, settings, onboarding
  MainActivity.kt
  TicketCheckApp.kt   Application class wiring the DB/repository/settings singletons

app/src/test/java/com/ticketcheck/offline/
  FakeDaos.kt                In-memory fake DAOs for JVM unit testing
  TicketVerificationTest.kt  Tests for the core scan logic (see below)
```

### The core verification logic

`TicketRepository.processScan(code)` implements exactly the rule from the spec:

```
ticket = findTicket(code)
if ticket == null            -> record INVALID, show INVALID
else if ticket.status == USED -> record ALREADY_USED, show ALREADY_USED
else                          -> atomically mark USED, record VALID, show VALID
```

The "atomically mark USED" step uses a single conditional SQL statement
(`UPDATE tickets SET status='USED' ... WHERE ticketCode=? AND status='VALID'`)
wrapped in a coroutine `Mutex`. This means two near-simultaneous calls for the
same code (e.g. the camera firing two callbacks for one physical scan) can
never both succeed — only one wins the transition to USED, the other is
correctly reported as ALREADY_USED. This is also covered by a unit test that
fires 5 concurrent scans of the same code and asserts exactly 1 succeeds.

### The QR payload abstraction

`qr/QrCodeGenerator.kt` defines a small `TicketQrPayload` interface with a
`PlainCodePayload` implementation (the QR just contains the raw ticket code).
If you ever want to upgrade to a signed/random token instead of the plain
code, you only need to write a new `TicketQrPayload` implementation and swap
it in one place — the generator, scanner, and database layers don't change.

---

## 3. Beginner-friendly setup instructions

You've never built an Android app before — here's exactly what to click.

### Step 1 — Install Android Studio
Download it from https://developer.android.com/studio and install it
(Windows, Mac, or Linux all work). Accept the default options during setup.

### Step 2 — Open the project
Launch Android Studio → **File → Open** → select the `TicketCheckOffline`
folder (the one containing `settings.gradle.kts`) → **OK**.

### Step 3 — Wait for Gradle sync
The bottom status bar will show "Gradle sync in progress". This can take a
few minutes the first time (it downloads the exact library versions listed
in `app/build.gradle.kts`). Just wait until it finishes — don't close the
window.

### Step 4 — Install the required SDK
If Android Studio shows a banner like "Install missing SDK package", click
**Install** and accept the license. Otherwise: **Tools → SDK Manager** →
make sure **Android 14.0 (API 34)** is checked → **Apply**.

### Step 5 — Connect your Android phone
Use a USB cable to connect your phone to your computer.

### Step 6 — Enable Developer Options on your phone
Go to **Settings → About phone** → tap **Build number** 7 times until it
says "You are now a developer".

### Step 7 — Enable USB debugging
Go to **Settings → System → Developer options** → turn on **USB debugging**.
A popup will appear on your phone the first time you connect — tap **Allow**.

### Step 8 — Run the app
In Android Studio, at the top toolbar you'll see a device dropdown (it
should show your phone's name) and a green ▶ **Run** button. Click ▶.
The app will build and install automatically, then open on your phone.

### Step 9 — Grant camera permission
The first time you open the Scanner screen, Android will ask for camera
permission — tap **Allow**. (This is the only permission the app requests.)

### Step 10 — Create a test event
On first launch you'll see **Welcome / Create your first event**. Fill in
an event name (e.g. "SISH 2026"), date, venue, and ticket prefix (e.g.
"SISH"), then tap **CREATE EVENT**.

### Step 11 — Generate test tickets
From the home screen tap **SETTINGS → GENERATE 10 TEST TICKETS** (clearly
marked as demo data), or go to **MANAGE TICKETS → Generate** and create
however many you want.

### Step 12 — Generate / share a QR
Tap **GENERATE QR** on the home screen, type a ticket code (e.g. `SISH01`),
tap **LOAD**, then **SAVE QR** or **SHARE QR** (this opens Android's normal
share sheet — WhatsApp, email, etc. all work).

### Step 13 — Scan a QR
Tap **SCAN TICKET**, point the camera at the QR you just generated (you can
display it on a second phone or a printed page, or even just aim at the
saved image on screen). You should see a big green **VALID TICKET** result.

### Step 14 — Test duplicate scanning
Scan the exact same QR again — you should now see a red **ALREADY USED**
result, and the ticket's status is unchanged.

### Step 15 — Export a backup
Go to **BACKUP / EXPORT → BACKUP DATA**. A JSON file is created on the
phone and the Android share sheet opens so you can save/send it anywhere.

### Step 16 — Restore a backup
Go to **BACKUP / EXPORT → RESTORE BACKUP**, pick the JSON file you just
exported (or any previous one), confirm the warning dialog, and check that
your tickets and their statuses come back.

### Step 17 — Build a release APK
When you're ready to install this on the actual scanning phone without a
USB cable: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. When it
finishes, click the **locate** link in the notification to find the `.apk`
file (under `app/build/outputs/apk/debug/` or `/release/`), then copy it to
your phone and open it to install (you may need to allow "install from
unknown sources" the first time).

---

## 4. Running the unit tests

The core scan-verification logic has its own JVM unit tests that don't need
a device or emulator. In Android Studio: right-click
`app/src/test/java/com/ticketcheck/offline/TicketVerificationTest.kt` →
**Run 'TicketVerificationTest'**. Or from a terminal in the project root:

```
./gradlew testDebugUnitTest
```

Covered cases: valid ticket → USED; already-used ticket stays unchanged;
unregistered code is INVALID and never created; 5 simultaneous scans of the
same code only succeed once (duplicate camera detection); ticket status
persists across a simulated restart; duplicate manual ticket codes are
rejected; sequential generation pads codes correctly; backup/restore round
trip preserves statuses; resetting USED status makes a ticket scannable
again.

---

## 5. Offline guarantee

There is no `INTERNET` permission in `AndroidManifest.xml`, and nothing in
the codebase makes an HTTP/network call. The only permissions requested are
`CAMERA` (for scanning) and `VIBRATE` (for haptic feedback). ML Kit's
barcode scanner and ZXing's QR generator both run entirely on-device. Room
stores everything in a local SQLite file. You can verify this yourself by
turning on Airplane Mode and running through steps 12–16 above — everything
still works.

## 6. Notes on scale for a small event

This is intentionally simple — no accounts, no payments, no cloud sync, no
backend. Room + indexed lookups by `ticketCode` comfortably handle the
10–10,000 ticket range mentioned in the spec on a single phone. If you ever
outgrow "one phone, one event", that's a deliberately out-of-scope feature
for this version.
