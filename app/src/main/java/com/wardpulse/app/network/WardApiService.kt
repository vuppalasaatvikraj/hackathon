package com.wardpulse.app.network

import com.wardpulse.app.model.*
import retrofit2.http.*

interface WardPulseApiService {

    @POST("auth/register")
    suspend fun registerHospital(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun loginHospital(@Body request: LoginRequest): AuthResponse

    @GET("patients")
    suspend fun getPatients(
        @Header("Authorization") bearerToken: String,
        @Query("ward") ward: String = "ALL",
        @Query("updated_since") updatedSince: Long = 0L
    ): List<PatientResponse>

    @POST("patients")
    suspend fun addOrUpdatePatient(
        @Header("Authorization") bearerToken: String,
        @Body vitals: VitalsRequest
    ): PatientResponse

    @POST("patients/batch-sync")
    suspend fun batchSyncPatients(
        @Header("Authorization") bearerToken: String,
        @Body batch: BatchSyncRequest
    ): List<PatientResponse>

    @DELETE("patients/{patientId}")
    suspend fun deletePatient(
        @Header("Authorization") bearerToken: String,
        @Path("patientId") patientId: String
    ): DeleteResponse

    @HTTP(method = "DELETE", path = "patients", hasBody = true)
    suspend fun bulkDeletePatients(
        @Header("Authorization") bearerToken: String,
        @Body request: BulkDeleteRequest
    ): BulkDeleteResponse

    @POST("patients/reset-demo")
    suspend fun resetDemoPatients(
        @Header("Authorization") bearerToken: String
    ): List<PatientResponse>
}