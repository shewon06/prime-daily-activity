package lk.prime.dailyactivity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DashboardGreen = Color(0xFF123D2A)
private val DashboardGold = Color(0xFFD6A62E)
private val DashboardRed = Color(0xFFD32F2F)
private val DashboardBlue = Color(0xFF1565C0)
private val DashboardPurple = Color(0xFF6A3DB8)
private val DashboardOrange = Color(0xFFEF6C00)

data class StaffDaySummary(
    val salesCode: String,
    val name: String,
    val zone: String,
    val present: Boolean,
    val dayStarted: Boolean,
    val dayEnded: Boolean,
    val totalPlan: Int,
    val totalDone: Int,
    val prospectingPlan: Int = 0,
    val prospectingDone: Int = 0,
    val followUpsPlan: Int = 0,
    val followUpsDone: Int = 0,
    val appointmentsPlan: Int = 0,
    val appointmentsDone: Int = 0,
    val presentationsPlan: Int = 0,
    val presentationsDone: Int = 0,
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val workedDays: Int = 0
) {
    val achievement: Int get() = if (totalPlan == 0) 0 else totalDone * 100 / totalPlan
}

private data class ZoneDaySummary(
    val zone: String,
    val staff: Int,
    val present: Int,
    val plan: Int,
    val done: Int
) {
    val achievement: Int get() = if (plan == 0) 0 else done * 100 / plan
}

