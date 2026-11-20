# Privacy Policy for E6B Pro

**Effective Date:** November 18, 2026  
**Application Name:** E6B Pro (Package ID: `com.aistudio.e6bflightcomputer.vxnqpl`)

---

## 1. Overview

**E6B Pro** ("the App") is an offline-first aviation flight computer and cockpit calculation utility designed for pilots, flight students, and aviation enthusiasts. We are committed to protecting your privacy. Because the App is engineered to operate reliably in the cockpit without requiring an internet connection, **all flight calculations, aircraft weight-and-balance profiles, navigation logs (NavLogs), and saved logbook entries are stored locally on your device.**

---

## 2. Information We Collect and Store

### A. Locally Stored User Data (On-Device Only)
When you use E6B Pro, the following information you create or import is stored strictly in an on-device SQLite (Room) database inside the App's private storage:
- **Aircraft Profiles:** Custom aircraft names, tail numbers, weight-and-balance station arms/weights, CG envelope coordinates, fuel burn rates, and cruise airspeeds.
- **Navigation Logs (NavLogs):** Waypoint identifiers, altitudes, courses, distances, wind vectors, and fuel calculations.
- **Saved Cockpit Calculations:** Snapshots of wind triangle, atmosphere/density altitude, holding pattern, VNAV, or weight-and-balance calculations that you choose to save to the in-app logbook.

**We do not transmit, upload, sync, or back up this data to any external server or cloud database.**

### B. User-Initiated File Import & Export
- When you use the **Import / Export Phone E6B** feature to load or save `.json` or `.csv` files, the App uses the standard Android System File Picker (`Storage Access Framework`).
- The App only accesses the specific single file you explicitly select or create, and does not request or use broad storage permissions (`READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`).

### C. No Personal Identifiers, Location, or Telemetry
- **No Account Required:** E6B Pro does not require user registration, sign-in, email addresses, or phone numbers.
- **No Location Tracking:** The App does not access your device's GPS or background/foreground location.
- **No Analytics or Advertising SDKs:** The App contains no third-party advertising networks, behavioral trackers, or analytics SDKs.

---

## 3. Data Sharing and Disclosure

Because E6B Pro does not collect or transmit any user data off your device:
- We **never** sell, rent, trade, or share your personal or flight data with third parties.
- Your data leaves your device **only** if you explicitly export a `.json` backup file or copy an export payload to your device clipboard and share it yourself.

---

## 4. Data Retention and Deletion

All data created within E6B Pro remains on your device under your sole control:
- You can delete individual NavLog legs, custom aircraft profiles, or saved logbook calculations directly within the App at any time.
- Uninstalling E6B Pro or clearing the App's storage in your Android system settings permanently deletes all locally stored profiles, NavLogs, and calculation history.

---

## 5. Children's Privacy

E6B Pro does not knowingly collect any personal information from anyone, including children under the age of 13.

---

## 6. Operational Aviation Disclaimer

E6B Pro is designed as a supplemental situational-awareness, flight-training, and planning aid. Pilots in Command (PIC) must always verify all weight-and-balance, performance, fuel, and navigation calculations against official FAA/EASA-approved Pilot's Operating Handbooks (POH), Aircraft Flight Manuals (AFM), and current aeronautical charts prior to flight.

---

## 7. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect changes in App functionality or regulatory requirements. Any updates will be posted with a revised "Effective Date" at the top of this policy.

---

## 8. Contact Us

If you have any questions or concerns regarding this Privacy Policy or E6B Pro's data practices, please contact the developer at:
- **Email:** eagledares@gmail.com
