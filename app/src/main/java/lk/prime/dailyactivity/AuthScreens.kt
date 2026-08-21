package lk.prime.dailyactivity

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val LoginDark = Color(0xFF031B12)
private val LoginCard = Color(0xFF0A2B1D)
private val LoginGold = Color(0xFFD6A62E)

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: () -> Unit,
    loading: Boolean = false,
    error: String? = null
) {
    var salesCode by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF06291B), LoginDark)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gold = LoginGold.copy(alpha = 0.055f)
            drawOval(
                color = gold,
                topLeft = Offset(size.width * 0.72f, size.height * 0.05f),
                size = Size(size.width * 0.34f, size.height * 0.22f)
            )
            drawOval(
                color = gold,
                topLeft = Offset(size.width * 0.77f, size.height * 0.14f),
                size = Size(size.width * 0.20f, size.height * 0.15f)
            )
            drawOval(
                color = gold,
                topLeft = Offset(-size.width * 0.12f, size.height * 0.72f),
                size = Size(size.width * 0.42f, size.height * 0.24f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimeOfficialLogo(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(128.dp)
            )

            Text(
                "DAILY ACTIVITY",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "Plan • Perform • Achieve",
                color = LoginGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LoginCard)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "STAFF LOGIN",
                        color = LoginGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Enter your Sales Code and PIN",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = salesCode,
                        onValueChange = { salesCode = it.trim() },
                        label = { Text("Sales Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = LoginGold,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.60f),
                            focusedBorderColor = LoginGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            cursorColor = LoginGold
                        )
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text("PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = LoginGold,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.60f),
                            focusedBorderColor = LoginGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            cursorColor = LoginGold
                        )
                    )

                    if (error != null) {
                        Text(error, color = Color(0xFFFF7B7B), fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onLogin(salesCode, pin) },
                        enabled = !loading && salesCode.isNotBlank() && pin.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoginGold,
                            contentColor = LoginDark,
                            disabledContainerColor = LoginGold.copy(alpha = 0.30f),
                            disabledContentColor = Color.White.copy(alpha = 0.45f)
                        )
                    ) {
                        Text(
                            if (loading) "SIGNING IN..." else "LOGIN",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            TextButton(onClick = onRegister, enabled = !loading) {
                Text("New staff? Register here", color = LoginGold, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "PRIME Daily Activity • v1.0.0",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "© 2026 Damith Hettiarachchi. All Rights Reserved.",
                color = Color.White.copy(alpha = 0.48f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RegistrationScreen(
    onSubmit: (StaffProfile, String) -> Unit,
    onBack: () -> Unit,
    loading: Boolean = false,
    error: String? = null
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var photoValue by remember { mutableStateOf<String?>(null) }
    var photoBusy by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !photoBusy) {
            photoBusy = true
            photoError = null
            scope.launch {
                val result = runCatching { encodeProfilePhoto(context, uri) }
                photoBusy = false
                result.onSuccess { photoValue = it }
                    .onFailure { photoError = it.localizedMessage ?: "Could not prepare profile photo." }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Staff Registration", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimeColors.Green)
        Text("Register once. Management approval is required before login.")
        OutlinedTextField(code, { code = it.trim() }, label = { Text("Sales Code") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(zone, { zone = it }, label = { Text("Zone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(
            pin,
            { pin = it },
            label = { Text("Create PIN (minimum 6 characters)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F4EF))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(70.dp).background(PrimeColors.Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ProfilePhotoImage(photoValue, Modifier.size(62.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("PROFILE PHOTO", color = PrimeColors.Green, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(
                        if (isSavedProfilePhoto(photoValue)) "Photo selected" else "Add your staff photo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text("You can change it later after login.", fontSize = 10.sp, color = Color.Gray)
                    photoError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
                }
                TextButton(
                    onClick = { photoPicker.launch("image/*") },
                    enabled = !photoBusy && !loading
                ) {
                    Text(
                        if (photoBusy) "LOADING..." else if (isSavedProfilePhoto(photoValue)) "CHANGE" else "ADD",
                        color = PrimeColors.Gold,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        Button(
            onClick = {
                onSubmit(
                    StaffProfile(
                        salesCode = code,
                        fullName = name,
                        mobile = mobile,
                        zone = zone,
                        photoUri = photoValue
                    ),
                    pin
                )
            },
            enabled = !loading && !photoBusy && code.isNotBlank() && name.isNotBlank() && mobile.isNotBlank() && zone.isNotBlank() && pin.length >= 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "SUBMITTING..." else "SUBMIT FOR APPROVAL")
        }
        TextButton(onClick = onBack, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Login")
        }
    }
}

@Composable
fun PendingApprovalScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registration Received", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PrimeColors.Green)
        Spacer(Modifier.height(12.dp))
        Text("Your account is pending Management approval.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("BACK TO LOGIN") }
    }
}

object PrimeColors {
    val Green = Color(0xFF123D2A)
    val Gold = Color(0xFFD6A62E)
}
