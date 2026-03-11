package com.example.calanque.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calanque.network.PanierApi
import com.example.calanque.model.Reservation // Vérifie bien que ton fichier Reservation est dans ce dossier

@Composable
fun PanierScreen() {
    // État pour stocker la liste des réservations
    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Appel API
    LaunchedEffect(Unit) {
        try {
            val result = PanierApi.retrofitService.getReservation()
            reservations = result
        } catch (e: Exception) {
            errorMessage = "Erreur : ${e.localizedMessage}"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Mon Panier",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        } else if (reservations.isEmpty()) {
            Text(text = "Aucune réservation trouvée.")
        } else {
            LazyColumn {
                items(reservations) { res ->
                    ReservationItem(res)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Réservation n°${reservation.id}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Date : ${reservation.date}")
            if (reservation.commentaire != "string") {
                Text(text = "Note : ${reservation.commentaire}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "Statut : ${reservation.statut_reservation_id}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}