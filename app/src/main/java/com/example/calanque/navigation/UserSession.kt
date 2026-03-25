package com.example.calanque.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object UserSession {
    // Le "by mutableStateOf" permet à Compose de RE-DESSINER
    // les écrans automatiquement dès que la valeur change
    var token by mutableStateOf<String?>(null)
    var userId by mutableStateOf<Int?>(null)

    fun logout() {
        token = null
        userId = null
    }
}