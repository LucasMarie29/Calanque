package com.example.calanque.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calanque.model.User
import com.example.calanque.navigation.UserSession
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

// 1. Interface API pour l'utilisateur
interface AccountApiService {
    @GET("api/users/me")
    suspend fun getUser(
        @Header("Authorization") token: String // <-- On ajoute l'en-tête ici
    ): User
}

// 2. ViewModel
class AccountViewModel : ViewModel() {
    var user by mutableStateOf<User?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/")
        .addConverterFactory(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }.asConverterFactory("application/json".toMediaType()))
        .build()

    private val service = retrofit.create(AccountApiService::class.java)

    fun fetchUser() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = UserSession.token
                if (!token.isNullOrBlank()) {
                    // On appelle l'endpoint "me" avec le Bearer token
                    user = service.getUser("Bearer $token")
                } else {
                    errorMessage = "Vous n'êtes pas connecté (Token manquant)"
                }
            } catch (e: Exception) {
                Log.e("ACCOUNT_ERROR", "Erreur API", e)
                errorMessage = "Impossible de charger le profil : ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }
}

// 3. L'écran UI
@Composable
fun AccountScreen(
    onNavigateToAuth: () -> Unit,
    viewModel: AccountViewModel = viewModel()
) {
    // On lance le chargement au premier affichage de l'écran
    LaunchedEffect(Unit) {
        viewModel.fetchUser()
    }

    Scaffold(
        topBar = {
            Text(
                "Mon Compte",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                viewModel.errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.errorMessage!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateToAuth) {
                            Text("Se connecter")
                        }
                    }
                }

                viewModel.user != null -> {
                    val currentUser = viewModel.user!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "${currentUser.prenom} ${currentUser.nom}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        AccountDetailRow("Email", currentUser.email)
                        AccountDetailRow("Téléphone", currentUser.telephone ?: "Non renseigné")
                        AccountDetailRow(
                            "Adresse",
                            "${currentUser.adresse}\n${currentUser.cp} ${currentUser.ville}"
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                // Logique de déconnexion : on vide le token
                                UserSession.token = null
                                onNavigateToAuth()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Se déconnecter")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountDetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}