package com.example.calanque.cart

import androidx.compose.runtime.mutableStateListOf
import com.example.calanque.screens.Activity

// ─────────────────────────────────────────────
// Modèle d'un article dans le panier
// ─────────────────────────────────────────────
data class CartItem(
    val id: Long = System.currentTimeMillis(),   // clé unique locale
    val activity: Activity,
    val date: String,       // format dd/MM/yyyy
    val heure: String,      // format HH:mm
    val participants: Int
) {
    val prixTotal: Double get() = activity.tarif * participants
}

// ─────────────────────────────────────────────
// Singleton partagé dans toute l'app
// Le panier est purement côté client (doc §4.4)
// ─────────────────────────────────────────────
object CartManager {
    val items = mutableStateListOf<CartItem>()

    val total: Double get() = items.sumOf { it.prixTotal }

    fun add(item: CartItem) {
        items.add(item)
    }

    fun remove(id: Long) {
        items.removeAll { it.id == id }
    }

    fun clear() {
        items.clear()
    }
}
