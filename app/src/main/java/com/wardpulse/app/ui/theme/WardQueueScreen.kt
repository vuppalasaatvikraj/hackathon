package com.wardpulse.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wardpulse.app.model.PatientResponse
import com.wardpulse.app.network.WardRepository
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardQueueScreen(
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    selectedWard: String,
    onSelectWard: (String) -> Unit,
    patients: List<PatientResponse>,
    isLoading: Boolean,
    isOfflineMode: Boolean,
    onRefresh: () -> Unit,
    onResetDemo: () -> Unit,
    onPatientClick: (PatientResponse) -> Unit,
    onNewCheckClick: () -> Unit
) {
    var nowEpoch by remember { mutableLongStateOf(System.currentTimeMillis() / 1000L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            nowEpoch = System.currentTimeMillis() / 1000L
        }
    }

    val dueCount = patients.count { (it.lastCheckedEpoch + it.recheckIntervalMins * 60L) <= nowEpoch || it.riskTier == "RED" }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF090D16), Color(0xFF0F172A))
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("WardPulse", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        MagicShimmerBadge(
                            text = if (isOfflineMode) "OFFLINE SAFE" else "AI TRIAGE LIVE",
                            baseColor = if (isOfflineMode) Color(0xFFF59E0B) else Color(0xFF38BDF8)
                        )
                    }
                    Row {
                        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh", tint = Color.White) }
                        IconButton(onClick = onResetDemo) { Icon(Icons.Default.RestartAlt, "Reset Demo", tint = Color(0xFF94A3B8)) }
                    }
                }

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    singleLine = true,
                    label = { Text("FastAPI Hotspot URL", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WardRepository.WARDS.forEach { ward ->
                        FilterChip(
                            selected = selectedWard == ward,
                            onClick = { onSelectWard(ward) },
                            label = {
                                Text(
                                    text = if (ward == "ALL") "All Wards" else ward,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewCheckClick,
                containerColor = Color(0xFF0284C7),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, "Check Vitals") },
                text = { Text("Check Vitals", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F5F9))
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (dueCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NURSE ALARM: $dueCount bed(s) require immediate escalation or 4-hr checkup!",
                            color = Color(0xFF991B1B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "URGENCY QUEUE (${patients.size} BEDS • CLOSEST ONSET FIRST)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(patients) { patient ->
                    PatientQueueCard(patient = patient, nowEpoch = nowEpoch, onClick = { onPatientClick(patient) })
                }
            }
        }
    }
}

@Composable
private fun PatientQueueCard(patient: PatientResponse, nowEpoch: Long, onClick: () -> Unit) {
    val (badgeBg, badgeText) = tierColors(patient.riskTier)
    val riskPct = (patient.riskScore * 100).roundToInt()

    val dueEpoch = patient.lastCheckedEpoch + (patient.recheckIntervalMins * 60L)
    val secsRemaining = dueEpoch - nowEpoch
    val isOverdue = secsRemaining <= 0
    val timerText = if (isOverdue) {
        val minsOver = maxOf(1L, abs(secsRemaining) / 60L)
        "CHECKUP DUE NOW (${minsOver}m overdue)"
    } else {
        val hrs = secsRemaining / 3600L
        val mins = (secsRemaining % 3600L) / 60L
        if (hrs > 0) "Next ${patient.recheckIntervalMins / 60}h check in ${hrs}h ${mins}m"
        else "Re-check in ${mins}m"
    }

    MagicGlowAlertCard(isUrgent = patient.riskTier == "RED", onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${patient.ward} • ${patient.bed}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )
                Text(
                    text = patient.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = if (patient.riskTier == "UNCERTAIN") "UNCERTAIN" else "${patient.riskTier} • $riskPct%",
                        color = badgeText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (patient.riskTier == "RED" || patient.riskTier == "YELLOW") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Est. Onset: ~${patient.estimatedHoursToOnset} hrs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = patient.topReasons.firstOrNull() ?: "Vitals recorded",
            fontSize = 13.sp,
            color = Color(0xFF334155),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SI: ${patient.shockIndex} | MAP: ${patient.map}",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
            Surface(
                color = if (isOverdue) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = timerText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverdue) Color(0xFFB91C1C) else Color(0xFF0369A1),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

fun tierColors(tier: String): Pair<Color, Color> = when (tier.uppercase()) {
    "RED" -> Pair(Color(0xFFDC2626), Color.White)
    "YELLOW" -> Pair(Color(0xFFD97706), Color.White)
    "GREEN" -> Pair(Color(0xFF059669), Color.White)
    else -> Pair(Color(0xFF475569), Color.White)
}