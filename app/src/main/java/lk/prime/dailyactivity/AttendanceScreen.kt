package lk.prime.dailyactivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AttendanceDark = Color(0xFF031B12)
private val AttendanceCard = Color(0xFF0A2B1D)
private val AttendanceGreen = Color(0xFF123D2A)
private val AttendanceGold = Color(0xFFD6A62E)
private val AttendanceSuccess = Color(0xFF35B94B)

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
    val date = remember { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()) }
    val day = remember { SimpleDateFormat("EEEE", Locale.US).format(Date()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AttendanceDark)
            .systemBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ATTENDANCE",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Start your workday",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("PRIME", color = AttendanceGold, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("AGRI BUSINESS", color = AttendanceGold.copy(alpha = 0.85f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AttendanceCard)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("TODAY", color = AttendanceGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(date, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(day, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SALES CODE", color = Color.White.copy(alpha = 0.52f), fontSize = 10.sp)
                    Text(
                        salesCode.ifBlank { "—" },
                        color = AttendanceGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AttendanceCard)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            if (record.checkedIn) AttendanceSuccess.copy(alpha = 0.18f) else AttendanceGold.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (record.checkedIn) "✓" else "◷",
                        color = if (record.checkedIn) AttendanceSuccess else AttendanceGold,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    if (record.checkedIn) "ATTENDANCE MARKED" else "READY TO START?",
                    color = if (record.checkedIn) AttendanceSuccess else Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (record.checkedIn) "Checked in at ${record.checkInTime}" else "Mark your attendance before creating today's plan.",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 13.sp
                )
            }
        }

        error?.let {
            Text(it, color = Color(0xFFFF7B7B), fontSize = 12.sp)
        }

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
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AttendanceGold,
                contentColor = AttendanceDark,
                disabledContainerColor = if (record.checkedIn) AttendanceSuccess.copy(alpha = 0.25f) else AttendanceGold.copy(alpha = 0.30f),
                disabledContentColor = if (record.checkedIn) AttendanceSuccess else Color.White.copy(alpha = 0.50f)
            )
        ) {
            Text(
                when {
                    saving -> "SAVING..."
                    record.checkedIn -> "✓  ATTENDANCE MARKED"
                    else -> "MARK ATTENDANCE"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
        }

        Button(
            onClick = onContinue,
            enabled = record.checkedIn && !saving,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AttendanceGreen,
                contentColor = Color.White,
                disabledContainerColor = AttendanceCard,
                disabledContentColor = Color.White.copy(alpha = 0.32f)
            )
        ) {
            Text("CONTINUE TO TODAY'S PLAN  →", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text(
            "Your check-in time is saved automatically when attendance is marked.",
            color = Color.White.copy(alpha = 0.46f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
