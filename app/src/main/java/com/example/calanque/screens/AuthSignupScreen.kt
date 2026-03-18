package com.example.calanque.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

val text = "Hello toast!"
val duration = Toast.LENGTH_SHORT


// 1. Modèles de données pour l'Inscription
@Serializable
data class SignupRequest(
    val email: String,
    val nom: String,
    val prenom: String,
    val adresse: String,
    val cp: String,
    val ville: String,
    val telephone: String,
    val password: String,
    val role_id: Int,
    val is_active: Int
)

@Serializable
data class SignupResponse(
    val message: String? = null,
    val id: Int? = null // Dépend de ce que ton API renvoie en cas de succès
)

// 2. Interface API (Peut être fusionnée avec AuthApiService)
interface SignupApiService {
    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse
}

// 3. ViewModel pour gérer la logique d'inscription
class SignupViewModel : ViewModel() {
    var email by mutableStateOf("")
    var nom by mutableStateOf("")
    var prenom by mutableStateOf("")
    var adresse by mutableStateOf("")
    var cp by mutableStateOf("")
    var ville by mutableStateOf("")
    var telephone by mutableStateOf("")
    var password by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isSuccess by mutableStateOf(false)

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/")
        .addConverterFactory(jsonConfig.asConverterFactory("application/json".toMediaType()))
        .build()

    private val service = retrofit.create(SignupApiService::class.java)

    fun onSignupClick(onSuccess: () -> Unit) {
        // Validation simple
        if (email.isBlank() || password.isBlank() || nom.isBlank() || prenom.isBlank()) {
            errorMessage = "Veuillez remplir les champs obligatoires"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val request = SignupRequest(
                    email = email,
                    nom = nom,
                    prenom = prenom,
                    adresse = adresse,
                    cp = cp,
                    ville = ville,
                    telephone = telephone,
                    password = password,
                    role_id = 1,
                    is_active = 1
                )
                service.signup(request)
                isSuccess = true
                onSuccess()
            } catch (e: retrofit2.HttpException) { // <-- D'ABORD L'ERREUR HTTP
                val errorBody = e.response()?.errorBody()?.string()
                android.util.Log.e("DEBUG_API", "Le serveur dit : $errorBody")
                errorMessage = "Erreur validation : $errorBody"
            } catch (e: Exception) { // <-- ENSUITE L'ERREUR GÉNÉRALE
                errorMessage = "Erreur lors de l'inscription. Vérifiez votre connexion."
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}



// 4. L'écran (UI)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onNavigateBack: () -> Unit,
    viewModel: SignupViewModel = viewModel()
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un compte") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AJOUT DE viewModel. devant chaque variable
            OutlinedTextField(
                value = viewModel.nom,
                onValueChange = { viewModel.nom = it },
                label = { Text("Nom") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.prenom,
                onValueChange = { viewModel.prenom = it },
                label = { Text("Prénom") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.telephone,
                onValueChange = { viewModel.telephone = it },
                label = { Text("Téléphone") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedTextField(
                value = viewModel.adresse,
                onValueChange = { viewModel.adresse = it },
                label = { Text("Adresse") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.cp,
                    onValueChange = { viewModel.cp = it },
                    label = { Text("CP") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = viewModel.ville,
                    onValueChange = { viewModel.ville = it },
                    label = { Text("Ville") },
                    modifier = Modifier.weight(2f)
                )
            }

            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("Mot de passe") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        viewModel.onSignupClick(onSuccess = {
                            // 2. On affiche le Toast au moment du succès
                            Toast.makeText(context, "Inscription réussie !", Toast.LENGTH_SHORT).show()

                            // Puis on navigue en arrière
                            onNavigateBack()
                        })
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)

                ) {
                    Text("S'inscrire")
                }
            }
            viewModel.errorMessage?.let {
                Text(text = it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}