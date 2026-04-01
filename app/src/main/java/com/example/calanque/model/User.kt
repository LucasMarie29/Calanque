package com.example.calanque.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val email: String,
    val nom: String,
    val prenom: String,
    val adresse: String,
    val cp: String,
    val ville: String,
    val telephone: String,
    val role_id: Int,
    val is_active: Boolean
)