package com.wardpulse.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wardpulse.app.model.PatientResponse
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertResultScreen(result: PatientResponse, onBackToQueue: () -> Unit) {
    val (bannerBg, bannerText) = tierColors(result.riskTier)
    val riskPct = (result.riskScore * 100).roundToInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${result.ward} • ${result.bed}", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A), titleContentColor = Color.White)
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bannerBg)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = bannerText, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (result.riskTier == "UNCERTAIN") "UNCERTAIN READING" else "${result.riskTier} ALERT • $riskPct% RISK",
                            color = bannerText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (result.riskTier == "RED" || result.riskTier == "YELLOW") {
                        Spacer(modifier = Modifier.height(10.dp))
                        MagicShimmerBadge(
                            text = "EST. SEPSIS ONSET WINDOW: ~${result.estimatedHoursToOnset} HOURS",
                            baseColor = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Patient: ${result.name} (${result.ward})", color = bannerText.copy(alpha = 0.9f), fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MagicBentoStat(
                    title = "SHOCK INDEX (HR/SBP)",
                    value = "${result.shockIndex}",
                    accentColor = if (result.shockIndex >= 0.9) Color(0xFFDC2626) else Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
                MagicBentoStat(
                    title = "MEAN ARTERIAL BP",
                    value = "${result.map} mmHg",
                    accentColor = if (result.map < 65.0) Color(0xFFDC2626) else Color(0xFF059669),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("CLINICAL TRIGGERS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    result.topReasons.forEach { reason ->
                        Text("•  $reason", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("RECOMMENDED ACTION & REMINDER", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(result.recommendedAction, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0C4A6E))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, null, tint = Color(0xFF0369A1), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val cycleLabel = if (result.recheckIntervalMins == 240) "4-Hour Routine Checkup Alarm Active"
                        else "${result.recheckIntervalMins}-Minute Urgent Re-Check Timer Active"
                        Text(cycleLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBackToQueue,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Back to Ward Urgency Queue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}