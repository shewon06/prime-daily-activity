package lk.prime.dailyactivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
private val PrimeDark = Color(0xFF031B12)
private val PrimeDarkCard = Color(0xFF0A2B1D)
private val ProspectGreen = Color(0xFF35B94B)
private val FollowGold = Color(0xFFE2A91E)
private val AppointmentOrange = Color(0xFFEA6B16)
private val PresentationPurple = Color(0xFF9B58E8)

enum class AppScreen { LOGIN, REGISTER, PENDING, ATTENDANCE, DAILY, MONTHLY_TARGET, MANAGEMENT }

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

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimeGreen,
            secondary = PrimeGold,
            background = PrimeBg
        )
    ) {
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
                                            screen = when (profile.role) {
                                                UserRole.ADMIN, UserRole.ZONAL_MANAGER -> AppScreen.MANAGEMENT
                                                UserRole.STAFF -> AppScreen.ATTENDANCE
                                            }
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
                    onRegister = {
                        authError = null
                        screen = AppScreen.REGISTER
                    },
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
                    onBack = {
                        authError = null
                        screen = AppScreen.LOGIN
                    },
                    loading = busy,
                    error = authError
                )

                AppScreen.PENDING -> PendingApprovalScreen(
                    onBack = {
                        authError = null
                        screen = AppScreen.LOGIN
                    }
                )

                AppScreen.ATTENDANCE -> AttendanceScreen(
                    salesCode = currentProfile?.salesCode.orEmpty(),
                    repository = repository,
                    onContinue = {
                        scope.launch {
                            val code = currentProfile?.salesCode.orEmpty()
                            if (code.isNotBlank()) {
                                currentProfile = repository.getStaffBySalesCode(code).getOrNull() ?: currentProfile
                            }
                            screen = AppScreen.DAILY
                        }
                    }
                )

                AppScreen.DAILY -> DailyActivityScreen(
                    profile = currentProfile,
                    repository = repository,
                    onMonthlyTarget = { screen = AppScreen.MONTHLY_TARGET }
                )

                AppScreen.MONTHLY_TARGET -> MonthlyTargetScreen(
                    profile = currentProfile,
                    repository = repository,
                    onBack = { screen = AppScreen.DAILY }
                )

                AppScreen.MANAGEMENT -> currentProfile?.let { ManagementDashboardScreen(it, repository) }
            }
        }
    }
}

