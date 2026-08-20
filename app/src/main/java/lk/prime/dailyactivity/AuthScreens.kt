package lk.prime.dailyactivity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(onLogin: (String) -> Unit, onRegister: () -> Unit) {
    var salesCode by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PRIME", fontSize = 38.sp, fontWeight = FontWeight.Black, color = PrimeColors.Green)
        Text("Daily Activity", fontSize = 20.sp)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(salesCode, { salesCode = it }, label = { Text("Sales Code") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(pin, { pin = it }, label = { Text("PIN") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Button(onClick = { onLogin(salesCode) }, enabled = salesCode.isNotBlank() && pin.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("LOGIN")
        }
        TextButton(onClick = onRegister) { Text("New staff? Register here") }
    }
}

@Composable
fun RegistrationScreen(onSubmit: (StaffProfile) -> Unit, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Staff Registration", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimeColors.Green)
        Text("Register once. Management approval is required before login.")
        OutlinedTextField(code, { code = it }, label = { Text("Sales Code") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(zone, { zone = it }, label = { Text("Zone") }, modifier = Modifier.fillMaxWidth())
        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Profile Photo", fontWeight = FontWeight.Bold)
                Text("Photo capture / gallery selection will be connected in the device-storage phase.")
            }
        }
        Button(
            onClick = { onSubmit(StaffProfile(code, name, mobile, zone)) },
            enabled = code.isNotBlank() && name.isNotBlank() && mobile.isNotBlank() && zone.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("SUBMIT FOR APPROVAL") }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Login") }
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
