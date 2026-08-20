package lk.prime.dailyactivity

interface DataRepository {
    suspend fun registerStaff(profile: StaffProfile): Result<Unit>
    suspend fun getStaffBySalesCode(salesCode: String): Result<StaffProfile?>
    suspend fun getPendingRegistrations(): Result<List<StaffProfile>>
    suspend fun setApproval(salesCode: String, status: ApprovalStatus): Result<Unit>
    suspend fun updateProfilePhoto(salesCode: String, photoUrl: String): Result<Unit>
    suspend fun saveAttendance(salesCode: String, record: AttendanceRecord): Result<Unit>
    suspend fun getTodayActivity(salesCode: String): Result<DailyActivity?>
    suspend fun saveTodayActivity(salesCode: String, activity: DailyActivity): Result<Unit>
    suspend fun getZoneSummaries(zone: String): Result<List<StaffDaySummary>>
    suspend fun getAllIslandSummaries(): Result<List<StaffDaySummary>>
    suspend fun getMonthlyAttendance(salesCode: String, monthKey: String): Result<List<AttendanceDayDetail>>
}

data class AttendanceDayDetail(
    val date: String,
    val checkInTime: String?,
    val checkOutTime: String?
)

class InMemoryDataRepository : DataRepository {
    private val staff = linkedMapOf<String, StaffProfile>()
    private val attendance = linkedMapOf<String, AttendanceRecord>()
    private val activities = linkedMapOf<String, DailyActivity>()

    override suspend fun registerStaff(profile: StaffProfile) = runCatching {
        require(profile.salesCode.isNotBlank()); require(!staff.containsKey(profile.salesCode)) { "Sales Code already registered" }
        staff[profile.salesCode] = profile.copy(approvalStatus = ApprovalStatus.PENDING)
    }
    override suspend fun getStaffBySalesCode(salesCode: String) = runCatching { staff[salesCode] }
    override suspend fun getPendingRegistrations() = runCatching { staff.values.filter { it.approvalStatus == ApprovalStatus.PENDING } }
    override suspend fun setApproval(salesCode: String, status: ApprovalStatus) = runCatching { val current = requireNotNull(staff[salesCode]); staff[salesCode] = current.copy(approvalStatus = status) }
    override suspend fun updateProfilePhoto(salesCode: String, photoUrl: String) = runCatching {
        val current = requireNotNull(staff[salesCode])
        staff[salesCode] = current.copy(photoUri = photoUrl)
    }
    override suspend fun saveAttendance(salesCode: String, record: AttendanceRecord) = runCatching { attendance[salesCode] = record }
    override suspend fun getTodayActivity(salesCode: String) = runCatching { activities[salesCode] }
    override suspend fun saveTodayActivity(salesCode: String, activity: DailyActivity) = runCatching {
        val old = activities[salesCode]; if (old?.dayLocked == true) error("Day is already locked")
        if (old?.planLocked == true) { require(activity.prospectingPlan == old.prospectingPlan); require(activity.followUpsPlan == old.followUpsPlan); require(activity.appointmentsPlan == old.appointmentsPlan); require(activity.presentationsPlan == old.presentationsPlan) }
        activities[salesCode] = activity
    }
    override suspend fun getZoneSummaries(zone: String) = runCatching { summaries().filter { it.zone == zone } }
    override suspend fun getAllIslandSummaries() = runCatching { summaries() }
    override suspend fun getMonthlyAttendance(salesCode: String, monthKey: String) = runCatching {
        attendance[salesCode]?.takeIf { it.checkedIn }?.let { listOf(AttendanceDayDetail("Today", it.checkInTime, it.checkOutTime)) } ?: emptyList()
    }

    private fun summaries(): List<StaffDaySummary> = staff.values.filter { it.approvalStatus == ApprovalStatus.APPROVED }.map { person ->
        val a = activities[person.salesCode] ?: DailyActivity(); val att = attendance[person.salesCode] ?: AttendanceRecord()
        StaffDaySummary(person.salesCode, person.fullName, person.zone, att.checkedIn, a.planLocked, a.dayLocked,
            a.prospectingPlan + a.followUpsPlan + a.appointmentsPlan + a.presentationsPlan,
            a.prospectingDone + a.followUpsDone + a.appointmentsDone + a.presentationsDone,
            a.prospectingPlan, a.prospectingDone, a.followUpsPlan, a.followUpsDone, a.appointmentsPlan, a.appointmentsDone,
            a.presentationsPlan, a.presentationsDone, att.checkInTime, att.checkOutTime, if (att.checkedIn) 1 else 0)
    }
}
