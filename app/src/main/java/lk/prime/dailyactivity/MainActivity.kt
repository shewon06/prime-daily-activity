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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

private fun currentTime(): String =
    SimpleDateFormat("hh:mm a", Locale.US).format(Date()).lowercase(Locale.US)

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
                        busy = true; authError = null
                        auth.signInWithEmailAndPassword(authEmail(code), pin).addOnCompleteListener { task ->
                            if (!task.isSuccessful) { busy = false; authError = "Invalid Sales Code or PIN." }
                            else scope.launch {
                                val profile = repository.getStaffBySalesCode(code).getOrNull(); busy = false
                                when (profile?.approvalStatus) {
                                    ApprovalStatus.APPROVED -> { currentProfile = profile; screen = AppScreen.ATTENDANCE }
                                    ApprovalStatus.PENDING -> { auth.signOut(); screen = AppScreen.PENDING }
                                    ApprovalStatus.REJECTED -> { auth.signOut(); authError = "This registration was not approved." }
                                    null -> { auth.signOut(); authError = "Staff profile not found." }
                                }
                            }
                        }
                    },
                    onRegister = { authError = null; screen = AppScreen.REGISTER }, loading = busy, error = authError
                )
                AppScreen.REGISTER -> RegistrationScreen(
                    onSubmit = { profile, pin ->
                        busy = true; authError = null
                        auth.createUserWithEmailAndPassword(authEmail(profile.salesCode), pin).addOnCompleteListener { task ->
                            if (!task.isSuccessful) { busy = false; authError = task.exception?.localizedMessage ?: "Registration failed." }
                            else scope.launch {
                                val result = repository.registerStaff(profile); busy = false
                                if (result.isSuccess) { auth.signOut(); currentProfile = profile; screen = AppScreen.PENDING }
                                else { auth.currentUser?.delete(); authError = result.exceptionOrNull()?.localizedMessage ?: "Could not save registration." }
                            }
                        }
                    }, onBack = { authError = null; screen = AppScreen.LOGIN }, loading = busy, error = authError
                )
                AppScreen.PENDING -> PendingApprovalScreen(onBack = { authError = null; screen = AppScreen.LOGIN })
                AppScreen.ATTENDANCE -> AttendanceScreen(salesCode = currentProfile?.salesCode.orEmpty(), repository = repository, onContinue = { screen = AppScreen.DAILY })
                AppScreen.DAILY -> DailyActivityScreen(currentProfile, repository)
            }
        }
    }
}

