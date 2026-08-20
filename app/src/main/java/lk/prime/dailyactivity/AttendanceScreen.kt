package lk.prime.dailyactivity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceScreen(onContinue: () -> Unit) {
    var record by remember { mutableStateOf(AttendanceRecord()) }
    val now: () -> String = { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Attendance", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimeColors.Green)
        Text("Mark attendance before creating today's plan.")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Today's Attendance", fontWeight = FontWeight.Bold)
                Text(if (record.checkedIn) "Checked in: ${record.checkInTime}" else "Not checked in")
            }
        }
        Button(
            onClick = { record = record.copy(checkedIn = true, checkInTime = now()) },
            enabled = !record.checkedIn,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (record.checkedIn) "ATTENDANCE MARKED ✓" else "MARK ATTENDANCE") }
        Button(onClick = onContinue, enabled = record.checkedIn, modifier = Modifier.fillMaxWidth()) {
            Text("CONTINUE TO TODAY'S PLAN")
        }
    }
}
