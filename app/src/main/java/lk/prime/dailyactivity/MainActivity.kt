package lk.prime.dailyactivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val PrimeGreen = Color(0xFF123D2A)
private val PrimeGold = Color(0xFFD6A62E)
private val PrimeBg = Color(0xFFF5F7F3)

enum class AppScreen { LOGIN, REGISTER, PENDING, ATTENDANCE, DAILY }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PrimeDailyActivityApp() }
    }
}

private fun authEmail(salesCode: String): String =
    salesCode.trim().lowercase().replace(Regex("[^a-z0-9._-]"), "-") + "@prime-staff.app"

@Composable
fun PrimeDailyActivityApp() {
    var screen by remember { mutableStateOf(AppScreen.LOGIN) }
    var currentProfile by remember { mutableStateOf<StaffProfile?>(null) }
    var busy by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val repository = remember { FirebaseDataRepository() }

    MaterialTheme(colorScheme = lightColorScheme(primary = PrimeGreen, secondary = PrimeGold, background = PrimeBg)) {
        Surface(modifier = Modifier.fillMaxSize(), color = PrimeBg) {
            when (screen) {
                AppScreen.LOGIN -> LoginScreen(
                    onLogin = { code, pin ->
                        busy = true
                        authError = null
                        auth.signInWithEmailAndPassword(authEmail(code), pin).addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                busy = false
                                authError = "Invalid Sales Code or PIN."
                            } else {
                                scope.launch {
                                    val profile = repository.getStaffBySalesCode(code).getOrNull()
                                    busy = false
                                    when (profile?.approvalStatus) {
                                        ApprovalStatus.APPROVED -> {
                                            currentProfile = profile
                                            screen = AppScreen.ATTENDANCE
                                        }
                                        ApprovalStatus.PENDING -> {
                                            auth.signOut()
                                            screen = AppScreen.PENDING
                                        }
                                        ApprovalStatus.REJECTED -> {
                                            auth.signOut()
                                            authError = "This registration was not approved."
                                        }
                                        null -> {
                                            auth.signOut()
                                            authError = "Staff profile not found."
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onRegister = { authError = null; screen = AppScreen.REGISTER },
                    loading = busy,
                    error = authError
                )
                AppScreen.REGISTER -> RegistrationScreen(
                    onSubmit = { profile, pin ->
                        busy = true
                        authError = null
                        auth.createUserWithEmailAndPassword(authEmail(profile.salesCode), pin).addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                busy = false
                                authError = task.exception?.localizedMessage ?: "Registration failed."
                            } else {
                                scope.launch {
                                    val result = repository.registerStaff(profile)
                                    busy = false
                                    if (result.isSuccess) {
                                        auth.signOut()
                                        currentProfile = profile
                                        screen = AppScreen.PENDING
                                    } else {
                                        auth.currentUser?.delete()
                                        authError = result.exceptionOrNull()?.localizedMessage ?: "Could not save registration."
                                    }
                                }
                            }
                        }
                    },
                    onBack = { authError = null; screen = AppScreen.LOGIN },
                    loading = busy,
                    error = authError
                )
                AppScreen.PENDING -> PendingApprovalScreen(onBack = { authError = null; screen = AppScreen.LOGIN })
                AppScreen.ATTENDANCE -> AttendanceScreen(
                    salesCode = currentProfile?.salesCode.orEmpty(),
                    repository = repository,
                    onContinue = { screen = AppScreen.DAILY }
                )
                AppScreen.DAILY -> DailyActivityScreen(currentProfile)
            }
        }
    }
}

@Composable
private fun DailyActivityScreen(profile: StaffProfile?) {
    var dayStarted by remember { mutableStateOf(false) }
    var dayEnded by remember { mutableStateOf(false) }
    var prospectPlan by remember { mutableIntStateOf(0) }
    var followPlan by remember { mutableIntStateOf(0) }
    var appointmentPlan by remember { mutableIntStateOf(0) }
    var presentationPlan by remember { mutableIntStateOf(0) }
    var prospectDone by remember { mutableIntStateOf(0) }
    var followDone by remember { mutableIntStateOf(0) }
    var appointmentDone by remember { mutableIntStateOf(0) }
    var presentationDone by remember { mutableIntStateOf(0) }

    Column {
        PrimeHeader()
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(profile?.fullName ?: "PRIME Staff", fontSize = 14.sp, color = Color.Gray)
            Text("Your Daily Activity", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimeGreen)

            if (!dayStarted) {
                Text("Enter counts only. Once START MY DAY is pressed, today's plan cannot be changed.")
                PlanRow("Prospecting", prospectPlan) { prospectPlan = it.coerceAtLeast(0) }
                PlanRow("Follow Ups", followPlan) { followPlan = it.coerceAtLeast(0) }
                PlanRow("Appointments", appointmentPlan) { appointmentPlan = it.coerceAtLeast(0) }
                PlanRow("Presentations", presentationPlan) { presentationPlan = it.coerceAtLeast(0) }
                Button(onClick = { dayStarted = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimeGold)) {
                    Text("START MY DAY  🔒", color = PrimeGreen, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("TODAY'S PLAN • LOCKED 🔒", fontWeight = FontWeight.Bold, color = PrimeGreen)
                DoneRow("Prospecting", prospectPlan, prospectDone, !dayEnded) { prospectDone = it.coerceAtLeast(0) }
                DoneRow("Follow Ups", followPlan, followDone, !dayEnded) { followDone = it.coerceAtLeast(0) }
                DoneRow("Appointments", appointmentPlan, appointmentDone, !dayEnded) { appointmentDone = it.coerceAtLeast(0) }
                DoneRow("Presentations", presentationPlan, presentationDone, !dayEnded) { presentationDone = it.coerceAtLeast(0) }
                val totalPlan = prospectPlan + followPlan + appointmentPlan + presentationPlan
                val totalDone = prospectDone + followDone + appointmentDone + presentationDone
                val pct = if (totalPlan == 0) 0 else totalDone * 100 / totalPlan
                Text("Daily Achievement: $pct%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimeGreen)
                Button(onClick = { dayEnded = true }, enabled = !dayEnded, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimeGold)) {
                    Text(if (dayEnded) "DAY ENDED • LOCKED 🔒" else "END MY DAY  🔒", color = PrimeGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PrimeHeader() {
    Surface(color = PrimeGreen, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("PRIME", color = PrimeGold, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(10.dp))
            Text("Agri Business & Plantations", color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PlanRow(label: String, value: Int, onChange: (Int) -> Unit) = CounterCard(label, "PLAN", value, onChange)

@Composable
private fun DoneRow(label: String, plan: Int, done: Int, enabled: Boolean, onChange: (Int) -> Unit) = CounterCard(label, "DONE / $plan", done, onChange, enabled)

@Composable
private fun CounterCard(label: String, sub: String, value: Int, onChange: (Int) -> Unit, enabled: Boolean = true) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(label, fontWeight = FontWeight.Bold); Text(sub, fontSize = 12.sp, color = Color.Gray) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { onChange(value - 1) }, enabled = enabled) { Text("−") }
                Text("  $value  ", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { onChange(value + 1) }, enabled = enabled) { Text("+") }
            }
        }
    }
}
