package com.example.calanque.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calanque.cart.CartItem
import com.example.calanque.cart.CartManager
import com.example.calanque.navigation.UserSession
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// ─────────────────────────────────────────────
// Models pour l'API réservation (doc §4.4)
// POST /api/reservations
// ─────────────────────────────────────────────
@Serializable
data class ReservationActivityPayload(
    val activite_id: Int,
    val date_activite: String,    // yyyy-MM-dd
    val heure_activite: String,   // HH:mm:ss
    val nb_participants: Int
)

@Serializable
data class ReservationPayload(
    val date: String,             // yyyy-MM-dd (date du jour)
    val commentaire: String = "",
    val activities: List<ReservationActivityPayload>
)

@Serializable
data class ReservationResponse(
    val id: Int,
    val date: String,
    val statut_reservation_id: Int
)

// ─────────────────────────────────────────────
// API
// ─────────────────────────────────────────────
interface ReservationApiService {
    @POST("api/reservations")
    suspend fun createReservation(
        @Header("Authorization") token: String,
        @Body payload: ReservationPayload
    ): Response<ReservationResponse>
}

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────
class PanierViewModel : ViewModel() {
    var isLoading    by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successId    by mutableStateOf<Int?>(null)

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    private val service = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ReservationApiService::class.java)

    // Convertit dd/MM/yyyy → yyyy-MM-dd
    private fun toIso(fr: String): String {
        val parts = fr.split("/")
        return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else fr
    }

    // Convertit "09:00" → "09:00:00"
    private fun toTime(h: String): String = if (h.length == 5) "$h:00" else h

    fun validerReservation() {
        val token = UserSession.token
        if (token == null) {
            errorMessage = "Vous devez être connecté pour réserver."
            return
        }
        if (CartManager.items.isEmpty()) {
            errorMessage = "Le panier est vide."
            return
        }

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        val payload = ReservationPayload(
            date        = today,
            commentaire = "",
            activities  = CartManager.items.map { item ->
                ReservationActivityPayload(
                    activite_id      = item.activity.id,
                    date_activite    = toIso(item.date),
                    heure_activite   = toTime(item.heure),
                    nb_participants  = item.participants
                )
            }
        )

        viewModelScope.launch {
            isLoading    = true
            errorMessage = null
            try {
                val response = service.createReservation("Bearer $token", payload)
                if (response.isSuccessful) {
                    successId = response.body()?.id
                    CartManager.clear()
                    Log.d("PANIER", "Réservation créée : #${successId}")
                } else {
                    errorMessage = "Erreur serveur : ${response.code()} ${response.message()}"
                    Log.e("PANIER", "Erreur API : ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau : ${e.localizedMessage}"
                Log.e("PANIER", "Exception", e)
            } finally {
                isLoading = false
            }
        }
    }
}

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────
@Composable
fun PanierScreen(
    onGoToLogin: () -> Unit = {},
    onReservationSuccess: () -> Unit = {},
    viewModel: PanierViewModel = viewModel()
) {
    val cartItems = CartManager.items

    // Redirige vers login si non connecté et tentative de réserver
    var triedToReserve by remember { mutableStateOf(false) }

    LaunchedEffect(triedToReserve) {
        if (triedToReserve && UserSession.token == null) {
            onGoToLogin()
            triedToReserve = false
        }
    }

    LaunchedEffect(viewModel.successId) {
        if (viewModel.successId != null) {
            onReservationSuccess()
        }
    }

    Scaffold(
        containerColor = CalanquesTheme.Background,
        topBar = {
            Box(
                modifier = Modifier
                    .shadow(elevation = 2.dp, spotColor = CalanquesTheme.BlackAlpha12)
                    .background(CalanquesTheme.White)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(3.dp).height(20.dp).background(CalanquesTheme.Red, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text       = "Mon Panier",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CalanquesTheme.CalibriFamily,
                        color      = CalanquesTheme.Black
                    )
                    if (cartItems.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CalanquesTheme.Red)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("${cartItems.size}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(CalanquesTheme.Red).align(Alignment.BottomStart))
            }
        }
    ) { innerPadding ->

        when {
            // ── Panier vide ───────────────────────────────
            cartItems.isEmpty() && viewModel.successId == null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter            = painterResource(android.R.drawable.ic_menu_agenda),
                            contentDescription = null,
                            tint               = CalanquesTheme.LightGrey,
                            modifier           = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Votre panier est vide",
                            fontSize   = 15.sp,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            color      = CalanquesTheme.Grey
                        )
                        Text(
                            "Ajoutez des activités depuis la liste",
                            fontSize   = 12.sp,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            color      = CalanquesTheme.LightGrey
                        )
                    }
                }
            }

            // ── Chargement ────────────────────────────────
            viewModel.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CalanquesTheme.Blue, strokeWidth = 2.5.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Réservation en cours…", fontSize = 13.sp,
                            fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Grey)
                    }
                }
            }

            // ── Succès ────────────────────────────────────
            viewModel.successId != null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(32.dp))
                                .background(CalanquesTheme.LightGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Réservation confirmée !",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            color      = CalanquesTheme.Black
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Numéro : #${viewModel.successId}",
                            fontSize   = 14.sp,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            color      = CalanquesTheme.Grey
                        )
                    }
                }
            }

            // ── Liste des articles ────────────────────────
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    LazyColumn(
                        modifier            = Modifier.weight(1f),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cartItems, key = { it.id }) { item ->
                            CartItemCard(
                                item     = item,
                                onRemove = { CartManager.remove(item.id) }
                            )
                        }
                    }

                    // ── Récapitulatif + bouton Réserver ───
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, spotColor = CalanquesTheme.BlackAlpha12)
                            .background(CalanquesTheme.White)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Erreur éventuelle
                        if (viewModel.errorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CalanquesTheme.RedLight)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    viewModel.errorMessage!!,
                                    fontSize   = 13.sp,
                                    fontFamily = CalanquesTheme.CalibriFamily,
                                    color      = CalanquesTheme.Red
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total",
                                fontSize   = 14.sp,
                                fontFamily = CalanquesTheme.CalibriFamily,
                                color      = CalanquesTheme.Grey
                            )
                            Text(
                                "${CartManager.total.toInt()} €",
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CalanquesTheme.CalibriFamily,
                                color      = CalanquesTheme.Blue
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (UserSession.token == null) {
                                    triedToReserve = true
                                } else {
                                    viewModel.validerReservation()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = CalanquesTheme.Red)
                        ) {
                            Icon(painterResource(android.R.drawable.ic_menu_send), null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (UserSession.token != null) "Réserver" else "Se connecter pour réserver",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CalanquesTheme.CalibriFamily,
                                color      = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Carte article du panier
// ─────────────────────────────────────────────
@Composable
fun CartItemCard(item: CartItem, onRemove: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = CalanquesTheme.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            // Barre rouge en haut de la carte
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(CalanquesTheme.Red))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = item.activity.nom,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CalanquesTheme.CalibriFamily,
                        color      = CalanquesTheme.Black
                    )
                    Spacer(Modifier.height(6.dp))
                    // Date + heure
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(android.R.drawable.ic_menu_today), null,
                            tint = CalanquesTheme.Blue, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${item.date}  ${item.heure}", fontSize = 13.sp,
                            fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Grey)
                    }
                    Spacer(Modifier.height(3.dp))
                    // Participants
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(android.R.drawable.ic_menu_myplaces), null,
                            tint = CalanquesTheme.Grey, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${item.participants} participant${if (item.participants > 1) "s" else ""}",
                            fontSize = 13.sp, fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Grey)
                    }
                    Spacer(Modifier.height(6.dp))
                    // Prix total de cet article
                    Text(
                        "${item.prixTotal.toInt()} €",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CalanquesTheme.CalibriFamily,
                        color      = CalanquesTheme.Blue
                    )
                }

                // Bouton supprimer
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint               = CalanquesTheme.Red
                    )
                }
            }
        }
    }
}