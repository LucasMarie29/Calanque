package com.example.calanque.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import retrofit2.http.GET
import retrofit2.http.Header

@Serializable
data class Reservation(
    val id: Int,
    val date: String,
    val commentaire: String,
    val utilisateur_id: Int,
    val statut_reservation_id: Int,
    val activities: List<String> = emptyList()
)

// 1. L'interface avec le paramètre Header pour le Token
interface PanierApiService {
    @GET("api/reservations")
    suspend fun getReservations(
        @Header("Authorization") token: String
    ): List<Reservation>
}

class PanierViewModel : ViewModel() {
    var reservations by mutableStateOf<List<Reservation>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // On configure le JSON pour être très tolérant
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val service = retrofit.create(PanierApiService::class.java)

    fun fetchReservations() {
        // On récupère les infos de session
        val currentUserId = UserSession.userId
        val currentToken = UserSession.token

        // LOG DE DEBUG : Vérifie dans ton Logcat si ces valeurs s'affichent !
        Log.d("PANIER_DEBUG", "ID: $currentUserId | Token present: ${currentToken != null}")

        if (currentUserId == null || currentToken == null) {
            errorMessage = "Session invalide. Reconnectez-vous."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // IMPORTANT : Vérifie si ton API demande "Bearer " ou juste le token
                val response = service.getReservations(currentToken)

                // On filtre
                reservations = response.filter { it.utilisateur_id == currentUserId }

                Log.d("PANIER_DEBUG", "Nb reservations trouvées : ${reservations.size}")
            } catch (e: Exception) {
                errorMessage = "Erreur : ${e.localizedMessage}"
                Log.e("PANIER_DEBUG", "Erreur API", e)
            } finally {
                isLoading = false
            }
        }
    }
}

@Composable
fun PanierScreen(viewModel: PanierViewModel = viewModel()) {
    // 1. On utilise le token aussi pour être sûr de déclencher au changement
    LaunchedEffect(UserSession.userId, UserSession.token) {
        if (UserSession.userId != null) {
            Log.d("PANIER_DEBUG", "Déclenchement automatique du fetch")
            viewModel.fetchReservations()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mes Réservations", style = MaterialTheme.typography.headlineMedium)
                // 2. UN BOUTON MANUEL POUR TESTER
                IconButton(onClick = { viewModel.fetchReservations() }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
    ){ paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (UserSession.userId == null) {
                Text(
                    text = "Connectez-vous pour voir vos réservations",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.errorMessage != null) {
                Column(modifier = Modifier.align(Alignment.Center).padding(16.dp)) {
                    Text(text = viewModel.errorMessage!!, color = Color.Red)
                    Button(onClick = { viewModel.fetchReservations() }) {
                        Text("Réessayer")
                    }
                }
            } else if (viewModel.reservations.isEmpty()) {
                Text(
                    text = "Aucune réservation trouvée",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                ) {
                    items(viewModel.reservations) { res ->
                        ReservationItem(res)
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationItem(reservation: Reservation) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Réservation #${reservation.id}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "📅 Date : ${reservation.date}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Statut : ${reservation.statut_reservation_id}", style = MaterialTheme.typography.labelMedium)
        }
    }
}