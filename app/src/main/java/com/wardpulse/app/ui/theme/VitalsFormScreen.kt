package com.wardpulse.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wardpulse.app.model.VitalsRequest
import com.wardpulse.app.network.WardRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsFormScreen(
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSubmit: (VitalsRequest) -> Unit
) {
    val presets = remember { WardRepository.getPresets() }
    val defaultReq = presets.first().request

    var patientId by remember { mutableStateOf(defaultReq.patientId) }
    var name by remember { mutableStateOf(defaultReq.name) }
    var ward by remember { mutableStateOf(defaultReq.ward) }
    var bed by remember { mutableStateOf(defaultReq.bed) }
    var age by remember { mutableStateOf(defaultReq.age.toString()) }
    var hr by remember { mutableStateOf(defaultReq.hr.toInt().toString()) }
    var sbp by remember { mutableStateOf(defaultReq.sbp.toInt().toString()) }
    var dbp by remember { mutableStateOf(defaultReq.dbp.toInt().toString()) }
    var temp by remember { mutableStateOf(defaultReq.temp.toString()) }
    var resp by remember { mutableStateOf(defaultReq.resp.toInt().toString()) }
    var o2sat by remember { mutableStateOf(defaultReq.o2sat.toInt().toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("15-Sec Vitals Check", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("1-TAP DEMO PRESETS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            val r = preset.request
                            patientId = r.patientId; name = r.name; ward = r.ward; bed = r.bed
                            age = r.age.toString(); hr = r.hr.toInt().toString(); sbp = r.sbp.toInt().toString()
                            dbp = r.dbp.toInt().toString(); temp = r.temp.toString(); resp = r.resp.toInt().toString()
                            o2sat = r.o2sat.toInt().toString()
                        },
                        label = { Text(preset.label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("SELECT WARD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("General Ward A", "General Ward B", "Post-Op Ward").forEach { w ->
                    FilterChip(
                        selected = ward == w,
                        onClick = { ward = w },
                        label = { Text(w, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = bed, onValueChange = { bed = it }, label = { Text("Bed #") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age (yrs)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Patient Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Spacer(modifier = Modifier.height(14.dp))
            Text("BEDSIDE VITALS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VitalNumberField("Heart Rate (bpm)", hr, { hr = it }, Modifier.weight(1f))
                VitalNumberField("Resp Rate (/min)", resp, { resp = it }, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VitalNumberField("Systolic BP (mmHg)", sbp, { sbp = it }, Modifier.weight(1f))
                VitalNumberField("Diastolic BP (mmHg)", dbp, { dbp = it }, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VitalNumberField("Temp (°C)", temp, { temp = it }, Modifier.weight(1f))
                VitalNumberField("Oxygen SpO2 (%)", o2sat, { o2sat = it }, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    onSubmit(
                        VitalsRequest(
                            patientId = patientId.ifBlank { "P99" },
                            name = name.ifBlank { "Ward Patient" },
                            ward = ward,
                            bed = bed.ifBlank { "Bed 10" },
                            age = age.toIntOrNull() ?: 45,
                            hr = hr.toDoubleOrNull() ?: 80.0,
                            sbp = sbp.toDoubleOrNull() ?: 120.0,
                            dbp = dbp.toDoubleOrNull() ?: 80.0,
                            temp = temp.toDoubleOrNull() ?: 37.0,
                            resp = resp.toDoubleOrNull() ?: 16.0,
                            o2sat = o2sat.toDoubleOrNull() ?: 98.0
                        )
                    )
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isSubmitting) {
                    UiversePulseRadarLoader(label = "Running Sepsis ML...")
                } else {
                    Text("Run Hourly Risk & Onset Check", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VitalNumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}