package lk.prime.dailyactivity

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PrimeLogoBase64 = "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"

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: () -> Unit,
    loading: Boolean = false,
    error: String? = null
) {
    var salesCode by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val logo = remember {
        runCatching {
            val bytes = Base64.decode(PrimeLogoBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF02160F), Color(0xFF073321), Color(0xFF0E4B31))
                )
            )
    ) {
        PineappleWatermark(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(6.dp))

            if (logo != null) {
                Image(
                    bitmap = logo,
                    contentDescription = "PRIME Agri Business & Plantations",
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(116.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("PRIME", fontSize = 36.sp, fontWeight = FontWeight.Black, color = PrimeColors.Gold)
            }

            Text(
                "DAILY ACTIVITY",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                "Plantation Sales Management",
                color = PrimeColors.Gold.copy(alpha = 0.86f),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2B1D).copy(alpha = 0.96f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("WELCOME BACK", color = PrimeColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("Sign in to start your day", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = salesCode,
                        onValueChange = { salesCode = it.trim() },
                        label = { Text("Sales Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp),
                        colors = loginFieldColors()
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text("PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(13.dp),
                        colors = loginFieldColors()
                    )

                    if (error != null) {
                        Text(error, color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onLogin(salesCode, pin) },
                        enabled = !loading && salesCode.isNotBlank() && pin.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimeColors.Gold,
                            contentColor = Color(0xFF052217),
                            disabledContainerColor = Color(0xFF6F6538),
                            disabledContentColor = Color.White.copy(alpha = 0.55f)
                        )
                    ) {
                        Text(if (loading) "SIGNING IN..." else "LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }

                    TextButton(
                        onClick = onRegister,
                        enabled = !loading,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimeColors.Gold)
                    ) {
                        Text("New staff? Register here")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "PRIME • GROW • PERFORM • ACHIEVE",
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PineappleWatermark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val gold = Color(0xFFD6A62E).copy(alpha = 0.055f)
        val cx = size.width * 0.80f
        val bodyWidth = size.width * 0.30f
        val bodyHeight = size.height * 0.39f
        val bodyLeft = cx - bodyWidth / 2f
        val bodyTop = size.height * 0.43f

        drawOval(
            color = gold,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight)
        )

        val leafBaseY = bodyTop + 8f
        val leafTopY = size.height * 0.05f
        val stroke = size.width * 0.018f
        drawLine(gold, Offset(cx, leafBaseY), Offset(cx, leafTopY), strokeWidth = stroke)
        drawLine(gold, Offset(cx - 8f, leafBaseY), Offset(cx - bodyWidth * 0.46f, leafTopY + 28f), strokeWidth = stroke)
        drawLine(gold, Offset(cx + 8f, leafBaseY), Offset(cx + bodyWidth * 0.46f, leafTopY + 34f), strokeWidth = stroke)
        drawLine(gold, Offset(cx - 16f, leafBaseY + 8f), Offset(cx - bodyWidth * 0.33f, leafTopY + 82f), strokeWidth = stroke)
        drawLine(gold, Offset(cx + 16f, leafBaseY + 8f), Offset(cx + bodyWidth * 0.34f, leafTopY + 86f), strokeWidth = stroke)

        var y = bodyTop + bodyHeight * 0.18f
        repeat(4) {
            drawLine(
                gold,
                Offset(bodyLeft + bodyWidth * 0.10f, y),
                Offset(bodyLeft + bodyWidth * 0.90f, y + bodyHeight * 0.18f),
                strokeWidth = 2.5f
            )
            drawLine(
                gold,
                Offset(bodyLeft + bodyWidth * 0.90f, y),
                Offset(bodyLeft + bodyWidth * 0.10f, y + bodyHeight * 0.18f),
                strokeWidth = 2.5f
            )
            y += bodyHeight * 0.18f
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = PrimeColors.Gold,
    unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
    focusedLabelColor = PrimeColors.Gold,
    unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
    cursorColor = PrimeColors.Gold
)

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
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Staff Registration", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimeColors.Green)
        Text("Register once. Management approval is required before login.")
        OutlinedTextField(code, { code = it.trim() }, label = { Text("Sales Code") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(zone, { zone = it }, label = { Text("Zone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(pin, { pin = it }, label = { Text("Create PIN (minimum 6 characters)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Profile Photo", fontWeight = FontWeight.Bold)
                Text("Photo capture / gallery selection will be connected in the device-storage phase.")
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        Button(
            onClick = { onSubmit(StaffProfile(code, name, mobile, zone), pin) },
            enabled = !loading && code.isNotBlank() && name.isNotBlank() && mobile.isNotBlank() && zone.isNotBlank() && pin.length >= 6,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (loading) "SUBMITTING..." else "SUBMIT FOR APPROVAL") }
        TextButton(onClick = onBack, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Back to Login") }
    }
}

@Composable
fun PendingApprovalScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
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
