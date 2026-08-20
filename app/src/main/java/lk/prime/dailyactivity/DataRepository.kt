package lk.prime.dailyactivity

/**
 * One contract for the app UI. During prototype/testing we can use an in-memory
 * implementation. When Firebase is connected, the UI can switch to a Firebase
 * implementation without redesigning screens.
 */
interface DataRepository {
    suspend fun registerStaff(profile: StaffProfile): Result<Unit>
    suspend fun getStaffBySalesCode(salesCode: String): Result<StaffProfile?>
    suspend fun getPendingRegistrations(): Result<List<StaffProfile>>
    suspend fun setApproval(salesCode: String, status: ApprovalStatus): Result<Unit>

    suspend fun saveAttendance(salesCode: String, record: AttendanceRecord): Result<Unit>
    suspend fun getTodayActivity(salesCode: String): Result<DailyActivity?>
    suspend fun saveTodayActivity(salesCode: String, activity: DailyActivity): Result<Unit>

    suspend fun getZoneSummaries(zone: String): Result<List<StaffDaySummary>>
    suspend fun getAllIslandSummaries(): Result<List<StaffDaySummary>>
}

class InMemoryDataRepository : DataRepository {
    private val staff = linkedMapOf<String, StaffProfile>()
    private val attendance = linkedMapOf<String, AttendanceRecord>()
    private val activities = linkedMapOf<String, DailyActivity>()

    override suspend fun registerStaff(profile: StaffProfile) = runCatching {
        require(profile.salesCode.isNotBlank())
        require(!staff.containsKey(profile.salesCode)) { "Sales Code already registered" }
        staff[profile.salesCode] = profile.copy(approvalStatus = ApprovalStatus.PENDING)
    }

    override suspend fun getStaffBySalesCode(salesCode: String) = runCatching { staff[salesCode] }

    override suspend fun getPendingRegistrations() = runCatching {
        staff.values.filter { it.approvalStatus == ApprovalStatus.PENDING }
    }

    override suspend fun setApproval(salesCode: String, status: ApprovalStatus) = runCatching {
        val current = requireNotNull(staff[salesCode])
        staff[salesCode] = current.copy(approvalStatus = status)
    }

    override suspend fun saveAttendance(salesCode: String, record: AttendanceRecord) = runCatching {
        attendance[salesCode] = record
    }

    override suspend fun getTodayActivity(salesCode: String) = runCatching { activities[salesCode] }

    override suspend fun saveTodayActivity(salesCode: String, activity: DailyActivity) = runCatching {
        val old = activities[salesCode]
        if (old?.dayLocked == true) error("Day is already locked")
        if (old?.planLocked == true) {
            require(activity.prospectingPlan == old.prospectingPlan)
            require(activity.followUpsPlan == old.followUpsPlan)
            require(activity.appointmentsPlan == old.appointmentsPlan)
            require(activity.presentationsPlan == old.presentationsPlan)
        }
        activities[salesCode] = activity
    }

    override suspend fun getZoneSummaries(zone: String) = runCatching { summaries().filter { it.zone == zone } }
    override suspend fun getAllIslandSummaries() = runCatching { summaries() }

    private fun summaries(): List<StaffDaySummary> = staff.values
        .filter { it.approvalStatus == ApprovalStatus.APPROVED }
        .map { person ->
            val a = activities[person.salesCode] ?: DailyActivity()
            val att = attendance[person.salesCode] ?: AttendanceRecord()
            StaffDaySummary(
                salesCode = person.salesCode,
                name = person.fullName,
                zone = person.zone,
                present = att.checkedIn,
                dayStarted = a.planLocked,
                dayEnded = a.dayLocked,
                totalPlan = a.prospectingPlan + a.followUpsPlan + a.appointmentsPlan + a.presentationsPlan,
                totalDone = a.prospectingDone + a.followUpsDone + a.appointmentsDone + a.presentationsDone
            )
        }
}
