package lk.prime.dailyactivity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceScreen(
    salesCode: String,
    repository: DataRepository,
    onContinue: () -> Unit
) {
    var record by remember { mutableStateOf(AttendanceRecord()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
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

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                val newRecord = record.copy(checkedIn = true, checkInTime = now())
                saving = true
                error = null
                scope.launch {
                    val result = repository.saveAttendance(salesCode, newRecord)
                    saving = false
                    if (result.isSuccess) {
                        record = newRecord
                    } else {
                        error = result.exceptionOrNull()?.localizedMessage ?: "Could not save attendance."
                    }
                }
            },
            enabled = !record.checkedIn && !saving && salesCode.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    saving -> "SAVING..."
                    record.checkedIn -> "ATTENDANCE MARKED ✓"
                    else -> "MARK ATTENDANCE"
                }
            )
        }

        Button(onClick = onContinue, enabled = record.checkedIn && !saving, modifier = Modifier.fillMaxWidth()) {
            Text("CONTINUE TO TODAY'S PLAN")
        }
    }
}