@Composable
private fun DailyActivityScreen(
    profile: StaffProfile?,
    repository: DataRepository,
    onMonthlyTarget: () -> Unit
) {
    val salesCode = profile?.salesCode.orEmpty()
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
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

    fun applyActivity(activity: DailyActivity) {
        prospectPlan = activity.prospectingPlan
        followPlan = activity.followUpsPlan
        appointmentPlan = activity.appointmentsPlan
        presentationPlan = activity.presentationsPlan
        prospectDone = activity.prospectingDone
        followDone = activity.followUpsDone
        appointmentDone = activity.appointmentsDone
        presentationDone = activity.presentationsDone
        dayStarted = activity.planLocked
        dayEnded = activity.dayLocked
    }

    fun currentActivity(
        planLocked: Boolean = dayStarted,
        dayLocked: Boolean = dayEnded
    ) = DailyActivity(
        prospectPlan,
        followPlan,
        appointmentPlan,
        presentationPlan,
        prospectDone,
        followDone,
        appointmentDone,
        presentationDone,
        planLocked,
        dayLocked
    )

    fun save(activity: DailyActivity, onSuccess: () -> Unit = {}) {
        if (salesCode.isBlank() || saving) return
        saving = true
        error = null
        scope.launch {
            val result = repository.saveTodayActivity(salesCode, activity)
            saving = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                error = result.exceptionOrNull()?.localizedMessage ?: "Could not save today's activity."
            }
        }
    }

    fun endDay() {
        if (salesCode.isBlank() || saving || dayEnded) return
        saving = true
        error = null
        scope.launch {
            val activityResult = repository.saveTodayActivity(
                salesCode,
                currentActivity(planLocked = true, dayLocked = true)
            )
            if (activityResult.isFailure) {
                saving = false
                error = activityResult.exceptionOrNull()?.localizedMessage ?: "Could not end the day."
                return@launch
            }

            val attendanceResult = repository.saveAttendance(
                salesCode,
                AttendanceRecord(
                    checkedIn = true,
                    checkedOut = true,
                    checkInTime = null,
                    checkOutTime = currentTime()
                )
            )
            saving = false
            if (attendanceResult.isSuccess) {
                dayEnded = true
            } else {
                error = attendanceResult.exceptionOrNull()?.localizedMessage
                    ?: "Day ended, but checkout could not be saved."
            }
        }
    }

    LaunchedEffect(salesCode) {
        if (salesCode.isNotBlank()) {
            val result = repository.getTodayActivity(salesCode)
            result.getOrNull()?.let { applyActivity(it) }
            result.exceptionOrNull()?.let {
                error = it.localizedMessage ?: "Could not load today's activity."
            }
        }
        loaded = true
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimeGreen)
        }
        return
    }

    if (!dayStarted) {
        MyDayPlanScreen(
            profile = profile,
            prospectPlan = prospectPlan,
            followPlan = followPlan,
            appointmentPlan = appointmentPlan,
            presentationPlan = presentationPlan,
            saving = saving,
            error = error,
            onProspectChange = { prospectPlan = it.coerceAtLeast(0) },
            onFollowChange = { followPlan = it.coerceAtLeast(0) },
            onAppointmentChange = { appointmentPlan = it.coerceAtLeast(0) },
            onPresentationChange = { presentationPlan = it.coerceAtLeast(0) },
            onMonthlyTarget = onMonthlyTarget,
            onStart = {
                save(currentActivity(planLocked = true, dayLocked = false)) {
                    dayStarted = true
                }
            }
        )
        return
    }

    DailyPerformanceScreen(
        profile = profile,
        prospectPlan = prospectPlan,
        followPlan = followPlan,
        appointmentPlan = appointmentPlan,
        presentationPlan = presentationPlan,
        prospectDone = prospectDone,
        followDone = followDone,
        appointmentDone = appointmentDone,
        presentationDone = presentationDone,
        dayEnded = dayEnded,
        saving = saving,
        error = error,
        onProspectChange = {
            prospectDone = it.coerceAtLeast(0)
            save(currentActivity())
        },
        onFollowChange = {
            followDone = it.coerceAtLeast(0)
            save(currentActivity())
        },
        onAppointmentChange = {
            appointmentDone = it.coerceAtLeast(0)
            save(currentActivity())
        },
        onPresentationChange = {
            presentationDone = it.coerceAtLeast(0)
            save(currentActivity())
        },
        onMonthlyTarget = onMonthlyTarget,
        onEndDay = { endDay() }
    )
}

