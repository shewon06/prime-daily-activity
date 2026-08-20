package lk.prime.dailyactivity

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
import java.util.Locale

private val DashboardGreen = Color(0xFF123D2A)
private val DashboardGold = Color(0xFFD6A62E)
private val DashboardRed = Color(0xFFD32F2F)
private val DashboardBlue = Color(0xFF1565C0)
private val DashboardPurple = Color(0xFF6A3DB8)
private val DashboardOrange = Color(0xFFEF6C00)

data class StaffDaySummary(
    val salesCode: String, val name: String, val zone: String, val present: Boolean,
    val dayStarted: Boolean, val dayEnded: Boolean, val totalPlan: Int, val totalDone: Int,
    val prospectingPlan: Int = 0, val prospectingDone: Int = 0,
    val followUpsPlan: Int = 0, val followUpsDone: Int = 0,
    val appointmentsPlan: Int = 0, val appointmentsDone: Int = 0,
    val presentationsPlan: Int = 0, val presentationsDone: Int = 0,
    val checkInTime: String? = null, val checkOutTime: String? = null,
    val workedDays: Int = 0
) { val achievement: Int get() = if (totalPlan == 0) 0 else totalDone * 100 / totalPlan }

private data class ZoneDaySummary(val zone: String, val staff: Int, val present: Int, val plan: Int, val done: Int) {
    val achievement: Int get() = if (plan == 0) 0 else done * 100 / plan
}

@Composable
fun RegistrationApprovalScreen(pending: List<StaffProfile>, onApprove: (StaffProfile) -> Unit, onReject: (StaffProfile) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Registration Approvals", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DashboardGreen); Text("${pending.size} pending"); Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(pending) { staff -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(staff.fullName, fontWeight = FontWeight.Bold); Text("Sales Code: ${staff.salesCode} • Zone: ${staff.zone}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { onApprove(staff) }) { Text("APPROVE") }; OutlinedButton(onClick = { onReject(staff) }) { Text("REJECT") } } } } } }
    }
}

@Composable
fun ManagementDashboardScreen(profile: StaffProfile, repository: DataRepository) {
    var staff by remember { mutableStateOf<List<StaffDaySummary>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf<String?>(null) }
    suspend fun loadDashboard() { loading = true; error = null; val result = if (profile.role == UserRole.ZONAL_MANAGER) repository.getZoneSummaries(profile.zone) else repository.getAllIslandSummaries(); result.onSuccess { staff = it }.onFailure { error = it.localizedMessage ?: "Could not load dashboard." }; loading = false }
    LaunchedEffect(profile.salesCode) { loadDashboard() }
    Column(Modifier.fillMaxSize()) {
        Surface(color = DashboardGreen, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("PRIME", color = DashboardGold, fontSize = 27.sp, fontWeight = FontWeight.Black); Text("Management Dashboard", color = Color.White, fontSize = 14.sp) } }
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DashboardGreen) }
        else Dashboard(
            title = if (profile.role == UserRole.ZONAL_MANAGER) profile.zone else "All Island",
            subtitle = if (profile.role == UserRole.ZONAL_MANAGER) "Zonal Manager Dashboard" else "Company Overview & Performance",
            staff = staff,
            error = error,
            showZonePerformance = profile.role != UserRole.ZONAL_MANAGER,
            showAttendanceOverview = profile.role == UserRole.ADMIN
        )
    }
}

@Composable fun ZonalManagerDashboard(zone: String, staff: List<StaffDaySummary>) { Dashboard(zone, "Zonal Manager Dashboard", staff, showZonePerformance = false, showAttendanceOverview = false) }
@Composable fun AllIslandDashboard(staff: List<StaffDaySummary>) { Dashboard("All Island", "Company Overview & Performance", staff, showZonePerformance = true, showAttendanceOverview = true) }

