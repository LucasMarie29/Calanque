package com.example.calanque.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import retrofit2.http.GET

// 1. Modèle de données correspondant à ton JSON
@Serializable
data class Reservation(
    val id: Int,
    val date: String,
    val commentaire: String,
    val utilisateur_id: Int,
    val statut_reservation_id: Int,
    val activities: List<String> = emptyList() // Liste vide par défaut pour éviter les erreurs
)

// 2. Interface API
interface PanierApiService {
    @GET("api/reservations")
    suspend fun getReservations(): List<Reservation>
}

// 3. ViewModel pour gérer la logique
class PanierViewModel : ViewModel() {
    var reservations by mutableStateOf<List<Reservation>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/")
        .addConverterFactory(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true // Utile si l'API envoie des nulls inattendus
        }.asConverterFactory("application/json".toMediaType()))
        .build()

    private val service = retrofit.create(PanierApiService::class.java)

    init { fetchReservations() }

    private fun fetchReservations() {
        viewModelScope.launch {
            isLoading = true
            try {
                reservations = service.getReservations()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}

// 4. L'écran (UI)
@Composable
fun PanierScreen(viewModel: PanierViewModel = viewModel()) {
    Scaffold(
        topBar = {
            Text(
                "Mes Réservations",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.errorMessage != null) {
                Text(
                    text = "Erreur : ${viewModel.errorMessage}",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else if (viewModel.reservations.isEmpty()) {
                Text(
                    text = "Aucune réservation pour le moment",
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Réservation #${reservation.id}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "📅 Date : ${reservation.date}", style = MaterialTheme.typography.bodyLarge)

            if (reservation.commentaire != "string" && reservation.commentaire.isNotBlank()) {
                Text(
                    text = "💬 Note : ${reservation.commentaire}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Statut : ${reservation.statut_reservation_id}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}