@Composable
fun RegistrationApprovalScreen(
    pending: List<StaffProfile>,
    onApprove: (StaffProfile) -> Unit,
    onReject: (StaffProfile) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Registration Approvals", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DashboardGreen)
        Text("${pending.size} pending")
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pending) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(s.fullName, fontWeight = FontWeight.Bold)
                        Text("Sales Code: ${s.salesCode} • Zone: ${s.zone}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button({ onApprove(s) }) { Text("APPROVE") }
                            OutlinedButton({ onReject(s) }) { Text("REJECT") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagementDashboardScreen(profile: StaffProfile, repository: DataRepository) {
    var staff by remember { mutableStateOf<List<StaffDaySummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attendanceOpen by remember { mutableStateOf(false) }
    var historyMember by remember { mutableStateOf<StaffDaySummary?>(null) }
    var historyMonth by remember { mutableStateOf(currentMonthKey()) }

    suspend fun load() {
        loading = true
        val r = if (profile.role == UserRole.ZONAL_MANAGER) {
            repository.getZoneSummaries(profile.zone)
        } else {
            repository.getAllIslandSummaries()
        }
        r.onSuccess { staff = it }.onFailure { error = it.localizedMessage }
        loading = false
    }

    LaunchedEffect(profile.salesCode) { load() }

    if (historyMember != null && profile.role == UserRole.ADMIN) {
        MonthlyAttendanceScreen(historyMember!!, repository, historyMonth) {
            historyMember = null
        }
        return
    }

    if (attendanceOpen && profile.role == UserRole.ADMIN) {
        AttendanceReportScreen(
            staff = staff,
            repository = repository,
            onBack = { attendanceOpen = false },
            onMember = { member, monthKey ->
                historyMonth = monthKey
                historyMember = member
            }
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = DashboardGreen, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("PRIME", color = DashboardGold, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("Management Dashboard", color = Color.White)
            }
        }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Dashboard(
                title = if (profile.role == UserRole.ZONAL_MANAGER) profile.zone else "All Island",
                subtitle = if (profile.role == UserRole.ZONAL_MANAGER) "Zonal Manager Dashboard" else "Company Overview & Performance",
                staff = staff,
                error = error,
                showZonePerformance = profile.role != UserRole.ZONAL_MANAGER,
                showAttendanceButton = profile.role == UserRole.ADMIN,
                onAttendance = { attendanceOpen = true }
            )
        }
    }
}

@Composable
private fun Dashboard(
    title: String,
    subtitle: String,
    staff: List<StaffDaySummary>,
    error: String? = null,
    showZonePerformance: Boolean = true,
    showAttendanceButton: Boolean = false,
    onAttendance: () -> Unit = {}
) {
    val present = staff.count { it.present }
    val plan = staff.sumOf { it.totalPlan }
    val done = staff.sumOf { it.totalDone }
    val ach = if (plan == 0) 0 else done * 100 / plan
    val zones = staff.groupBy { it.zone.ifBlank { "Unassigned" } }.map { (z, m) ->
        ZoneDaySummary(z, m.size, m.count { it.present }, m.sumOf { it.totalPlan }, m.sumOf { it.totalDone })
    }.sortedByDescending { it.achievement }
    val top = staff.filter { it.totalPlan > 0 }
        .sortedWith(compareByDescending<StaffDaySummary> { it.achievement }.thenByDescending { it.totalDone })
        .take(3)

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(title.uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DashboardGreen)
            Text(subtitle, color = Color.Gray)
            error?.let { Text(it, color = DashboardRed) }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("TOTAL STAFF", staff.size, DashboardGreen, Modifier.weight(1f))
                SummaryCard("PRESENT", present, DashboardGreen, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("ABSENT", staff.size - present, DashboardRed, Modifier.weight(1f))
                SummaryCard("DAY STARTED", staff.count { it.dayStarted }, DashboardBlue, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("DAY ENDED", staff.count { it.dayEnded }, DashboardPurple, Modifier.weight(1f))
                SummaryCard("ACHIEVEMENT", ach, DashboardGold, Modifier.weight(1f), "%")
            }
        }
        if (showAttendanceButton) {
            item {
                Card(
                    Modifier.fillMaxWidth().clickable { onAttendance() },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ATTENDANCE REPORT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DashboardGreen)
                            Text("Today & monthly staff attendance", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("VIEW  ›", fontWeight = FontWeight.Bold, color = DashboardGold)
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ACTIVITY SUMMARY • PLAN vs DONE", fontWeight = FontWeight.Bold, color = DashboardGreen)
                    ActivitySummaryRow("Prospecting", staff.sumOf { it.prospectingPlan }, staff.sumOf { it.prospectingDone }, DashboardGreen)
                    ActivitySummaryRow("Follow Ups", staff.sumOf { it.followUpsPlan }, staff.sumOf { it.followUpsDone }, DashboardOrange)
                    ActivitySummaryRow("Appointments", staff.sumOf { it.appointmentsPlan }, staff.sumOf { it.appointmentsDone }, DashboardRed)
                    ActivitySummaryRow("Presentations", staff.sumOf { it.presentationsPlan }, staff.sumOf { it.presentationsDone }, DashboardPurple)
                    HorizontalDivider()
                    Text("TOTAL   Plan $plan • Done $done     $ach%", fontWeight = FontWeight.Bold, color = DashboardGreen)
                }
            }
        }
        if (showZonePerformance) {
            item { Text("ZONE PERFORMANCE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DashboardGreen) }
            items(zones) { z ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(z.zone.uppercase(), fontWeight = FontWeight.Bold, color = DashboardGreen)
                        Text("Staff ${z.staff} • Present ${z.present} • Absent ${z.staff - z.present}")
                        Text("Plan ${z.plan} • Done ${z.done} • ${z.achievement}%")
                    }
                }
            }
        }
        item { Text("TOP PERFORMERS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DashboardGreen) }
        items(top.size) { i ->
            val m = top[i]
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("#${i + 1}  ${m.name}", fontWeight = FontWeight.Bold)
                    Text("${m.achievement}%", color = DashboardGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Text("STAFF STATUS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DashboardGreen) }
        items(staff) { m ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(m.name, fontWeight = FontWeight.Bold)
                    Text("${m.salesCode} • ${m.zone}", color = Color.Gray)
                    Text("Plan ${m.totalPlan} | Done ${m.totalDone}")
                }
            }
        }
    }
}

@Composable
private fun AttendanceReportScreen(
    staff: List<StaffDaySummary>,
    repository: DataRepository,
    onBack: () -> Unit,
    onMember: (StaffDaySummary, String) -> Unit
) {
    var monthlyMode by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(currentMonthKey()) }
    var monthlyRows by remember { mutableStateOf<List<StaffMonthlyAttendanceSummary>>(emptyList()) }
    var monthlyLoading by remember { mutableStateOf(false) }
    var monthlyError by remember { mutableStateOf<String?>(null) }
    val currentMonth = remember { currentMonthKey() }

    LaunchedEffect(monthlyMode, selectedMonth) {
        if (monthlyMode) {
            monthlyLoading = true
            monthlyError = null
            repository.getMonthlyAttendanceReport(selectedMonth)
                .onSuccess { monthlyRows = it }
                .onFailure { monthlyError = it.localizedMessage ?: "Could not load monthly attendance." }
            monthlyLoading = false
        }
    }

    val present = staff.count { it.present }

    Column(Modifier.fillMaxSize()) {
        Surface(color = DashboardGreen, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "‹  ATTENDANCE REPORT",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
                Text(
                    if (monthlyMode) monthDisplay(selectedMonth) else SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()),
                    color = DashboardGold,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (monthlyMode) "Monthly staff attendance summary" else "Present $present  •  Absent ${staff.size - present}",
                    color = Color.White
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { monthlyMode = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!monthlyMode) DashboardGreen else Color(0xFFE8ECE9),
                    contentColor = if (!monthlyMode) Color.White else DashboardGreen
                )
            ) { Text("TODAY", fontWeight = FontWeight.Bold) }
            Button(
                onClick = { monthlyMode = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (monthlyMode) DashboardGreen else Color(0xFFE8ECE9),
                    contentColor = if (monthlyMode) Color.White else DashboardGreen
                )
            ) { Text("MONTHLY", fontWeight = FontWeight.Bold) }
        }

        if (!monthlyMode) {
            TodayAttendanceReport(staff, onMember)
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = { selectedMonth = shiftMonth(selectedMonth, -1) }) { Text("‹") }
                    Text(monthDisplay(selectedMonth), fontWeight = FontWeight.Black, color = DashboardGreen)
                    OutlinedButton(
                        onClick = { selectedMonth = shiftMonth(selectedMonth, 1) },
                        enabled = selectedMonth < currentMonth
                    ) { Text("›") }
                }

                when {
                    monthlyLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DashboardGreen)
                    }
                    monthlyError != null -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.TopCenter) {
                        Text(monthlyError ?: "Could not load report.", color = DashboardRed)
                    }
                    else -> MonthlyAttendanceReport(
                        rows = monthlyRows,
                        staffToday = staff,
                        monthKey = selectedMonth,
                        onMember = onMember
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayAttendanceReport(
    staff: List<StaffDaySummary>,
    onMember: (StaffDaySummary, String) -> Unit
) {
    val present = staff.count { it.present }
    val month = currentMonthKey()
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("PRESENT", present, DashboardGreen, Modifier.weight(1f))
                SummaryCard("ABSENT", staff.size - present, DashboardRed, Modifier.weight(1f))
            }
        }
        items(staff.sortedWith(compareByDescending<StaffDaySummary> { it.present }.thenBy { it.name })) { m ->
            AttendanceOverviewCard(m) { onMember(m, month) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MonthlyAttendanceReport(
    rows: List<StaffMonthlyAttendanceSummary>,
    staffToday: List<StaffDaySummary>,
    monthKey: String,
    onMember: (StaffDaySummary, String) -> Unit
) {
    val totalDays = rows.sumOf { it.attendance.size }
    val activeStaff = rows.count { it.attendance.isNotEmpty() }
    val totalMinutes = rows.sumOf { row -> row.attendance.sumOf { workingMinutes(it.checkInTime, it.checkOutTime) } }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("STAFF", rows.size, DashboardGreen, Modifier.weight(1f))
                SummaryCard("ACTIVE", activeStaff, DashboardBlue, Modifier.weight(1f))
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("MONTH TOTAL", fontWeight = FontWeight.Bold, color = DashboardGreen)
                    Text("Attendance Days: $totalDays", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Recorded Hours: ${minutesLabel(totalMinutes)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        items(rows) { row ->
            val total = row.attendance.sumOf { workingMinutes(it.checkInTime, it.checkOutTime) }
            val member = staffToday.firstOrNull { it.salesCode == row.salesCode }
                ?: StaffDaySummary(row.salesCode, row.fullName, row.zone, false, false, false, 0, 0)
            Card(
                Modifier.fillMaxWidth().clickable { onMember(member, monthKey) },
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(row.fullName, fontWeight = FontWeight.Bold)
                            Text("${row.salesCode} • ${row.zone}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("${row.attendance.size} DAYS", color = DashboardGreen, fontWeight = FontWeight.Black)
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Hours", color = Color.Gray)
                        Text(minutesLabel(total), fontWeight = FontWeight.Bold, color = DashboardGreen)
                    }
                    if (row.attendance.isNotEmpty()) {
                        val avg = total / row.attendance.size
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Average / Day", color = Color.Gray)
                            Text(minutesLabel(avg), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text("Tap for daily attendance history ›", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
        if (rows.isEmpty()) {
            item { Text("No staff records available.", color = Color.Gray) }
        }
    }
}

@Composable
private fun AttendanceOverviewCard(m: StaffDaySummary, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(m.name, fontWeight = FontWeight.Bold)
                    Text("${m.salesCode} • ${m.zone}", fontSize = 12.sp, color = Color.Gray)
                }
                Text(
                    if (m.present) "PRESENT" else "ABSENT",
                    color = if (m.present) DashboardGreen else DashboardRed,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth()) {
                AttendanceValue("IN", m.checkInTime ?: "—")
                AttendanceValue("OUT", m.checkOutTime ?: "—")
                AttendanceValue("TODAY", workingTimeLabel(m.checkInTime, m.checkOutTime))
            }
            Text("Total Worked Days: ${m.workedDays}", fontWeight = FontWeight.SemiBold, color = DashboardGreen)
            Text("Tap for monthly history ›", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun MonthlyAttendanceScreen(
    m: StaffDaySummary,
    repo: DataRepository,
    monthKey: String,
    onBack: () -> Unit
) {
    var rows by remember { mutableStateOf<List<AttendanceDayDetail>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(m.salesCode, monthKey) {
        loading = true
        error = null
        repo.getMonthlyAttendance(m.salesCode, monthKey)
            .onSuccess { rows = it }
            .onFailure { error = it.localizedMessage ?: "Could not load attendance history." }
        loading = false
    }

    val totalMinutes = rows.sumOf { workingMinutes(it.checkInTime, it.checkOutTime) }

    Column(Modifier.fillMaxSize()) {
        Surface(color = DashboardGreen, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "‹  ATTENDANCE HISTORY",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
                Text(m.name, color = DashboardGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${m.salesCode} • ${monthDisplay(monthKey)}", color = Color.White)
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.TopCenter) {
                Text(error ?: "Could not load attendance history.", color = DashboardRed)
            }
            else -> LazyColumn(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("MONTH SUMMARY", fontWeight = FontWeight.Bold, color = DashboardGreen)
                            Text("Worked Days: ${rows.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Total Hours: ${minutesLabel(totalMinutes)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            if (rows.isNotEmpty()) {
                                Text("Average / Day: ${minutesLabel(totalMinutes / rows.size)}", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                items(rows) { r ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(formatAttendanceDate(r.date), fontWeight = FontWeight.Bold)
                                Text("In ${r.checkInTime ?: "—"}  •  Out ${r.checkOutTime ?: "—"}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(
                                if (r.checkOutTime.isNullOrBlank()) "Working" else minutesLabel(workingMinutes(r.checkInTime, r.checkOutTime)),
                                fontWeight = FontWeight.Bold,
                                color = DashboardGreen
                            )
                        }
                    }
                }
                if (rows.isEmpty()) {
                    item { Text("No attendance records for this month.", color = Color.Gray) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.AttendanceValue(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(value.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun currentMonthKey(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

private fun shiftMonth(monthKey: String, delta: Int): String = runCatching {
    val format = SimpleDateFormat("yyyy-MM", Locale.US)
    val calendar = Calendar.getInstance().apply { time = format.parse(monthKey) ?: Date() }
    calendar.add(Calendar.MONTH, delta)
    format.format(calendar.time)
}.getOrDefault(monthKey)

private fun monthDisplay(monthKey: String): String = runCatching {
    val input = SimpleDateFormat("yyyy-MM", Locale.US)
    val output = SimpleDateFormat("MMMM yyyy", Locale.US)
    output.format(input.parse(monthKey) ?: Date()).uppercase(Locale.US)
}.getOrDefault(monthKey)

private fun formatAttendanceDate(date: String): String = runCatching {
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val output = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
    output.format(input.parse(date) ?: Date())
}.getOrDefault(date)

private fun workingMinutes(a: String?, b: String?): Long {
    if (a.isNullOrBlank() || b.isNullOrBlank()) return 0
    return runCatching {
        val f = SimpleDateFormat("hh:mm a", Locale.US)
        val s = f.parse(a.uppercase())!!
        val e = f.parse(b.uppercase())!!
        var x = (e.time - s.time) / 60000
        if (x < 0) x += 1440
        x
    }.getOrDefault(0)
}

private fun minutesLabel(x: Long): String = "${x / 60}h ${x % 60}m"

private fun workingTimeLabel(a: String?, b: String?): String = when {
    a.isNullOrBlank() -> "—"
    b.isNullOrBlank() -> "Working"
    else -> minutesLabel(workingMinutes(a, b))
}

@Composable
private fun ActivitySummaryRow(l: String, p: Int, d: Int, c: Color) {
    val a = if (p == 0) 0 else d * 100 / p
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(l, fontWeight = FontWeight.SemiBold)
            Text("Plan $p • Done $d", fontSize = 13.sp, color = Color.Gray)
        }
        Text("$a%", fontWeight = FontWeight.Bold, color = c)
    }
}

@Composable
private fun SummaryCard(
    l: String,
    v: Int,
    c: Color,
    modifier: Modifier = Modifier,
    suffix: String = ""
) {
    Card(modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(l, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text("$v$suffix", fontSize = 28.sp, fontWeight = FontWeight.Black, color = c)
        }
    }
}
