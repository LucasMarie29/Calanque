package com.example.calanque.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityType(
    val id: Int,
    val libelle: String,
    val image_url: String?
)

@Serializable
data class Activity(
    val id: Int,
    @SerialName("nom")
    val nom: String = "Inconnu",
    val description: String = "",
    val duree: String? = null,
    @SerialName("tarif")
    val prix: Double = 0.0,
    val type_id: Int = 0,
    @SerialName("image_url")
    val image_url: String?
)

@Serializable
data class AvailabilitySlot(
    val heure: String,
    val places_restantes: Int
)

@Serializable
data class Availability(
    val date: String,
    val slots: List<AvailabilitySlot>
)
