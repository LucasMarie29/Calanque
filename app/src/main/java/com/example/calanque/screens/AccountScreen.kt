package com.example.calanque.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Importe tout pour mutableStateOf et LaunchedEffect
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
    @GET("api/users/{user_id}")
    suspend fun getUser(
        @Path("user_id") userId: Int,
        @Header("Authorization") token: String // <-- On ajoute l'en-tête ici
    ): User
}

// 2. ViewModel
class AccountViewModel : ViewModel() {
    var user by mutableStateOf<User?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/") // Même URL que ton Panier
        .addConverterFactory(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }.asConverterFactory("application/json".toMediaType()))
        .build()

    private val service = retrofit.create(AccountApiService::class.java)

    fun fetchUser(userId: Int) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // On récupère le token depuis la session
                val token = UserSession.token

                if (token != null) {
                    // On ajoute "Bearer " devant le token, c'est la norme standard
                    Log.d("DEBUG_ACCOUNT", "Appel API avec ID: $userId et Token: ${UserSession.token}")
                    user = service.getUser(userId, "Bearer $token")
                } else {
                    errorMessage = "Session expirée ou token manquant"
                }
            } catch (e: Exception) {
                errorMessage = "Erreur : ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }
}

// 3. L'écran UI
@Composable
fun AccountScreen(userId: Int, onNavigateToAuth: () -> Unit, viewModel: AccountViewModel = viewModel()) {

    // On lance l'appel dès que l'ID change ou que l'écran s'affiche
    LaunchedEffect(userId) {
        viewModel.fetchUser(userId)
    }

    Scaffold(
        topBar = {
            Text("Mon Compte", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(16.dp))
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.errorMessage != null) {
                // CAS ERREUR : On affiche la VRAIE erreur pour comprendre ce qui cloche
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Ici on affiche le vrai message d'erreur en rouge
                    Text(text = viewModel.errorMessage!!, style = MaterialTheme.typography.bodyLarge, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateToAuth) {
                        Text("Aller à la page de connexion")
                    }
                }
            } else if (viewModel.user == null) {
                // CAS OU IL N'Y A PAS DE DONNÉES (ID par défaut qui ne marche pas)
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Erreur avec l'ID $userId : ${viewModel.errorMessage}", color = Color.Red)
                    Button(onClick = onNavigateToAuth, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Se connecter")
                    }
                }
            } else {
                // CAS OU L'UTILISATEUR EST CHARGÉ
                viewModel.user?.let { currentUser ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(text = "${currentUser.prenom} ${currentUser.nom}", style = MaterialTheme.typography.headlineMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        AccountDetailRow("Email", currentUser.email)
                        AccountDetailRow("Téléphone", currentUser.telephone)
                        AccountDetailRow("Adresse", "${currentUser.adresse}\n${currentUser.cp} ${currentUser.ville}")

                        Spacer(modifier = Modifier.weight(1f))

                        // Ici le bouton devient "Se déconnecter"
                        Button(onClick = onNavigateToAuth, modifier = Modifier.fillMaxWidth()) {
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