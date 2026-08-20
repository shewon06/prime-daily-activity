package lk.prime.dailyactivity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class StaffDaySummary(
    val salesCode: String,
    val name: String,
    val zone: String,
    val present: Boolean,
    val dayStarted: Boolean,
    val dayEnded: Boolean,
    val totalPlan: Int,
    val totalDone: Int
) {
    val achievement: Int get() = if (totalPlan == 0) 0 else totalDone * 100 / totalPlan
}

@Composable
fun RegistrationApprovalScreen(
    pending: List<StaffProfile>,
    onApprove: (StaffProfile) -> Unit,
    onReject: (StaffProfile) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Registration Approvals", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimeColors.Green)
        Text("${pending.size} pending")
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pending) { staff ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(staff.fullName, fontWeight = FontWeight.Bold)
                        Text("Sales Code: ${staff.salesCode} • Zone: ${staff.zone}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onApprove(staff) }) { Text("APPROVE") }
                            OutlinedButton(onClick = { onReject(staff) }) { Text("REJECT") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZonalManagerDashboard(zone: String, staff: List<StaffDaySummary>) {
    val visible = staff.filter { it.zone == zone }
    Dashboard(title = "$zone Zone", subtitle = "Zonal Manager Dashboard", staff = visible)
}

@Composable
fun AllIslandDashboard(staff: List<StaffDaySummary>) {
    Dashboard(title = "All Island", subtitle = "Management Dashboard", staff = staff)
}

@Composable
private fun Dashboard(title: String, subtitle: String, staff: List<StaffDaySummary>) {
    val present = staff.count { it.present }
    val started = staff.count { it.dayStarted }
    val ended = staff.count { it.dayEnded }
    val plan = staff.sumOf { it.totalPlan }
    val done = staff.sumOf { it.totalDone }
    val achievement = if (plan == 0) 0 else done * 100 / plan

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = PrimeColors.Green)
        Text(subtitle)
        Spacer(Modifier.height(14.dp))
        Text("Staff ${staff.size}   •   Present $present   •   Started $started   •   Ended $ended", fontWeight = FontWeight.Bold)
        Text("Plan $plan   •   Done $done   •   Achievement $achievement%", color = PrimeColors.Green)
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(staff) { member ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(member.name, fontWeight = FontWeight.Bold)
                        Text("${member.salesCode} • ${member.zone}")
                        Text("Plan ${member.totalPlan}  |  Done ${member.totalDone}  |  ${member.achievement}%")
                        Text(
                            when {
                                member.dayEnded -> "Day Ended 🔒"
                                member.dayStarted -> "Day In Progress"
                                member.present -> "Present • Not Started"
                                else -> "Not Present"
                            }
                        )
                    }
                }
            }
        }
    }
}
