package lk.prime.dailyactivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

private val TargetDark = Color(0xFF031B12)
private val TargetCard = Color(0xFF0A2B1D)
private val TargetGreen = Color(0xFF123D2A)
private val TargetGold = Color(0xFFD6A62E)
private val TargetSuccess = Color(0xFF35B94B)

private fun sriLankaDate(pattern: String): String =
    SimpleDateFormat(pattern, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Colombo")
    }.format(Date())

private fun money(value: Long): String = NumberFormat.getNumberInstance(Locale.US).format(value)

@Composable
fun MonthlyTargetScreen(
    profile: StaffProfile?,
    repository: DataRepository,
    onBack: () -> Unit
) {
    val salesCode = profile?.salesCode.orEmpty()
    val monthKey = remember { sriLankaDate("yyyy-MM") }
    val dateKey = remember { sriLankaDate("yyyy-MM-dd") }
    val monthName = remember { sriLankaDate("MMMM yyyy").uppercase(Locale.US) }
    val scope = rememberCoroutineScope()

    var record by remember { mutableStateOf(MonthlySalesTarget(monthKey = monthKey)) }
    var targetText by remember { mutableStateOf("") }
    var achievementText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val result = repository.getMonthlySalesTarget(salesCode, monthKey, dateKey)
        result.onSuccess {
            record = it
            if (it.targetLocked) targetText = it.targetAmount.toString()
        }.onFailure {
            error = it.localizedMessage ?: "Could not load monthly target."
        }
    }

    LaunchedEffect(salesCode, monthKey, dateKey) {
        loading = true
        error = null
        if (salesCode.isNotBlank()) reload()
        loading = false
    }

    val balance = max(record.targetAmount - record.achievedAmount, 0L)
    val percentage = if (record.targetAmount <= 0L) 0.0
    else (record.achievedAmount.toDouble() / record.targetAmount.toDouble()) * 100.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TargetDark)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text("‹", color = TargetGold, fontSize = 34.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("MONTHLY TARGET", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text(monthName, color = TargetGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            PrimeOfficialLogo(modifier = Modifier.width(112.dp).height(58.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TargetCard)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile?.fullName?.ifBlank { "PRIME Staff" } ?: "PRIME Staff", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Sales Code: $salesCode", color = TargetGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${profile?.zone.orEmpty()}", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
            }
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TargetGold)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TargetCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("MONTHLY SALES TARGET", color = TargetGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(
                                if (record.targetLocked) "LOCKED 🔒" else "NOT SET / NOT LOCKED",
                                color = if (record.targetLocked) TargetSuccess else Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (record.targetLocked) {
                            Text("Rs. ${money(record.targetAmount)}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    if (!record.targetLocked) {
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { targetText = it.filter(Char::isDigit) },
                            label = { Text("Monthly Target (Rs.)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = TargetGold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.30f),
                                focusedLabelColor = TargetGold,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.60f),
                                cursorColor = TargetGold
                            )
                        )

                        Button(
                            onClick = {
                                val amount = targetText.toLongOrNull() ?: 0L
                                saving = true
                                error = null
                                message = null
                                scope.launch {
                                    val result = repository.lockMonthlySalesTarget(salesCode, monthKey, amount)
                                    if (result.isSuccess) {
                                        reload()
                                        message = "Monthly target locked successfully."
                                    } else {
                                        error = result.exceptionOrNull()?.localizedMessage ?: "Could not lock monthly target."
                                    }
                                    saving = false
                                }
                            },
                            enabled = !saving && targetText.toLongOrNull()?.let { it > 0L } == true,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TargetGold, contentColor = TargetDark)
                        ) {
                            Text(if (saving) "SAVING..." else "LOCK MONTHLY TARGET  🔒", fontWeight = FontWeight.Black)
                        }
                    }

                    Text(
                        "Target can be set on any day of the month. Once locked, it cannot be edited for that month.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TargetCard)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TODAY'S ACHIEVEMENT", color = TargetGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("Already added today: Rs. ${money(record.todayAchievement)}", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)

                    OutlinedTextField(
                        value = achievementText,
                        onValueChange = { achievementText = it.filter(Char::isDigit) },
                        label = { Text("Add Achievement (Rs.)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TargetSuccess,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.30f),
                            focusedLabelColor = TargetSuccess,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.60f),
                            cursorColor = TargetSuccess
                        )
                    )

                    Button(
                        onClick = {
                            val amount = achievementText.toLongOrNull() ?: 0L
                            saving = true
                            error = null
                            message = null
                            scope.launch {
                                val result = repository.addSalesAchievement(salesCode, monthKey, dateKey, amount)
                                if (result.isSuccess) {
                                    achievementText = ""
                                    reload()
                                    message = "Achievement added."
                                } else {
                                    error = result.exceptionOrNull()?.localizedMessage ?: "Could not add achievement."
                                }
                                saving = false
                            }
                        },
                        enabled = !saving && achievementText.toLongOrNull()?.let { it > 0L } == true,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TargetGreen, contentColor = Color.White)
                    ) {
                        Text(if (saving) "SAVING..." else "ADD TODAY'S ACHIEVEMENT  +", fontWeight = FontWeight.Black)
                    }

                    Text(
                        "You can add achievements even before the monthly target is locked.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TargetCard)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MONTHLY PROGRESS", color = TargetGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    TargetSummaryRow("TARGET", if (record.targetAmount > 0L) "Rs. ${money(record.targetAmount)}" else "Not set")
                    TargetSummaryRow("ACHIEVED", "Rs. ${money(record.achievedAmount)}", TargetSuccess)
                    TargetSummaryRow("BALANCE", if (record.targetAmount > 0L) "Rs. ${money(balance)}" else "—")
                    TargetSummaryRow("ACHIEVEMENT", if (record.targetAmount > 0L) String.format(Locale.US, "%.1f%%", percentage) else "—", TargetGold)
                    if (record.targetAmount > 0L) {
                        LinearProgressIndicator(
                            progress = { (percentage / 100.0).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = TargetGold,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }

        message?.let { Text(it, color = TargetSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        error?.let { Text(it, color = Color(0xFFFF7777), fontSize = 12.sp) }

        Text(
            "This month closes automatically at midnight on the last day (Sri Lanka time). The next month starts with a new unlocked target.",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun TargetSummaryRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}
