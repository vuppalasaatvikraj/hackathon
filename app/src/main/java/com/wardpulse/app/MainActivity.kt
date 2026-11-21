package com.wardpulse.app

import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.wardpulse.app.model.PatientResponse
import com.wardpulse.app.network.SessionManager
import com.wardpulse.app.network.WardRepository
import com.wardpulse.app.ui.AlertResultScreen
import com.wardpulse.app.ui.AuthScreen
import com.wardpulse.app.ui.VitalsFormScreen
import com.wardpulse.app.ui.WardQueueScreen
import kotlinx.coroutines.launch

private enum class ActiveScreen { AUTH, QUEUE, FORM, RESULT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(applicationContext)
        val repository = WardRepository(sessionManager)

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                var currentScreen by remember {
                    mutableStateOf(if (sessionManager.isLoggedIn()) ActiveScreen.QUEUE else ActiveScreen.AUTH)
                }
                var serverUrl by remember { mutableStateOf(sessionManager.getServerUrl()) }
                var selectedWard by remember { mutableStateOf("ALL") }
                var patients by remember { mutableStateOf(WardRepository.getInitialDemoPatients()) }
                var selectedResult by remember { mutableStateOf<PatientResponse?>(null) }
                var isLoading by remember { mutableStateOf(false) }
                var isOfflineMode by remember { mutableStateOf(false) }
                var authError by remember { mutableStateOf<String?>(null) }

                fun playNurseAlertTone() {
                    try {
                        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        RingtoneManager.getRingtone(applicationContext, uri)?.play()
                    } catch (_: Exception) {}
                }

                LaunchedEffect(currentScreen, selectedWard) {
                    if (currentScreen == ActiveScreen.QUEUE) {
                        isLoading = true
                        val (list, offline) = repository.fetchWardQueue(serverUrl, selectedWard)
                        patients = list
                        isOfflineMode = offline
                        isLoading = false
                    }
                }

                when (currentScreen) {
                    ActiveScreen.AUTH -> AuthScreen(
                        serverUrl = serverUrl,
                        onServerUrlChange = {
                            serverUrl = it
                            sessionManager.saveServerUrl(it)
                        },
                        isLoading = isLoading,
                        errorMessage = authError,
                        onLogin = { email, password ->
                            scope.launch {
                                isLoading = true
                                authError = null
                                val res = repository.login(serverUrl, email, password)
                                isLoading = false
                                res.onSuccess {
                                    currentScreen = ActiveScreen.QUEUE
                                }.onFailure {
                                    authError = "Login failed: ${it.localizedMessage ?: "Check server URL & credentials"}"
                                }
                            }
                        },
                        onRegister = { hospitalName, email, password ->
                            scope.launch {
                                isLoading = true
                                authError = null
                                val res = repository.register(serverUrl, hospitalName, email, password)
                                isLoading = false
                                res.onSuccess {
                                    currentScreen = ActiveScreen.QUEUE
                                }.onFailure {
                                    authError = "Registration failed: ${it.localizedMessage ?: "Check server URL"}"
                                }
                            }
                        },
                        onContinueOfflineDemo = {
                            repository.enterOfflineDemoMode(serverUrl)
                            isOfflineMode = true
                            currentScreen = ActiveScreen.QUEUE
                        }
                    )

                    ActiveScreen.QUEUE -> WardQueueScreen(
                        serverUrl = serverUrl,
                        onServerUrlChange = {
                            serverUrl = it
                            sessionManager.saveServerUrl(it)
                        },
                        selectedWard = selectedWard,
                        onSelectWard = { selectedWard = it },
                        patients = patients,
                        isLoading = isLoading,
                        isOfflineMode = isOfflineMode,
                        onRefresh = {
                            scope.launch {
                                isLoading = true
                                val (list, offline) = repository.fetchWardQueue(serverUrl, selectedWard)
                                patients = list
                                isOfflineMode = offline
                                isLoading = false
                            }
                        },
                        onResetDemo = {
                            scope.launch {
                                isLoading = true
                                val (list, offline) = repository.resetQueue(serverUrl)
                                patients = if (selectedWard == "ALL") list else list.filter { it.ward == selectedWard }
                                isOfflineMode = offline
                                isLoading = false
                            }
                        },
                        onPatientClick = { patient ->
                            selectedResult = patient
                            currentScreen = ActiveScreen.RESULT
                        },
                        onNewCheckClick = { currentScreen = ActiveScreen.FORM }
                    )

                    ActiveScreen.FORM -> VitalsFormScreen(
                        isSubmitting = isLoading,
                        onBack = { currentScreen = ActiveScreen.QUEUE },
                        onSubmit = { request ->
                            scope.launch {
                                isLoading = true
                                val (result, offline) = repository.analyzeAndUpdate(serverUrl, request)
                                isOfflineMode = offline
                                selectedResult = result
                                if (result.riskTier == "RED") playNurseAlertTone()
                                val (updatedList, _) = repository.fetchWardQueue(serverUrl, selectedWard)
                                patients = updatedList
                                isLoading = false
                                currentScreen = ActiveScreen.RESULT
                            }
                        }
                    )

                    ActiveScreen.RESULT -> {
                        val res = selectedResult
                        if (res != null) AlertResultScreen(result = res, onBackToQueue = { currentScreen = ActiveScreen.QUEUE })
                        else currentScreen = ActiveScreen.QUEUE
                    }
                }
            }
        }
    }
}