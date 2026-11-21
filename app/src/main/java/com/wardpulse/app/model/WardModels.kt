package com.wardpulse.app.model

import com.google.gson.annotations.SerializedName

// --- AUTH MODELS (Matches main.py Pydantic models verbatim) ---
data class RegisterRequest(
    @SerializedName("hospital_name") val hospitalName: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("seed_demo_patients") val seedDemoPatients: Boolean = true
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    @SerializedName("hospital_id") val hospitalId: String? = null,
    @SerializedName("hospital_name") val hospitalName: String? = null,
    @SerializedName("email") val email: String? = null
)

// --- PATIENT & VITALS MODELS ---
data class VitalsRequest(
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("name") val name: String,
    @SerializedName("ward") val ward: String,
    @SerializedName("bed") val bed: String,
    @SerializedName("age") val age: Int,
    @SerializedName("hr") val hr: Double,
    @SerializedName("sbp") val sbp: Double,
    @SerializedName("dbp") val dbp: Double,
    @SerializedName("temp") val temp: Double,
    @SerializedName("resp") val resp: Double,
    @SerializedName("o2sat") val o2sat: Double
)

data class PatientResponse(
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("name") val name: String,
    @SerializedName("ward") val ward: String,
    @SerializedName("bed") val bed: String,
    @SerializedName("risk_score") val riskScore: Double,
    @SerializedName("risk_tier") val riskTier: String,
    @SerializedName("estimated_hours_to_onset") val estimatedHoursToOnset: Double,
    @SerializedName("recheck_interval_mins") val recheckIntervalMins: Int,
    @SerializedName("last_checked_epoch") val lastCheckedEpoch: Long,
    @SerializedName("updated_at_epoch") val updatedAtEpoch: Long? = null,
    @SerializedName("shock_index") val shockIndex: Double,
    @SerializedName("map") val map: Double,
    @SerializedName("top_reasons") val topReasons: List<String>,
    @SerializedName("recommended_action") val recommendedAction: String,
    val isOfflineFallback: Boolean = false
)

// --- SYNC & DELETE MODELS ---
data class BatchSyncRequest(
    @SerializedName("patients") val patients: List<VitalsRequest>
)

data class DeleteResponse(
    @SerializedName("status") val status: String? = "deleted",
    @SerializedName("patient_id") val patientId: String? = null
)

data class BulkDeleteRequest(
    @SerializedName("patient_ids") val patientIds: List<String>
)

data class BulkDeleteResponse(
    @SerializedName("deleted_count") val deletedCount: Int = 0
)

data class VitalsPreset(
    val label: String,
    val request: VitalsRequest
)