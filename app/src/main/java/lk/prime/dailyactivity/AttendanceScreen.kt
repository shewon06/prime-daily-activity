package lk.prime.dailyactivity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

private val AttendanceDark = Color(0xFF031B12)
private val AttendanceCard = Color(0xFF0A2B1D)
private val AttendanceGreen = Color(0xFF123D2A)
private val AttendanceGold = Color(0xFFD6A62E)
private val AttendanceSuccess = Color(0xFF35B94B)

private suspend fun compressProfilePhoto(
    context: android.content.Context,
    uri: android.net.Uri
): String = withContext(Dispatchers.IO) {
    val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        ?: error("Could not read selected image.")

    val maxDimension = 360f
    val scale = min(1f, min(maxDimension / original.width.toFloat(), maxDimension / original.height.toFloat()))
    val width = (original.width * scale).toInt().coerceAtLeast(1)
    val height = (original.height * scale).toInt().coerceAtLeast(1)
    val resized = if (width != original.width || height != original.height) {
        Bitmap.createScaledBitmap(original, width, height, true)
    } else {
        original
    }

    val output = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 72, output)
    val bytes = output.toByteArray()

    if (resized !== original) resized.recycle()
    original.recycle()

    require(bytes.size < 700_000) { "Selected photo is too large. Please choose another photo." }
    "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}

@Composable
private fun StaffPhotoImage(photoValue: String?, modifier: Modifier = Modifier) {
    if (photoValue.isNullOrBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("👤", fontSize = 28.sp)
        }
        return
    }

    if (photoValue.startsWith("data:image")) {
        val image = remember(photoValue) {
            runCatching {
                val encoded = photoValue.substringAfter("base64,")
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            Box(modifier = modifier, contentAlignment = Alignment.Center) { Text("👤", fontSize = 28.sp) }
        }
    } else {
        AsyncImage(
            model = photoValue,
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun AttendanceScreen(
    salesCode: String,
    repository: DataRepository,
    onContinue: () -> Unit
) {
    var record by remember { mutableStateOf(AttendanceRecord()) }
    var attendanceLoaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var photoValue by remember { mutableStateOf<String?>(null) }
    var photoUploading by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val now: () -> String = { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) }
    val date = remember { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()) }
    val day = remember { SimpleDateFormat("EEEE", Locale.US).format(Date()) }

    LaunchedEffect(salesCode) {
        attendanceLoaded = false
        if (salesCode.isNotBlank()) {
            repository.getStaffBySalesCode(salesCode).getOrNull()?.let {
                photoValue = it.photoUri
            }
            val attendanceResult = repository.getTodayAttendance(salesCode)
            attendanceResult.getOrNull()?.let { record = it }
            attendanceResult.exceptionOrNull()?.let {
                error = it.localizedMessage ?: "Could not load today's attendance."
            }
        }
        attendanceLoaded = true
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && salesCode.isNotBlank() && !photoUploading) {
            photoUploading = true
            photoError = null
            scope.launch {
                val result = runCatching {
                    val encodedPhoto = compressProfilePhoto(context, uri)
                    repository.updateProfilePhoto(salesCode, encodedPhoto).getOrThrow()
                    encodedPhoto
                }
                photoUploading = false
                result.onSuccess { photoValue = it }
                    .onFailure { photoError = it.localizedMessage ?: "Could not save profile photo." }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AttendanceDark)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ATTENDANCE", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("Start your workday", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
            }
            PrimeOfficialLogo(modifier = Modifier.width(142.dp).height(80.dp))
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
                    Text(salesCode.ifBlank { "—" }, color = AttendanceGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AttendanceCard)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(62.dp).background(AttendanceGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    StaffPhotoImage(
                        photoValue = photoValue,
                        modifier = Modifier.size(54.dp).clip(CircleShape)
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("PROFILE PHOTO", color = AttendanceGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (photoValue.isNullOrBlank()) "Add your staff photo" else "Profile photo saved",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    photoError?.let { Text(it, color = Color(0xFFFF7B7B), fontSize = 9.sp) }
                }
                TextButton(
                    onClick = { photoPicker.launch("image/*") },
                    enabled = !photoUploading && salesCode.isNotBlank()
                ) {
                    Text(
                        if (photoUploading) "SAVING..." else if (photoValue.isNullOrBlank()) "ADD" else "CHANGE",
                        color = AttendanceGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
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
                    if (!attendanceLoaded) {
                        CircularProgressIndicator(
                            color = AttendanceGold,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Text(
                            if (record.checkedIn) "✓" else "◷",
                            color = if (record.checkedIn) AttendanceSuccess else AttendanceGold,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    when {
                        !attendanceLoaded -> "CHECKING TODAY'S ATTENDANCE"
                        record.checkedIn -> "ATTENDANCE MARKED"
                        else -> "READY TO START?"
                    },
                    color = if (record.checkedIn) AttendanceSuccess else Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    when {
                        !attendanceLoaded -> "Please wait a moment."
                        record.checkedIn && record.checkedOut -> "Checked in at ${record.checkInTime} • Day ended at ${record.checkOutTime}"
                        record.checkedIn -> "Checked in at ${record.checkInTime}"
                        else -> "Mark your attendance before creating today's plan."
                    },
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 13.sp
                )
            }
        }

        error?.let { Text(it, color = Color(0xFFFF7B7B), fontSize = 12.sp) }

        Button(
            onClick = {
                val newRecord = AttendanceRecord(
                    checkedIn = true,
                    checkedOut = false,
                    checkInTime = now(),
                    checkOutTime = null
                )
                saving = true
                error = null
                scope.launch {
                    val result = repository.saveAttendance(salesCode, newRecord)
                    if (result.isSuccess) {
                        record = repository.getTodayAttendance(salesCode).getOrNull() ?: newRecord
                    } else {
                        error = result.exceptionOrNull()?.localizedMessage ?: "Could not save attendance."
                    }
                    saving = false
                }
            },
            enabled = attendanceLoaded && !record.checkedIn && !saving && salesCode.isNotBlank(),
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
                    !attendanceLoaded -> "CHECKING..."
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
            enabled = attendanceLoaded && record.checkedIn && !saving,
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
            "Attendance can be marked only once per day. Your first check-in time is kept for the full day.",
            color = Color.White.copy(alpha = 0.46f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp)
        )
    }
}
