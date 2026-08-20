# PRIME Daily Activity

Android-only daily field activity application for PRIME Agri Business & Plantations.

## Core workflow

1. Staff registration with Sales Code, staff details and profile photo.
2. New registrations remain pending until Admin/Management approval.
3. Approved staff can log in and record attendance.
4. Staff enters the morning activity plan using counts only (no customer names/locations required in v1).
5. Activities: Prospecting, Follow Ups, Appointments, Presentations.
6. `START MY DAY` permanently locks that day's PLAN counts.
7. During the day, staff may update DONE counts with simple +/- controls.
8. `END MY DAY` shows Plan vs Done and achievement, then locks the completed day.
9. Staff can view their own performance/history.
10. Zonal Managers can view only staff/data belonging to their Zone.
11. Admin/Management can view All-Island data and drill down Zone -> Staff -> Daily performance.
12. Locked plans are view-only for managers/admin in normal operation.

## Product principle

**TYPE LESS — TAP MORE**

## Visual identity

PRIME dark green + gold, professional agriculture/plantation identity. Use the official PRIME logo asset when added to the project.

## Initial Android stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM-style UI/state separation
- Cloud authentication/database/storage will be connected after the local app flow is stable.

## Build phases

### Phase 1 — UI + local workflow
- Login / registration
- Registration approval states
- Staff home
- Attendance
- Morning plan
- Plan locking
- Done-count updates
- End-day report and locking
- Staff history
- Zonal Manager dashboard
- All-Island Management dashboard

### Phase 2 — Shared live data
- Authentication
- Central staff/zone database
- Photo storage
- Role-based permissions
- Manager/Admin dashboards using live data

### Phase 3 — Test release
- Generate Android APK
- Small staff pilot
- Fix workflow/UI issues
- Production release preparation
