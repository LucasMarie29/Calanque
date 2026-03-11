package com.example.calanque.model

import kotlinx.serialization.Serializable

@Serializable
data class Reservation(
    val id: Int,
    val date: String,
    val commentaire: String,
    val utilisateur_id: Int,
    val statut_reservation_id: Int
)