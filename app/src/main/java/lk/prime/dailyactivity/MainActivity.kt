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

private val PrimeGreen = Color(0xFF123D2A)
private val PrimeGold = Color(0xFFD6A62E)
private val PrimeBg = Color(0xFFF5F7F3)

data class ActivityCount(val name: String, val plan: Int, val done: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PrimeDailyActivityApp() }
    }
}

@Composable
fun PrimeDailyActivityApp() {
    MaterialTheme(colorScheme = lightColorScheme(primary = PrimeGreen, secondary = PrimeGold, background = PrimeBg)) {
        var dayStarted by remember { mutableStateOf(false) }
        var dayEnded by remember { mutableStateOf(false) }
        var prospectPlan by remember { mutableIntStateOf(20) }
        var followPlan by remember { mutableIntStateOf(15) }
        var appointmentPlan by remember { mutableIntStateOf(5) }
        var presentationPlan by remember { mutableIntStateOf(3) }
        var prospectDone by remember { mutableIntStateOf(0) }
        var followDone by remember { mutableIntStateOf(0) }
        var appointmentDone by remember { mutableIntStateOf(0) }
        var presentationDone by remember { mutableIntStateOf(0) }

        Surface(modifier = Modifier.fillMaxSize(), color = PrimeBg) {
            Column {
                PrimeHeader()
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Good Morning", fontSize = 14.sp, color = Color.Gray)
                    Text("Your Daily Activity", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimeGreen)

                    if (!dayStarted) {
                        Text("Set today's plan. Once you START MY DAY, plan numbers are permanently locked.")
                        PlanRow("Prospecting", prospectPlan) { prospectPlan = it.coerceAtLeast(0) }
                        PlanRow("Follow Ups", followPlan) { followPlan = it.coerceAtLeast(0) }
                        PlanRow("Appointments", appointmentPlan) { appointmentPlan = it.coerceAtLeast(0) }
                        PlanRow("Presentations", presentationPlan) { presentationPlan = it.coerceAtLeast(0) }
                        Button(
                            onClick = { dayStarted = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimeGold)
                        ) { Text("START MY DAY  🔒", color = PrimeGreen, fontWeight = FontWeight.Bold) }
                    } else {
                        Text("TODAY'S PLAN  •  LOCKED 🔒", fontWeight = FontWeight.Bold, color = PrimeGreen)
                        DoneRow("Prospecting", prospectPlan, prospectDone, !dayEnded) { prospectDone = it.coerceAtLeast(0) }
                        DoneRow("Follow Ups", followPlan, followDone, !dayEnded) { followDone = it.coerceAtLeast(0) }
                        DoneRow("Appointments", appointmentPlan, appointmentDone, !dayEnded) { appointmentDone = it.coerceAtLeast(0) }
                        DoneRow("Presentations", presentationPlan, presentationDone, !dayEnded) { presentationDone = it.coerceAtLeast(0) }

                        val totalPlan = prospectPlan + followPlan + appointmentPlan + presentationPlan
                        val totalDone = prospectDone + followDone + appointmentDone + presentationDone
                        val pct = if (totalPlan == 0) 0 else (totalDone * 100 / totalPlan)
                        Text("Daily Achievement: $pct%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimeGreen)

                        Button(
                            onClick = { dayEnded = true },
                            enabled = !dayEnded,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimeGold)
                        ) { Text(if (dayEnded) "DAY ENDED • LOCKED 🔒" else "END MY DAY  🔒", color = PrimeGreen, fontWeight = FontWeight.Bold) }
                    }
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
private fun PlanRow(label: String, value: Int, onChange: (Int) -> Unit) {
    CounterCard(label, "PLAN", value, true, onChange)
}

@Composable
private fun DoneRow(label: String, plan: Int, done: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold)
                Text("Plan: $plan", color = Color.Gray)
            }
            IconButton(onClick = { onChange(done - 1) }, enabled = enabled) { Text("−", fontSize = 24.sp) }
            Text("$done", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onChange(done + 1) }, enabled = enabled) { Text("+", fontSize = 24.sp) }
        }
    }
}

@Composable
private fun CounterCard(label: String, caption: String, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold)
                Text(caption, color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = { onChange(value - 1) }, enabled = enabled) { Text("−", fontSize = 24.sp) }
            Text("$value", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onChange(value + 1) }, enabled = enabled) { Text("+", fontSize = 24.sp) }
        }
    }
}
