# Firebase setup for PRIME Daily Activity

The Android UI/local workflow is being built first. Shared live data will use Firebase Authentication + Cloud Firestore + Cloud Storage.

## Why
- One central staff database across phones.
- Staff authentication.
- Registration approval state.
- Daily attendance and locked plans/results.
- Zone-scoped manager access.
- All-Island admin access.
- Profile photo storage.

## Required Firebase project
Create one Firebase project for PRIME and register Android app:

`lk.prime.dailyactivity`

Download `google-services.json` from Firebase and place it in `app/google-services.json` locally. Do NOT commit private service-account keys.

## Firestore collections

### users/{uid}
- salesCode
- fullName
- mobile
- zone
- role: STAFF | ZONAL_MANAGER | ADMIN
- approvalStatus: PENDING | APPROVED | REJECTED
- photoUrl
- active

### dailyActivities/{uid_yyyyMMdd}
- uid
- salesCode
- zone
- date
- attendanceCheckedIn
- checkInTimestamp
- prospectingPlan
- followUpsPlan
- appointmentsPlan
- presentationsPlan
- planLocked
- planLockedAt
- prospectingDone
- followUpsDone
- appointmentsDone
- presentationsDone
- dayLocked
- dayLockedAt

## Locking rule
Once `planLocked` becomes true, the client UI must not allow plan fields to change. Once `dayLocked` becomes true, Done values must also become read-only. Firestore Security Rules must enforce these rules as well; UI-only locking is not sufficient.

## Roles
- STAFF: own profile and own daily records only.
- ZONAL_MANAGER: read staff/daily records only for assigned zone; no editing staff locked plans.
- ADMIN: All-Island read access and registration/role approval functions.

## Production security
Use Firebase Authentication and restrictive Firestore/Storage Security Rules. Enable App Check before production rollout.
