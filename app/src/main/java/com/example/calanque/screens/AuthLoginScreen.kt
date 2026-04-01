package com.example.calanque.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calanque.navigation.UserSession
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// 1. Modèles de données pour l'Authentification
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val access_token: String,
    val token_type: String,
    val user_id: Int? = null
)

// Modèle pour gérer les erreurs détaillées de ton API (FastAPI)
@Serializable
data class ApiError(
    val detail: List<ErrorDetail>
)

@Serializable
data class ErrorDetail(
    val msg: String,
    val type: String
)

// 2. Interface API
interface AuthApiService {
    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") motDePasse: String
    ): AuthResponse
}

// 3. ViewModel pour gérer la logique de connexion
class AuthViewModel : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var token by mutableStateOf<String?>(null)

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/")
        .addConverterFactory(jsonConfig.asConverterFactory("application/json".toMediaType()))
        .build()

    private val service = retrofit.create(AuthApiService::class.java)

    fun onLoginClick() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Veuillez remplir tous les champs"
            return

        }


        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // On envoie les deux strings directement
                val response = service.login(email, password)
                token = response.access_token
                UserSession.token = response.access_token
                UserSession.userId = response.user_id
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                android.util.Log.e("DEBUG_LOGIN", "Le serveur dit : $errorBody")
                errorMessage = "Identifiants incorrects (422)"
            } catch (e: Exception) {
                errorMessage = "Erreur de connexion"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}

// 4. L'écran (UI)
@Composable
fun AuthScreen(onNavigateToSignup: () -> Unit,
               onLoginSuccess: () -> Unit,
               viewModel: AuthViewModel = viewModel()) {
    LaunchedEffect(viewModel.token) {
        if (viewModel.token != null) {
            onLoginSuccess()
        }
    }
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Connexion",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("Mot de passe") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.onLoginClick() },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Se connecter")
                }
            }
            TextButton(onClick = { onNavigateToSignup() }) {
                Text("Pas encore de compte ? S'inscrire")
            }

            viewModel.errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = Color.Red)
            }

            if (viewModel.token != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Connecté avec succès !", color = Color(0xFF4CAF50))
            }
        }
    }
}