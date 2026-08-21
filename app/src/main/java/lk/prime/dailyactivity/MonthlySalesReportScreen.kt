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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val SalesReportGreen = Color(0xFF123D2A)
private val SalesReportGold = Color(0xFFD6A62E)
private val SalesReportRed = Color(0xFFD32F2F)
private val SalesReportBlue = Color(0xFF1565C0)

private data class ZoneMonthlySalesSummary(
    val zone: String,
    val members: List<StaffMonthlySalesSummary>
) {
    val staffCount: Int get() = members.size
    val targetsSet: Int get() = members.count { it.targetLocked }
    val totalTarget: Long get() = members.sumOf { it.targetAmount }
    val totalAchieved: Long get() = members.sumOf { it.achievedAmount }
    val balance: Long get() = (totalTarget - totalAchieved).coerceAtLeast(0L)
    val achievementPercent: Double
        get() = if (totalTarget <= 0L) 0.0 else totalAchieved.toDouble() * 100.0 / totalTarget.toDouble()
}

@Composable
fun MonthlySalesReportScreen(
    repository: DataRepository,
    onBack: () -> Unit
) {
    var selectedMonth by remember { mutableStateOf(salesCurrentMonthKey()) }
    var rows by remember { mutableStateOf<List<StaffMonthlySalesSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedZone by remember { mutableStateOf<String?>(null) }
    var selectedStaff by remember { mutableStateOf<StaffMonthlySalesSummary?>(null) }
    val currentMonth = remember { salesCurrentMonthKey() }

    suspend fun load() {
        loading = true
        error = null
        repository.getMonthlySalesReport(selectedMonth)
            .onSuccess { rows = it }
            .onFailure { error = it.localizedMessage ?: "Could not load monthly sales report." }
        loading = false
    }

    LaunchedEffect(selectedMonth) {
        selectedZone = null
        selectedStaff = null
        load()
    }

    selectedStaff?.let { member ->
        SalesAchievementHistoryScreen(
            member = member,
            monthKey = selectedMonth,
            onBack = { selectedStaff = null }
        )
        return
    }

    selectedZone?.let { zoneName ->
        ZoneSalesPerformanceScreen(
            zoneName = zoneName,
            members = rows.filter { it.zone.ifBlank { "Unassigned" } == zoneName },
            monthKey = selectedMonth,
            onBack = { selectedZone = null },
            onStaff = { selectedStaff = it }
        )
        return
    }

    val totalTarget = rows.sumOf { it.targetAmount }
    val totalAchieved = rows.sumOf { it.achievedAmount }
    val totalBalance = (totalTarget - totalAchieved).coerceAtLeast(0L)
    val overallPercent = if (totalTarget <= 0L) 0.0 else totalAchieved.toDouble() * 100.0 / totalTarget.toDouble()
    val targetsSet = rows.count { it.targetLocked }
    val zones = rows
        .groupBy { it.zone.ifBlank { "Unassigned" } }
        .map { (zone, members) -> ZoneMonthlySalesSummary(zone, members) }
        .sortedWith(
            compareByDescending<ZoneMonthlySalesSummary> { it.achievementPercent }
                .thenByDescending { it.totalAchieved }
                .thenBy { it.zone }
        )

    Column(Modifier.fillMaxSize()) {
        Surface(color = SalesReportGreen, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "‹  MONTHLY SALES REPORT",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
                Text(
                    salesMonthDisplay(selectedMonth),
                    color = SalesReportGold,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black
                )
                Text("Target vs Achievement • All Island", color = Color.White)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = { selectedMonth = salesShiftMonth(selectedMonth, -1) }) { Text("‹") }
            Text(salesMonthDisplay(selectedMonth), color = SalesReportGreen, fontWeight = FontWeight.Black)
            OutlinedButton(
                onClick = { selectedMonth = salesShiftMonth(selectedMonth, 1) },
                enabled = selectedMonth < currentMonth
            ) { Text("›") }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SalesReportGreen)
            }
            error != null -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.TopCenter) {
                Text(error ?: "Could not load report.", color = SalesReportRed)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("COMPANY SUMMARY", color = SalesReportGreen, fontWeight = FontWeight.Black)
                            SalesMoneyRow("TOTAL TARGET", totalTarget, SalesReportGreen)
                            SalesMoneyRow("ACHIEVED", totalAchieved, SalesReportBlue)
                            SalesMoneyRow("BALANCE", totalBalance, SalesReportRed)
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("OVERALL ACHIEVEMENT", fontWeight = FontWeight.Bold)
                                Text("${salesPercent(overallPercent)}%", color = SalesReportGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                            LinearProgressIndicator(
                                progress = { (overallPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                color = SalesReportGold,
                                trackColor = Color.LightGray.copy(alpha = 0.45f)
                            )
                            Text("Targets set: $targetsSet / ${rows.size}", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Text("ZONE PERFORMANCE", color = SalesReportGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }

                items(zones) { zone ->
                    ZoneSalesCard(zone = zone) { selectedZone = zone.zone }
                }

                if (zones.isEmpty()) {
                    item { Text("No approved staff records available.", color = Color.Gray) }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun ZoneSalesCard(
    zone: ZoneMonthlySalesSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(zone.zone.uppercase(Locale.US), fontWeight = FontWeight.Black, color = SalesReportGreen)
                    Text(
                        "Staff ${zone.staffCount} • Targets ${zone.targetsSet}/${zone.staffCount}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Text(
                    "${salesPercent(zone.achievementPercent)}%",
                    color = SalesReportGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
            HorizontalDivider()
            SalesCompactRow("Target", "Rs. ${salesMoney(zone.totalTarget)}")
            SalesCompactRow("Achieved", "Rs. ${salesMoney(zone.totalAchieved)}")
            SalesCompactRow("Balance", "Rs. ${salesMoney(zone.balance)}")
            LinearProgressIndicator(
                progress = { (zone.achievementPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = SalesReportGreen,
                trackColor = Color.LightGray.copy(alpha = 0.45f)
            )
            Text("Tap to view staff in this zone ›", color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ZoneSalesPerformanceScreen(
    zoneName: String,
    members: List<StaffMonthlySalesSummary>,
    monthKey: String,
    onBack: () -> Unit,
    onStaff: (StaffMonthlySalesSummary) -> Unit
) {
    val zone = remember(zoneName, members) { ZoneMonthlySalesSummary(zoneName, members) }

    Column(Modifier.fillMaxSize()) {
        Surface(color = SalesReportGreen, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "‹  ZONE PERFORMANCE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
                Text(zoneName.uppercase(Locale.US), color = SalesReportGold, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text(salesMonthDisplay(monthKey), color = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("ZONE SUMMARY", color = SalesReportGreen, fontWeight = FontWeight.Black)
                        SalesMoneyRow("TARGET", zone.totalTarget, SalesReportGreen)
                        SalesMoneyRow("ACHIEVED", zone.totalAchieved, SalesReportBlue)
                        SalesMoneyRow("BALANCE", zone.balance, SalesReportRed)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ACHIEVEMENT", fontWeight = FontWeight.Bold)
                            Text(
                                "${salesPercent(zone.achievementPercent)}%",
                                color = SalesReportGold,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            "Staff ${zone.staffCount} • Targets set ${zone.targetsSet}/${zone.staffCount}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Text("STAFF", color = SalesReportGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }

            items(
                members.sortedWith(
                    compareByDescending<StaffMonthlySalesSummary> { it.achievementPercent }
                        .thenByDescending { it.achievedAmount }
                        .thenBy { it.fullName }
                )
            ) { row ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onStaff(row) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(row.fullName, fontWeight = FontWeight.Bold)
                                Text(row.salesCode, color = Color.Gray, fontSize = 12.sp)
                            }
                            Text(
                                if (row.targetLocked) "${salesPercent(row.achievementPercent)}%" else "NO TARGET",
                                color = if (row.targetLocked) SalesReportGreen else SalesReportRed,
                                fontWeight = FontWeight.Black
                            )
                        }
                        HorizontalDivider()
                        SalesCompactRow("Target", if (row.targetLocked) "Rs. ${salesMoney(row.targetAmount)}" else "Not set")
                        SalesCompactRow("Achieved", "Rs. ${salesMoney(row.achievedAmount)}")
                        SalesCompactRow("Balance", if (row.targetLocked) "Rs. ${salesMoney(row.balance)}" else "—")
                        Text("Tap for individual monthly details ›", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }

            if (members.isEmpty()) {
                item { Text("No staff records in this zone.", color = Color.Gray) }
            }
        }
    }
}

@Composable
private fun SalesAchievementHistoryScreen(
    member: StaffMonthlySalesSummary,
    monthKey: String,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Surface(color = SalesReportGreen, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "‹  STAFF MONTHLY DETAILS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
                Text(member.fullName, color = SalesReportGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("${member.salesCode} • ${member.zone}", color = Color.White)
                Text(salesMonthDisplay(monthKey), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("MONTH SUMMARY", color = SalesReportGreen, fontWeight = FontWeight.Black)
                        SalesMoneyRow("TARGET", member.targetAmount, SalesReportGreen, member.targetLocked)
                        SalesMoneyRow("ACHIEVED", member.achievedAmount, SalesReportBlue)
                        SalesMoneyRow("BALANCE", member.balance, SalesReportRed, member.targetLocked)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ACHIEVEMENT", fontWeight = FontWeight.Bold)
                            Text(
                                if (member.targetLocked) "${salesPercent(member.achievementPercent)}%" else "Target not set",
                                color = if (member.targetLocked) SalesReportGold else SalesReportRed,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            item {
                Text("DAILY ACHIEVEMENTS", color = SalesReportGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }

            items(member.achievements) { day ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(salesDateDisplay(day.date), fontWeight = FontWeight.Bold)
                            Text(day.date, color = Color.Gray, fontSize = 11.sp)
                        }
                        Text("Rs. ${salesMoney(day.amount)}", color = SalesReportGreen, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            if (member.achievements.isEmpty()) {
                item { Text("No achievements added for this month.", color = Color.Gray) }
            }
        }
    }
}

@Composable
private fun SalesMoneyRow(label: String, value: Long, color: Color, available: Boolean = true) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        Text(if (available) "Rs. ${salesMoney(value)}" else "Not set", color = color, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SalesCompactRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun salesMoney(value: Long): String = NumberFormat.getNumberInstance(Locale.US).format(value)
private fun salesPercent(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun salesCurrentMonthKey(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

private fun salesShiftMonth(monthKey: String, delta: Int): String = runCatching {
    val format = SimpleDateFormat("yyyy-MM", Locale.US)
    val calendar = Calendar.getInstance().apply { time = format.parse(monthKey) ?: Date() }
    calendar.add(Calendar.MONTH, delta)
    format.format(calendar.time)
}.getOrDefault(monthKey)

private fun salesMonthDisplay(monthKey: String): String = runCatching {
    val input = SimpleDateFormat("yyyy-MM", Locale.US)
    val output = SimpleDateFormat("MMMM yyyy", Locale.US)
    output.format(input.parse(monthKey) ?: Date()).uppercase(Locale.US)
}.getOrDefault(monthKey)

private fun salesDateDisplay(date: String): String = runCatching {
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val output = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
    output.format(input.parse(date) ?: Date())
}.getOrDefault(date)
