package lk.prime.dailyactivity

enum class UserRole { STAFF, ZONAL_MANAGER, ADMIN }
enum class ApprovalStatus { PENDING, APPROVED, REJECTED }

data class StaffProfile(
    val salesCode: String,
    val fullName: String,
    val mobile: String,
    val zone: String,
    val role: UserRole = UserRole.STAFF,
    val photoUri: String? = null,
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING
)

data class AttendanceRecord(
    val checkedIn: Boolean = false,
    val checkedOut: Boolean = false,
    val checkInTime: String? = null,
    val checkOutTime: String? = null
)

data class DailyActivity(
    val prospectingPlan: Int = 0,
    val followUpsPlan: Int = 0,
    val appointmentsPlan: Int = 0,
    val presentationsPlan: Int = 0,
    val prospectingDone: Int = 0,
    val followUpsDone: Int = 0,
    val appointmentsDone: Int = 0,
    val presentationsDone: Int = 0,
    val planLocked: Boolean = false,
    val dayLocked: Boolean = false
)
