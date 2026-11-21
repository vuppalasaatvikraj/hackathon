package com.wardpulse.app.network

import com.wardpulse.app.model.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class WardRepository(private val sessionManager: SessionManager) {

    private var cachedPatients: MutableList<PatientResponse> = getInitialDemoPatients().toMutableList()
    private val pendingOfflineSync = mutableListOf<VitalsRequest>()

    private fun buildApi(baseUrl: String): WardPulseApiService {
        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val client = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WardPulseApiService::class.java)
    }

    private fun requireToken(): String = sessionManager.getBearerToken() ?: "Bearer offline-demo-token"

    // --- AUTH ---
    suspend fun login(baseUrl: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val res = buildApi(baseUrl).loginHospital(LoginRequest(email.trim(), password))
            sessionManager.saveSession(res.accessToken, res.hospitalName ?: email.substringBefore("@"), baseUrl)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(baseUrl: String, hospitalName: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val res = buildApi(baseUrl).registerHospital(RegisterRequest(hospitalName.trim(), email.trim(), password))
            sessionManager.saveSession(res.accessToken, res.hospitalName ?: hospitalName, baseUrl)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun enterOfflineDemoMode(baseUrl: String) {
        sessionManager.saveSession("offline-demo-token", "Demo Hospital (Offline)", baseUrl)
    }

    // --- PATIENTS & SYNC ---
    suspend fun fetchWardQueue(baseUrl: String, wardFilter: String = "ALL"): Pair<List<PatientResponse>, Boolean> {
        return try {
            val api = buildApi(baseUrl)
            val token = requireToken()

            if (pendingOfflineSync.isNotEmpty()) {
                api.batchSyncPatients(token, BatchSyncRequest(pendingOfflineSync.toList()))
                pendingOfflineSync.clear()
            }

            val response = api.getPatients(bearerToken = token, ward = wardFilter, updatedSince = 0L)
            if (wardFilter == "ALL") {
                cachedPatients = sortPatients(response).toMutableList()
            }
            Pair(sortPatients(response), false)
        } catch (e: Exception) {
            val filtered = if (wardFilter == "ALL") cachedPatients else cachedPatients.filter { it.ward == wardFilter }
            Pair(sortPatients(filtered), true)
        }
    }

    suspend fun analyzeAndUpdate(baseUrl: String, req: VitalsRequest): Pair<PatientResponse, Boolean> {
        return try {
            val result = buildApi(baseUrl).addOrUpdatePatient(requireToken(), req)
            upsertLocalCache(result)
            Pair(result, false)
        } catch (e: Exception) {
            pendingOfflineSync.add(req)
            val fallbackResult = computeLocalFallback(req)
            upsertLocalCache(fallbackResult)
            Pair(fallbackResult, true)
        }
    }

    suspend fun deletePatient(baseUrl: String, patientId: String): Pair<List<PatientResponse>, Boolean> {
        cachedPatients.removeAll { it.patientId == patientId }
        return try {
            buildApi(baseUrl).deletePatient(requireToken(), patientId)
            Pair(sortPatients(cachedPatients), false)
        } catch (e: Exception) {
            Pair(sortPatients(cachedPatients), true)
        }
    }

    suspend fun resetQueue(baseUrl: String): Pair<List<PatientResponse>, Boolean> {
        return try {
            val resetList = buildApi(baseUrl).resetDemoPatients(requireToken())
            cachedPatients = sortPatients(resetList).toMutableList()
            Pair(cachedPatients, false)
        } catch (e: Exception) {
            cachedPatients = getInitialDemoPatients().toMutableList()
            Pair(sortPatients(cachedPatients), true)
        }
    }

    private fun sortPatients(list: List<PatientResponse>): List<PatientResponse> {
        val tierRank = mapOf("RED" to 0, "YELLOW" to 1, "UNCERTAIN" to 2, "GREEN" to 3)
        return list.sortedWith(
            compareBy<PatientResponse> { tierRank[it.riskTier] ?: 4 }
                .thenBy { it.estimatedHoursToOnset }
                .thenByDescending { it.riskScore }
        )
    }

    private fun upsertLocalCache(patient: PatientResponse) {
        val idx = cachedPatients.indexOfFirst {
            it.patientId == patient.patientId ||
                    (it.ward.equals(patient.ward, ignoreCase = true) && it.bed.equals(patient.bed, ignoreCase = true))
        }
        if (idx >= 0) cachedPatients[idx] = patient else cachedPatients.add(patient)
    }

    private fun computeLocalFallback(req: VitalsRequest): PatientResponse {
        val nowEpoch = System.currentTimeMillis() / 1000L
        val shockIndex = ((req.hr / max(req.sbp, 1.0)) * 100).roundToInt() / 100.0
        val map = ((req.dbp + (req.sbp - req.dbp) / 3.0) * 10).roundToInt() / 10.0

        val isOutOfRange = req.hr < 25 || req.hr > 250 ||
                req.sbp < 40 || req.sbp > 260 ||
                req.dbp < 20 || req.dbp > 160 ||
                req.o2sat < 50 || req.o2sat > 100 ||
                req.temp < 32.0 || req.temp > 43.0 ||
                req.resp < 4 || req.resp > 60

        if (isOutOfRange) {
            return PatientResponse(
                patientId = req.patientId,
                name = req.name,
                ward = req.ward,
                bed = req.bed,
                riskScore = 0.0,
                riskTier = "UNCERTAIN",
                estimatedHoursToOnset = 6.0,
                recheckIntervalMins = 15,
                lastCheckedEpoch = nowEpoch,
                updatedAtEpoch = nowEpoch,
                shockIndex = shockIndex,
                map = map,
                topReasons = listOf("Vitals out of physiological limits (HR=${req.hr.toInt()})."),
                recommendedAction = "Re-check cuff/oximeter and re-enter valid vitals.",
                isOfflineFallback = true
            )
        }

        val reasons = mutableListOf<String>()
        var score = 0.12
        if (shockIndex >= 0.90) { score += 0.48; reasons.add("Rapid Shock Index surge ($shockIndex): HR rising while SBP drops") }
        else if (shockIndex >= 0.78) { score += 0.24; reasons.add("Borderline Shock Index ($shockIndex): early hemodynamic strain") }
        if (req.resp >= 22) { score += 0.22; reasons.add("Tachypnea: breathing rate elevated (${req.resp.toInt()}/min)") }
        if (req.o2sat < 92) { score += 0.25; reasons.add("Hypoxia warning: SpO2 dropped to ${req.o2sat.toInt()}%") }
        if (req.temp >= 38.3 || req.temp < 36.0) { score += 0.12; reasons.add("Abnormal core temperature (${req.temp}°C)") }
        if (reasons.isEmpty()) reasons.add("All 6 bedside vitals and ratios within safe range")

        val clampedScore = (min(max(score, 0.05), 0.98) * 100).roundToInt() / 100.0
        val tier = when {
            clampedScore >= 0.70 -> "RED"
            clampedScore >= 0.35 -> "YELLOW"
            else -> "GREEN"
        }
        val hoursToOnset = when (tier) {
            "RED" -> (((1.0 - clampedScore) * 6.0 + 0.6).coerceIn(0.5, 2.8) * 10).roundToInt() / 10.0
            "YELLOW" -> (((1.0 - clampedScore) * 6.0 + 1.2).coerceIn(2.5, 5.0) * 10).roundToInt() / 10.0
            else -> 6.0
        }
        val intervalMins = when (tier) { "RED" -> 30; "YELLOW" -> 60; else -> 240 }
        val action = when (tier) {
            "RED" -> "URGENT: Call duty doctor now. Re-check vitals in 30 mins."
            "YELLOW" -> "WATCHLIST: Repeat vitals check in 60 mins."
            else -> "STABLE: Routine 4-hour ward checkup schedule."
        }

        return PatientResponse(
            patientId = req.patientId,
            name = req.name,
            ward = req.ward,
            bed = req.bed,
            riskScore = clampedScore,
            riskTier = tier,
            estimatedHoursToOnset = hoursToOnset,
            recheckIntervalMins = intervalMins,
            lastCheckedEpoch = nowEpoch,
            updatedAtEpoch = nowEpoch,
            shockIndex = shockIndex,
            map = map,
            topReasons = reasons,
            recommendedAction = action,
            isOfflineFallback = true
        )
    }

    companion object {
        val WARDS = listOf("ALL", "General Ward A", "General Ward B", "Post-Op Ward")

        fun getPresets(): List<VitalsPreset> = listOf(
            VitalsPreset("Ward A • Bed 4 (Hidden Shock)", VitalsRequest("P04", "Ramesh K.", "General Ward A", "Bed 4", 58, 106.0, 96.0, 60.0, 38.1, 23.0, 94.0)),
            VitalsPreset("Post-Op • Bed 1 (Critical ~1.1h)", VitalsRequest("P01", "Lakshmi D.", "Post-Op Ward", "Bed 1", 67, 124.0, 86.0, 52.0, 39.0, 27.0, 90.0)),
            VitalsPreset("Ward B • Bed 6 (Stable 4h)", VitalsRequest("P06", "Arjun M.", "General Ward B", "Bed 6", 42, 76.0, 120.0, 78.0, 36.8, 16.0, 98.0)),
            VitalsPreset("Invalid Typo (HR=450)", VitalsRequest("P09", "Test Patient", "General Ward A", "Bed 9", 50, 450.0, 110.0, 70.0, 37.0, 18.0, 97.0))
        )

        fun getInitialDemoPatients(): List<PatientResponse> {
            val now = System.currentTimeMillis() / 1000L
            return listOf(
                PatientResponse(
                    patientId = "P01",
                    name = "Lakshmi D.",
                    ward = "Post-Op Ward",
                    bed = "Bed 1",
                    riskScore = 0.92,
                    riskTier = "RED",
                    estimatedHoursToOnset = 1.1,
                    recheckIntervalMins = 30,
                    lastCheckedEpoch = now - 1200,
                    updatedAtEpoch = now - 1200,
                    shockIndex = 1.44,
                    map = 63.3,
                    topReasons = listOf("Severe Shock Index (1.44) — est. ~1.1 hrs to septic shock", "Tachypnea (27/min) & SpO2 90%"),
                    recommendedAction = "URGENT: Call duty doctor now. Re-check vitals in 30 mins."
                ),
                PatientResponse(
                    patientId = "P04",
                    name = "Ramesh K.",
                    ward = "General Ward A",
                    bed = "Bed 4",
                    riskScore = 0.81,
                    riskTier = "RED",
                    estimatedHoursToOnset = 2.1,
                    recheckIntervalMins = 30,
                    lastCheckedEpoch = now - 600,
                    updatedAtEpoch = now - 600,
                    shockIndex = 1.10,
                    map = 72.0,
                    topReasons = listOf("Shock Index elevated (1.10) — est. ~2.1 hrs to deterioration", "BP dropping (96/60 mmHg)"),
                    recommendedAction = "URGENT: Call duty doctor now. Re-check vitals in 30 mins."
                ),
                PatientResponse(
                    patientId = "P03",
                    name = "Suresh V.",
                    ward = "General Ward B",
                    bed = "Bed 3",
                    riskScore = 0.54,
                    riskTier = "YELLOW",
                    estimatedHoursToOnset = 3.8,
                    recheckIntervalMins = 60,
                    lastCheckedEpoch = now - 2400,
                    updatedAtEpoch = now - 2400,
                    shockIndex = 0.84,
                    map = 78.0,
                    topReasons = listOf("Borderline Shock Index (0.84) with fever (38.3°C)"),
                    recommendedAction = "WATCHLIST: Repeat vitals check in 60 mins."
                ),
                PatientResponse(
                    patientId = "P06",
                    name = "Arjun M.",
                    ward = "General Ward A",
                    bed = "Bed 6",
                    riskScore = 0.14,
                    riskTier = "GREEN",
                    estimatedHoursToOnset = 6.0,
                    recheckIntervalMins = 240,
                    lastCheckedEpoch = now - 15300,
                    updatedAtEpoch = now - 15300,
                    shockIndex = 0.63,
                    map = 92.0,
                    topReasons = listOf("Vitals stable — 4-HOUR ROUTINE CHECKUP OVERDUE BY 15 MINS!"),
                    recommendedAction = "DUE NOW: Perform routine 4-hour bedside vitals check."
                )
            )
        }
    }
}