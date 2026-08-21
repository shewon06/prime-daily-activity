package lk.prime.dailyactivity

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseDataRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : DataRepository {
    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override suspend fun registerStaff(profile: StaffProfile): Result<Unit> = runCatching { val ref = db.collection("staff").document(profile.salesCode); require(profile.salesCode.isNotBlank()); require(!ref.get().await().exists()) { "Sales Code already registered" }; ref.set(profile.copy(approvalStatus = ApprovalStatus.PENDING).toMap()).await() }
    override suspend fun getStaffBySalesCode(salesCode: String): Result<StaffProfile?> = runCatching { db.collection("staff").document(salesCode).get().await().toStaffProfile() }
    override suspend fun getPendingRegistrations(): Result<List<StaffProfile>> = runCatching { db.collection("staff").whereEqualTo("approvalStatus", ApprovalStatus.PENDING.name).get().await().documents.mapNotNull { it.toStaffProfile() } }
    override suspend fun setApproval(salesCode: String, status: ApprovalStatus): Result<Unit> = runCatching { db.collection("staff").document(salesCode).update("approvalStatus", status.name).await() }
    override suspend fun updateProfilePhoto(salesCode: String, photoUrl: String): Result<Unit> = runCatching {
        require(isSavedProfilePhoto(photoUrl)) { "Invalid profile photo format." }
        db.collection("staff").document(salesCode).update("photoUri", photoUrl).await()
    }
    override suspend fun saveAttendance(salesCode: String, record: AttendanceRecord): Result<Unit> = runCatching {
        val ref = db.collection("dailyRecords").document("${todayKey()}_$salesCode")
        val existing = ref.get().await()
        @Suppress("UNCHECKED_CAST")
        val existingAttendance = existing.get("attendance") as? Map<String, Any?>
        val existingRecord = existingAttendance?.toAttendanceRecord()
        val mergedRecord = if (existingRecord?.checkedIn == true) {
            existingRecord.copy(
                checkedOut = existingRecord.checkedOut || record.checkedOut,
                checkOutTime = record.checkOutTime ?: existingRecord.checkOutTime
            )
        } else {
            record
        }
        ref.set(
            mapOf("salesCode" to salesCode, "date" to todayKey(), "attendance" to mergedRecord.toMap()),
            SetOptions.merge()
        ).await()
    }
    override suspend fun getTodayAttendance(salesCode: String): Result<AttendanceRecord?> = runCatching {
        val doc = db.collection("dailyRecords").document("${todayKey()}_$salesCode").get().await()
        @Suppress("UNCHECKED_CAST")
        (doc.get("attendance") as? Map<String, Any?>)?.toAttendanceRecord()
    }
    override suspend fun getTodayActivity(salesCode: String): Result<DailyActivity?> = runCatching { val doc = db.collection("dailyRecords").document("${todayKey()}_$salesCode").get().await(); @Suppress("UNCHECKED_CAST") ((doc.get("activity") as? Map<String, Any?>)?.toDailyActivity()) }
    override suspend fun saveTodayActivity(salesCode: String, activity: DailyActivity): Result<Unit> = runCatching {
        val ref = db.collection("dailyRecords").document("${todayKey()}_$salesCode")
        db.runTransaction { tx -> val old = tx.get(ref); @Suppress("UNCHECKED_CAST") val oldActivity = (old.get("activity") as? Map<String, Any?>)?.toDailyActivity(); if (oldActivity?.dayLocked == true) error("Day is already locked"); if (oldActivity?.planLocked == true) { require(activity.prospectingPlan == oldActivity.prospectingPlan); require(activity.followUpsPlan == oldActivity.followUpsPlan); require(activity.appointmentsPlan == oldActivity.appointmentsPlan); require(activity.presentationsPlan == oldActivity.presentationsPlan) }; tx.set(ref, mapOf("salesCode" to salesCode, "date" to todayKey(), "activity" to activity.toMap()), SetOptions.merge()) }.await()
    }

    override suspend fun getMonthlySalesTarget(salesCode: String, monthKey: String, dateKey: String): Result<MonthlySalesTarget> = runCatching {
        val doc = db.collection("monthlyTargets").document("${monthKey}_$salesCode").get().await()
        @Suppress("UNCHECKED_CAST")
        val achievements = doc.get("achievements") as? Map<String, Any?> ?: emptyMap()
        MonthlySalesTarget(
            monthKey = monthKey,
            targetAmount = (doc.get("targetAmount") as? Number)?.toLong() ?: 0L,
            targetLocked = doc.getBoolean("targetLocked") ?: false,
            achievedAmount = (doc.get("achievedAmount") as? Number)?.toLong() ?: 0L,
            todayAchievement = (achievements[dateKey] as? Number)?.toLong() ?: 0L
        )
    }

    override suspend fun lockMonthlySalesTarget(salesCode: String, monthKey: String, targetAmount: Long): Result<Unit> = runCatching {
        require(targetAmount > 0L) { "Enter a valid monthly target." }
        val ref = db.collection("monthlyTargets").document("${monthKey}_$salesCode")
        db.runTransaction { tx ->
            val old = tx.get(ref)
            require(old.getBoolean("targetLocked") != true) { "This month's target is already locked." }
            tx.set(
                ref,
                mapOf(
                    "salesCode" to salesCode,
                    "monthKey" to monthKey,
                    "targetAmount" to targetAmount,
                    "targetLocked" to true,
                    "lockedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
        }.await()
    }

    override suspend fun addSalesAchievement(salesCode: String, monthKey: String, dateKey: String, amount: Long): Result<Unit> = runCatching {
        require(amount > 0L) { "Enter a valid achievement amount." }
        require(dateKey.startsWith(monthKey)) { "Achievement date must be inside the current month." }
        val ref = db.collection("monthlyTargets").document("${monthKey}_$salesCode")
        db.runTransaction { tx ->
            val old = tx.get(ref)
            val currentTotal = (old.get("achievedAmount") as? Number)?.toLong() ?: 0L
            @Suppress("UNCHECKED_CAST")
            val oldAchievements = old.get("achievements") as? Map<String, Any?> ?: emptyMap()
            val achievements = oldAchievements.toMutableMap()
            val todayTotal = (achievements[dateKey] as? Number)?.toLong() ?: 0L
            achievements[dateKey] = todayTotal + amount
            tx.set(
                ref,
                mapOf(
                    "salesCode" to salesCode,
                    "monthKey" to monthKey,
                    "achievedAmount" to currentTotal + amount,
                    "achievements" to achievements,
                    "lastAchievementAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
        }.await()
    }

    override suspend fun getMonthlySalesReport(monthKey: String): Result<List<StaffMonthlySalesSummary>> = runCatching {
        val people = db.collection("staff")
            .whereEqualTo("approvalStatus", ApprovalStatus.APPROVED.name)
            .get().await().documents.mapNotNull { it.toStaffProfile() }

        val targetDocs = db.collection("monthlyTargets")
            .whereEqualTo("monthKey", monthKey)
            .get().await().documents

        val targetsByCode = targetDocs.mapNotNull { doc ->
            val code = doc.getString("salesCode") ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val achievementsMap = doc.get("achievements") as? Map<String, Any?> ?: emptyMap()
            val achievements = achievementsMap.entries.mapNotNull { (date, value) ->
                val amount = (value as? Number)?.toLong() ?: return@mapNotNull null
                SalesAchievementDay(date = date, amount = amount)
            }.sortedByDescending { it.date }

            code to StaffMonthlySalesSummary(
                salesCode = code,
                fullName = "",
                zone = "",
                targetAmount = (doc.get("targetAmount") as? Number)?.toLong() ?: 0L,
                targetLocked = doc.getBoolean("targetLocked") ?: false,
                achievedAmount = (doc.get("achievedAmount") as? Number)?.toLong() ?: achievements.sumOf { it.amount },
                achievements = achievements
            )
        }.toMap()

        people.map { person ->
            val target = targetsByCode[person.salesCode]
            StaffMonthlySalesSummary(
                salesCode = person.salesCode,
                fullName = person.fullName,
                zone = person.zone,
                targetAmount = target?.targetAmount ?: 0L,
                targetLocked = target?.targetLocked ?: false,
                achievedAmount = target?.achievedAmount ?: 0L,
                achievements = target?.achievements.orEmpty()
            )
        }.sortedWith(
            compareByDescending<StaffMonthlySalesSummary> { it.achievementPercent }
                .thenByDescending { it.achievedAmount }
                .thenBy { it.fullName }
        )
    }

    override suspend fun getZoneSummaries(zone: String) = runCatching { summaries(zone, false) }
    override suspend fun getAllIslandSummaries() = runCatching { summaries(null, true) }

    override suspend fun getMonthlyAttendance(salesCode: String, monthKey: String): Result<List<AttendanceDayDetail>> = runCatching {
        db.collection("dailyRecords").whereEqualTo("salesCode", salesCode).get().await().documents.mapNotNull { doc ->
            val date = doc.getString("date") ?: return@mapNotNull null
            if (!date.startsWith(monthKey)) return@mapNotNull null
            @Suppress("UNCHECKED_CAST") val att = doc.get("attendance") as? Map<String, Any?> ?: return@mapNotNull null
            if (att["checkedIn"] as? Boolean != true) return@mapNotNull null
            AttendanceDayDetail(date, att["checkInTime"] as? String, att["checkOutTime"] as? String)
        }.sortedByDescending { it.date }
    }

    override suspend fun getMonthlyAttendanceReport(monthKey: String): Result<List<StaffMonthlyAttendanceSummary>> = runCatching {
        val people = db.collection("staff")
            .whereEqualTo("approvalStatus", ApprovalStatus.APPROVED.name)
            .get().await().documents.mapNotNull { it.toStaffProfile() }

        val rowsByStaff = db.collection("dailyRecords").get().await().documents.mapNotNull { doc ->
            val date = doc.getString("date") ?: return@mapNotNull null
            if (!date.startsWith(monthKey)) return@mapNotNull null
            val code = doc.getString("salesCode") ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val att = doc.get("attendance") as? Map<String, Any?> ?: return@mapNotNull null
            if (att["checkedIn"] as? Boolean != true) return@mapNotNull null
            code to AttendanceDayDetail(
                date = date,
                checkInTime = att["checkInTime"] as? String,
                checkOutTime = att["checkOutTime"] as? String
            )
        }.groupBy({ it.first }, { it.second })

        people.map { person ->
            StaffMonthlyAttendanceSummary(
                salesCode = person.salesCode,
                fullName = person.fullName,
                zone = person.zone,
                attendance = rowsByStaff[person.salesCode].orEmpty().sortedByDescending { it.date }
            )
        }.sortedWith(compareBy<StaffMonthlyAttendanceSummary> { it.zone }.thenBy { it.fullName })
    }

    private suspend fun summaries(zone: String?, includeHistory: Boolean): List<StaffDaySummary> {
        val staffQuery = if (zone == null) db.collection("staff").whereEqualTo("approvalStatus", ApprovalStatus.APPROVED.name) else db.collection("staff").whereEqualTo("approvalStatus", ApprovalStatus.APPROVED.name).whereEqualTo("zone", zone)
        val people = staffQuery.get().await().documents.mapNotNull { it.toStaffProfile() }
        val todayRecords = db.collection("dailyRecords").whereEqualTo("date", todayKey()).get().await().documents; val recordsBySalesCode = todayRecords.associateBy { it.getString("salesCode") }
        val workedDays = if (includeHistory) db.collection("dailyRecords").get().await().documents.mapNotNull { d -> val code = d.getString("salesCode") ?: return@mapNotNull null; @Suppress("UNCHECKED_CAST") val att = d.get("attendance") as? Map<String, Any?>; if (att?.get("checkedIn") as? Boolean == true) code else null }.groupingBy { it }.eachCount() else emptyMap()
        return people.map { p -> val doc = recordsBySalesCode[p.salesCode]; @Suppress("UNCHECKED_CAST") val a = (doc?.get("activity") as? Map<String, Any?>)?.toDailyActivity() ?: DailyActivity(); @Suppress("UNCHECKED_CAST") val att = doc?.get("attendance") as? Map<String, Any?>
            StaffDaySummary(p.salesCode,p.fullName,p.zone,att?.get("checkedIn") as? Boolean ?: false,a.planLocked,a.dayLocked,a.prospectingPlan+a.followUpsPlan+a.appointmentsPlan+a.presentationsPlan,a.prospectingDone+a.followUpsDone+a.appointmentsDone+a.presentationsDone,a.prospectingPlan,a.prospectingDone,a.followUpsPlan,a.followUpsDone,a.appointmentsPlan,a.appointmentsDone,a.presentationsPlan,a.presentationsDone,att?.get("checkInTime") as? String,att?.get("checkOutTime") as? String,workedDays[p.salesCode] ?: 0) }
    }
}

private fun StaffProfile.toMap() = mapOf("salesCode" to salesCode,"fullName" to fullName,"mobile" to mobile,"zone" to zone,"role" to role.name,"photoUri" to photoUri,"approvalStatus" to approvalStatus.name)
private fun AttendanceRecord.toMap() = mapOf("checkedIn" to checkedIn,"checkedOut" to checkedOut,"checkInTime" to checkInTime,"checkOutTime" to checkOutTime)
private fun Map<String, Any?>.toAttendanceRecord() = AttendanceRecord(
    checkedIn = get("checkedIn") as? Boolean ?: false,
    checkedOut = get("checkedOut") as? Boolean ?: false,
    checkInTime = get("checkInTime") as? String,
    checkOutTime = get("checkOutTime") as? String
)
private fun DailyActivity.toMap() = mapOf("prospectingPlan" to prospectingPlan,"followUpsPlan" to followUpsPlan,"appointmentsPlan" to appointmentsPlan,"presentationsPlan" to presentationsPlan,"prospectingDone" to prospectingDone,"followUpsDone" to followUpsDone,"appointmentsDone" to appointmentsDone,"presentationsDone" to presentationsDone,"planLocked" to planLocked,"dayLocked" to dayLocked)
private fun com.google.firebase.firestore.DocumentSnapshot.toStaffProfile(): StaffProfile? {
    if (!exists()) return null
    val storedPhoto = getString("photoUri")?.takeIf(::isSavedProfilePhoto)
    return StaffProfile(
        getString("salesCode") ?: id,
        getString("fullName") ?: "",
        getString("mobile") ?: "",
        getString("zone") ?: "",
        runCatching { UserRole.valueOf(getString("role") ?: UserRole.STAFF.name) }.getOrDefault(UserRole.STAFF),
        storedPhoto,
        runCatching { ApprovalStatus.valueOf(getString("approvalStatus") ?: ApprovalStatus.PENDING.name) }.getOrDefault(ApprovalStatus.PENDING)
    )
}
private fun Map<String, Any?>.toDailyActivity() = DailyActivity((get("prospectingPlan") as? Number)?.toInt() ?: 0,(get("followUpsPlan") as? Number)?.toInt() ?: 0,(get("appointmentsPlan") as? Number)?.toInt() ?: 0,(get("presentationsPlan") as? Number)?.toInt() ?: 0,(get("prospectingDone") as? Number)?.toInt() ?: 0,(get("followUpsDone") as? Number)?.toInt() ?: 0,(get("appointmentsDone") as? Number)?.toInt() ?: 0,(get("presentationsDone") as? Number)?.toInt() ?: 0,get("planLocked") as? Boolean ?: false,get("dayLocked") as? Boolean ?: false)
