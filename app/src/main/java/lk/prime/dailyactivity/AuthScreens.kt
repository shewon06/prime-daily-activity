package lk.prime.dailyactivity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: () -> Unit,
    loading: Boolean = false,
    error: String? = null
) {
    var salesCode by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        Text("PRIME", fontSize = 38.sp, fontWeight = FontWeight.Black, color = PrimeColors.Green)
        Text("Daily Activity", fontSize = 20.sp)
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            salesCode,
            { salesCode = it.trim() },
            label = { Text("Sales Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            pin,
            { pin = it },
            label = { Text("PIN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onLogin(salesCode, pin) },
            enabled = !loading && salesCode.isNotBlank() && pin.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "SIGNING IN..." else "LOGIN")
        }
        TextButton(onClick = onRegister, enabled = !loading) { Text("New staff? Register here") }
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
    val Green = androidx.compose.ui.graphics.Color(0xFF123D2A)
    val Gold = androidx.compose.ui.graphics.Color(0xFFD6A62E)
}