@Composable
private fun DailyActivityScreen(profile: StaffProfile?, repository: DataRepository) {
    val salesCode = profile?.salesCode.orEmpty()
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dayStarted by remember { mutableStateOf(false) }
    var dayEnded by remember { mutableStateOf(false) }
    var prospectPlan by remember { mutableIntStateOf(0) }; var followPlan by remember { mutableIntStateOf(0) }
    var appointmentPlan by remember { mutableIntStateOf(0) }; var presentationPlan by remember { mutableIntStateOf(0) }
    var prospectDone by remember { mutableIntStateOf(0) }; var followDone by remember { mutableIntStateOf(0) }
    var appointmentDone by remember { mutableIntStateOf(0) }; var presentationDone by remember { mutableIntStateOf(0) }

    fun applyActivity(a: DailyActivity) {
        prospectPlan = a.prospectingPlan; followPlan = a.followUpsPlan; appointmentPlan = a.appointmentsPlan; presentationPlan = a.presentationsPlan
        prospectDone = a.prospectingDone; followDone = a.followUpsDone; appointmentDone = a.appointmentsDone; presentationDone = a.presentationsDone
        dayStarted = a.planLocked; dayEnded = a.dayLocked
    }
    fun currentActivity(planLocked: Boolean = dayStarted, dayLocked: Boolean = dayEnded) = DailyActivity(
        prospectPlan, followPlan, appointmentPlan, presentationPlan, prospectDone, followDone, appointmentDone, presentationDone, planLocked, dayLocked
    )
    fun save(activity: DailyActivity, onSuccess: () -> Unit = {}) {
        if (salesCode.isBlank() || saving) return
        saving = true; error = null
        scope.launch {
            val result = repository.saveTodayActivity(salesCode, activity); saving = false
            if (result.isSuccess) onSuccess() else error = result.exceptionOrNull()?.localizedMessage ?: "Could not save today's activity."
        }
    }
    fun endDay() {
        if (salesCode.isBlank() || saving || dayEnded) return
        saving = true; error = null
        scope.launch {
            val activityResult = repository.saveTodayActivity(salesCode, currentActivity(planLocked = true, dayLocked = true))
            if (activityResult.isFailure) {
                saving = false
                error = activityResult.exceptionOrNull()?.localizedMessage ?: "Could not end the day."
                return@launch
            }
            val attendanceResult = repository.saveAttendance(
                salesCode,
                AttendanceRecord(checkedIn = true, checkedOut = true, checkInTime = null, checkOutTime = currentTime())
            )
            saving = false
            if (attendanceResult.isSuccess) dayEnded = true
            else error = attendanceResult.exceptionOrNull()?.localizedMessage ?: "Day ended, but checkout could not be saved."
        }
    }

    LaunchedEffect(salesCode) {
        if (salesCode.isNotBlank()) {
            val result = repository.getTodayActivity(salesCode)
            result.getOrNull()?.let { applyActivity(it) }
            result.exceptionOrNull()?.let { error = it.localizedMessage ?: "Could not load today's activity." }
        }
        loaded = true
    }

    Column {
        PrimeHeader()
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(profile?.fullName ?: "PRIME Staff", fontSize = 14.sp, color = Color.Gray)
            Text("Your Daily Activity", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimeGreen)
            if (!loaded) { CircularProgressIndicator(); Text("Loading today's plan…"); return@Column }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (!dayStarted) {
                Text("Enter counts only. Once START MY DAY is pressed, today's plan cannot be changed.")
                PlanRow("Prospecting", prospectPlan) { prospectPlan = it.coerceAtLeast(0) }
                PlanRow("Follow Ups", followPlan) { followPlan = it.coerceAtLeast(0) }
                PlanRow("Appointments", appointmentPlan) { appointmentPlan = it.coerceAtLeast(0) }
                PlanRow("Presentations", presentationPlan) { presentationPlan = it.coerceAtLeast(0) }
                Button(onClick = { save(currentActivity(planLocked = true, dayLocked = false)) { dayStarted = true } }, enabled = !saving,
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimeGold)) {
                    Text(if (saving) "SAVING…" else "START MY DAY  🔒", color = PrimeGreen, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("TODAY'S PLAN • LOCKED 🔒", fontWeight = FontWeight.Bold, color = PrimeGreen)
                DoneRow("Prospecting", prospectPlan, prospectDone, !dayEnded && !saving) { prospectDone = it.coerceAtLeast(0); save(currentActivity()) }
                DoneRow("Follow Ups", followPlan, followDone, !dayEnded && !saving) { followDone = it.coerceAtLeast(0); save(currentActivity()) }
                DoneRow("Appointments", appointmentPlan, appointmentDone, !dayEnded && !saving) { appointmentDone = it.coerceAtLeast(0); save(currentActivity()) }
                DoneRow("Presentations", presentationPlan, presentationDone, !dayEnded && !saving) { presentationDone = it.coerceAtLeast(0); save(currentActivity()) }
                val totalPlan = prospectPlan + followPlan + appointmentPlan + presentationPlan
                val totalDone = prospectDone + followDone + appointmentDone + presentationDone
                val pct = if (totalPlan == 0) 0 else totalDone * 100 / totalPlan
                Text("Daily Achievement: $pct%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimeGreen)
                Button(onClick = { endDay() }, enabled = !dayEnded && !saving,
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimeGold)) {
                    Text(if (dayEnded) "DAY ENDED • LOCKED 🔒" else if (saving) "SAVING…" else "END MY DAY  🔒", color = PrimeGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable private fun PrimeHeader() { Surface(color = PrimeGreen, modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text("PRIME", color = PrimeGold, fontSize = 28.sp, fontWeight = FontWeight.Black); Spacer(Modifier.width(10.dp)); Text("Agri Business & Plantations", color = Color.White, fontSize = 13.sp) } } }
@Composable private fun PlanRow(label: String, value: Int, onChange: (Int) -> Unit) = CounterCard(label, "PLAN", value, onChange)
@Composable private fun DoneRow(label: String, plan: Int, done: Int, enabled: Boolean, onChange: (Int) -> Unit) = CounterCard(label, "DONE / $plan", done, onChange, enabled)
@Composable private fun CounterCard(label: String, sub: String, value: Int, onChange: (Int) -> Unit, enabled: Boolean = true) { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(label, fontWeight = FontWeight.Bold); Text(sub, fontSize = 12.sp, color = Color.Gray) }; Row(verticalAlignment = Alignment.CenterVertically) { OutlinedButton(onClick = { onChange(value - 1) }, enabled = enabled) { Text("−") }; Text("  $value  ", fontSize = 22.sp, fontWeight = FontWeight.Bold); OutlinedButton(onClick = { onChange(value + 1) }, enabled = enabled) { Text("+") } } } } }