@Composable
private fun DailyPerformanceScreen(
    profile: StaffProfile?,
    prospectPlan: Int,
    followPlan: Int,
    appointmentPlan: Int,
    presentationPlan: Int,
    prospectDone: Int,
    followDone: Int,
    appointmentDone: Int,
    presentationDone: Int,
    dayEnded: Boolean,
    saving: Boolean,
    error: String?,
    onProspectChange: (Int) -> Unit,
    onFollowChange: (Int) -> Unit,
    onAppointmentChange: (Int) -> Unit,
    onPresentationChange: (Int) -> Unit,
    onMonthlyTarget: () -> Unit,
    onEndDay: () -> Unit
) {
    val totalPlan = prospectPlan + followPlan + appointmentPlan + presentationPlan
    val totalDone = prospectDone + followDone + appointmentDone + presentationDone
    val pct = if (totalPlan == 0) 0 else totalDone * 100 / totalPlan
    val date = remember { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()) }
    val displayName = profile?.fullName?.ifBlank { "PRIME Staff" } ?: "PRIME Staff"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimeDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DAILY PERFORMANCE", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Update your completed activities", color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
            }
            PrimeMiniBrand()
        }

        DarkPlanCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(displayName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Sales Code: ${profile?.salesCode.orEmpty()}",
                        color = PrimeGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TODAY", color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(date, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = onMonthlyTarget,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E3A25), contentColor = TargetGoldForDaily)
        ) {
            Text("MONTHLY SALES TARGET  →", fontSize = 13.sp, fontWeight = FontWeight.Black)
        }

        Surface(
            color = Color(0xFF0E3A25),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TODAY'S PLAN", color = PrimeGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text("LOCKED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        DoneTargetCard("PR", "Prospecting", prospectPlan, prospectDone, ProspectGreen, !dayEnded && !saving, onProspectChange)
        DoneTargetCard("FU", "Follow Ups", followPlan, followDone, FollowGold, !dayEnded && !saving, onFollowChange)
        DoneTargetCard("AP", "Appointments", appointmentPlan, appointmentDone, AppointmentOrange, !dayEnded && !saving, onAppointmentChange)
        DoneTargetCard("PS", "Presentations", presentationPlan, presentationDone, PresentationPurple, !dayEnded && !saving, onPresentationChange)

        DarkPlanCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DAILY ACHIEVEMENT", color = PrimeGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("$pct%", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(
                        "$totalDone completed out of $totalPlan planned",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier.size(72.dp).background(PrimeGold.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$pct%", color = PrimeGold, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
            }
            LinearProgressIndicator(
                progress = { pct.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = PrimeGold,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }

        error?.let { Text(it, color = Color(0xFFFF7777), fontSize = 12.sp) }

        Button(
            onClick = onEndDay,
            enabled = !dayEnded && !saving,
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimeGold,
                contentColor = PrimeDark,
                disabledContainerColor = Color(0xFF18452E),
                disabledContentColor = Color.White.copy(alpha = 0.65f)
            )
        ) {
            Text(
                when {
                    dayEnded -> "DAY ENDED • LOCKED"
                    saving -> "SAVING…"
                    else -> "END MY DAY  🔒"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
        }

        Text(
            if (dayEnded) "Your workday is closed. Today's activity is locked."
            else "Update DONE counts as you complete activities. End your day when work is finished.",
            color = Color.White.copy(alpha = 0.58f),
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

private val TargetGoldForDaily = Color(0xFFD6A62E)

@Composable
private fun DoneTargetCard(
    icon: String,
    title: String,
    plan: Int,
    done: Int,
    accent: Color,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    DarkPlanCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ActivityBadge(icon, accent)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("PLAN  $plan", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DONE", color = Color.White.copy(alpha = 0.62f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                CounterButtons(done, accent, enabled, onChange)
            }
        }
    }
}

@Composable
private fun MyDayPlanScreen(
    profile: StaffProfile?,
    prospectPlan: Int,
    followPlan: Int,
    appointmentPlan: Int,
    presentationPlan: Int,
    saving: Boolean,
    error: String?,
    onProspectChange: (Int) -> Unit,
    onFollowChange: (Int) -> Unit,
    onAppointmentChange: (Int) -> Unit,
    onPresentationChange: (Int) -> Unit,
    onMonthlyTarget: () -> Unit,
    onStart: () -> Unit
) {
    val date = remember { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()) }
    val day = remember { SimpleDateFormat("EEEE", Locale.US).format(Date()) }
    val total = prospectPlan + followPlan + appointmentPlan + presentationPlan
    val displayName = profile?.fullName?.ifBlank { "PRIME Staff" } ?: "PRIME Staff"
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "P"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimeDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = PrimeGold, fontSize = 36.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("MY DAY PLAN", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Plan your activities for today", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
            }
            PrimeMiniBrand()
        }

        DarkPlanCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(58.dp).background(PrimeGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ProfilePhotoImage(
                        photoValue = profile?.photoUri,
                        modifier = Modifier.size(50.dp).background(Color(0xFFECECEC), CircleShape),
                        fallbackText = initial,
                        fallbackColor = PrimeGreen,
                        fallbackFontSize = 25.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Good Morning, $displayName 👋", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Sales Code: ${profile?.salesCode.orEmpty()}",
                        color = PrimeGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${profile?.role?.name?.replace('_', ' ') ?: "STAFF"} – ${profile?.zone.orEmpty()}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Today", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                    Text(date, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(day, color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp)
                }
            }
        }

        Button(
            onClick = onMonthlyTarget,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E3A25), contentColor = PrimeGold)
        ) {
            Text("MONTHLY SALES TARGET  →", fontSize = 13.sp, fontWeight = FontWeight.Black)
        }

        DarkPlanCard {
            Text("ⓘ  Plan your day smart!", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "Set your targets for each activity. You can update the DONE count during the day.",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp
            )
        }

        Text("SET YOUR TARGETS FOR TODAY", color = PrimeGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Surface(
            color = Color(0xFF0E3A25),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                "LOCKED AFTER START MY DAY",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        TargetPlanCard("PR", "Prospecting", "New Prospects", prospectPlan, ProspectGreen, onProspectChange)
        TargetPlanCard("FU", "Follow Ups", "Existing Customers", followPlan, FollowGold, onFollowChange)
        TargetPlanCard("AP", "Appointments", "Meetings / Appointments", appointmentPlan, AppointmentOrange, onAppointmentChange)
        TargetPlanCard("PS", "Presentations", "Product Presentations", presentationPlan, PresentationPurple, onPresentationChange)

        DarkPlanCard {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("TOTAL TARGETS", color = ProspectGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("All activities combined", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                }
                Text(total.toString(), color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
            }
        }

        DarkPlanCard {
            Text("PLAN LOCK", color = PrimeGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(
                "Once you tap START MY DAY, your plan will be locked.",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("You can only update DONE counts during the day.", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
        }

        error?.let { Text(it, color = Color(0xFFFF7777), fontSize = 12.sp) }

        Button(
            onClick = onStart,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimeGold, contentColor = PrimeDark)
        ) {
            Text(if (saving) "SAVING…" else "▶  START MY DAY", fontSize = 18.sp, fontWeight = FontWeight.Black)
        }

        Text(
            "Note: Plan realistically. A good plan helps you stay focused and achieve more.",
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun TargetPlanCard(
    icon: String,
    title: String,
    subtitle: String,
    value: Int,
    accent: Color,
    onChange: (Int) -> Unit
) {
    DarkPlanCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ActivityBadge(icon, accent)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TARGET (PLAN)", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                CounterButtons(value, accent, true, onChange)
            }
        }
    }
}

@Composable
private fun ActivityBadge(icon: String, accent: Color) {
    Box(
        modifier = Modifier.size(42.dp).background(accent.copy(alpha = 0.26f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CounterButtons(
    value: Int,
    accent: Color,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { onChange((value - 1).coerceAtLeast(0)) },
            enabled = enabled,
            modifier = Modifier.size(39.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.copy(alpha = 0.16f),
                contentColor = accent,
                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                disabledContentColor = Color.White.copy(alpha = 0.25f)
            )
        ) {
            Text("−", fontSize = 20.sp)
        }

        Text(" $value ", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)

        Button(
            onClick = { onChange(value + 1) },
            enabled = enabled,
            modifier = Modifier.size(39.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.copy(alpha = 0.16f),
                contentColor = accent,
                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                disabledContentColor = Color.White.copy(alpha = 0.25f)
            )
        ) {
            Text("+", fontSize = 20.sp)
        }
    }
}

@Composable
private fun PrimeMiniBrand() {
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("◆", color = PrimeGold, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Text("PRIME", color = PrimeGold, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
        Text("Agri Business & Plantations", color = PrimeGold, fontSize = 8.sp)
    }
}

@Composable
private fun DarkPlanCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimeDarkCard)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}