@Composable
private fun Dashboard(title: String, subtitle: String, staff: List<StaffDaySummary>, error: String? = null, showZonePerformance: Boolean = true, showAttendanceOverview: Boolean = false) {
    val present = staff.count { it.present }; val absent = staff.size - present; val started = staff.count { it.dayStarted }; val ended = staff.count { it.dayEnded }
    val plan = staff.sumOf { it.totalPlan }; val done = staff.sumOf { it.totalDone }; val achievement = if (plan == 0) 0 else done * 100 / plan
    val prospectingPlan = staff.sumOf { it.prospectingPlan }; val prospectingDone = staff.sumOf { it.prospectingDone }; val followUpsPlan = staff.sumOf { it.followUpsPlan }; val followUpsDone = staff.sumOf { it.followUpsDone }; val appointmentsPlan = staff.sumOf { it.appointmentsPlan }; val appointmentsDone = staff.sumOf { it.appointmentsDone }; val presentationsPlan = staff.sumOf { it.presentationsPlan }; val presentationsDone = staff.sumOf { it.presentationsDone }
    val zones = staff.groupBy { it.zone.ifBlank { "Unassigned" } }.map { (zone, members) -> ZoneDaySummary(zone, members.size, members.count { it.present }, members.sumOf { it.totalPlan }, members.sumOf { it.totalDone }) }.sortedByDescending { it.achievement }
    val topPerformers = staff.filter { it.totalPlan > 0 }.sortedWith(compareByDescending<StaffDaySummary> { it.achievement }.thenByDescending { it.totalDone }).take(3)

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(title.uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DashboardGreen); Text(subtitle, color = Color.Gray); error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { SummaryCard("TOTAL STAFF", staff.size, DashboardGreen, Modifier.weight(1f)); SummaryCard("PRESENT", present, DashboardGreen, Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { SummaryCard("ABSENT", absent, DashboardRed, Modifier.weight(1f)); SummaryCard("DAY STARTED", started, DashboardBlue, Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { SummaryCard("DAY ENDED", ended, DashboardPurple, Modifier.weight(1f)); SummaryCard("ACHIEVEMENT", achievement, DashboardGold, Modifier.weight(1f), "%") } }

        if (showAttendanceOverview) {
            item { Text("ATTENDANCE OVERVIEW", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DashboardGreen) }
            items(staff.sortedWith(compareByDescending<StaffDaySummary> { it.present }.thenBy { it.name })) { AttendanceOverviewCard(it) }
        }

        item { Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("ACTIVITY SUMMARY • PLAN vs DONE", fontWeight = FontWeight.Bold, color = DashboardGreen); ActivitySummaryRow("Prospecting", prospectingPlan, prospectingDone, DashboardGreen); HorizontalDivider(); ActivitySummaryRow("Follow Ups", followUpsPlan, followUpsDone, DashboardOrange); HorizontalDivider(); ActivitySummaryRow("Appointments", appointmentsPlan, appointmentsDone, DashboardRed); HorizontalDivider(); ActivitySummaryRow("Presentations", presentationsPlan, presentationsDone, DashboardPurple); HorizontalDivider(); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("TOTAL", fontWeight = FontWeight.Bold); Text("Plan $plan  •  Done $done", fontSize = 13.sp, color = Color.Gray) }; Text("$achievement%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = DashboardGreen) }; LinearProgressIndicator(progress = { achievement.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth(), color = DashboardGreen) } } }
        if (showZonePerformance) { item { Text("ZONE PERFORMANCE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DashboardGreen) }; items(zones) { ZonePerformanceCard(it) } }
        item { Text("TOP PERFORMERS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DashboardGreen) }
        if (topPerformers.isEmpty()) item { Text("No activity plans submitted yet.", color = Color.Gray) }
        else items(topPerformers.size) { index -> TopPerformerCard(index + 1, topPerformers[index]) }
        item { Text("STAFF STATUS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DashboardGreen) }
        items(staff.sortedByDescending { it.achievement }) { member -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(member.name, fontWeight = FontWeight.Bold); Text("${member.achievement}%", fontWeight = FontWeight.Bold, color = DashboardGreen) }; Text("${member.salesCode} • ${member.zone}", color = Color.Gray, fontSize = 12.sp); Spacer(Modifier.height(5.dp)); Text("Plan ${member.totalPlan}  |  Done ${member.totalDone}"); Text(when { member.dayEnded -> "Day Ended • Locked"; member.dayStarted -> "Day In Progress"; member.present -> "Present • Not Started"; else -> "Absent" }, color = when { member.dayEnded -> DashboardPurple; member.dayStarted -> DashboardBlue; member.present -> DashboardGreen; else -> DashboardRed }, fontWeight = FontWeight.SemiBold) } } }
    }
}

@Composable
private fun AttendanceOverviewCard(member: StaffDaySummary) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(member.name, fontWeight = FontWeight.Bold)
                    Text("${member.salesCode} • ${member.zone}", fontSize = 12.sp, color = Color.Gray)
                }
                Text(if (member.present) "PRESENT" else "ABSENT", color = if (member.present) DashboardGreen else DashboardRed, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AttendanceValue("IN", member.checkInTime ?: "—")
                AttendanceValue("OUT", member.checkOutTime ?: "—")
                AttendanceValue("TODAY", workingTimeLabel(member.checkInTime, member.checkOutTime))
            }
            Text("Worked Days: ${member.workedDays}", fontWeight = FontWeight.SemiBold, color = DashboardGreen)
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

private fun workingTimeLabel(checkIn: String?, checkOut: String?): String {
    if (checkIn.isNullOrBlank()) return "—"
    if (checkOut.isNullOrBlank()) return "Working"
    return runCatching {
        val format = SimpleDateFormat("hh:mm a", Locale.US)
        val start = format.parse(checkIn.uppercase(Locale.US)) ?: return@runCatching "—"
        val end = format.parse(checkOut.uppercase(Locale.US)) ?: return@runCatching "—"
        var minutes = (end.time - start.time) / 60000L
        if (minutes < 0) minutes += 24 * 60
        val hours = minutes / 60
        val mins = minutes % 60
        if (mins == 0L) "${hours}h" else "${hours}h ${mins}m"
    }.getOrDefault("—")
}

@Composable private fun TopPerformerCard(rank: Int, member: StaffDaySummary) {
    val rankLabel = when (rank) { 1 -> "#1"; 2 -> "#2"; else -> "#3" }; val accent = when (rank) { 1 -> DashboardGold; 2 -> Color.Gray; else -> DashboardOrange }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(rankLabel, fontSize = 24.sp, fontWeight = FontWeight.Black, color = accent, modifier = Modifier.width(48.dp)); Column(Modifier.weight(1f)) { Text(member.name, fontWeight = FontWeight.Bold); Text("${member.salesCode} • ${member.zone}", fontSize = 12.sp, color = Color.Gray); Text("Plan ${member.totalPlan} • Done ${member.totalDone}", fontSize = 13.sp) }; Text("${member.achievement}%", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DashboardGreen) } }
}

@Composable private fun ZonePerformanceCard(zone: ZoneDaySummary) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(zone.zone.uppercase(), fontWeight = FontWeight.Bold, color = DashboardGreen); Text("${zone.achievement}%", fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (zone.achievement >= 80) DashboardGreen else if (zone.achievement >= 50) DashboardOrange else DashboardRed) }; Text("Staff ${zone.staff}  •  Present ${zone.present}  •  Absent ${zone.staff - zone.present}", fontSize = 13.sp, color = Color.Gray); Text("Plan ${zone.plan}  •  Done ${zone.done}", fontWeight = FontWeight.SemiBold); LinearProgressIndicator(progress = { zone.achievement.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth(), color = DashboardGreen) } } }
@Composable private fun ActivitySummaryRow(label: String, plan: Int, done: Int, accent: Color) { val achievement = if (plan == 0) 0 else done * 100 / plan; Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.SemiBold); Text("Plan $plan  •  Done $done", fontSize = 13.sp, color = Color.Gray) }; Text("$achievement%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent) } }
@Composable private fun SummaryCard(label: String, value: Int, accent: Color, modifier: Modifier = Modifier, suffix: String = "") { Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(15.dp)) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray); Spacer(Modifier.height(4.dp)); Text("$value$suffix", fontSize = 28.sp, fontWeight = FontWeight.Black, color = accent) } } }
