package com.example.calanque.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.calanque.cart.CartItem
import com.example.calanque.cart.CartManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import com.example.calanque.R
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────
// Models
// ─────────────────────────────────────────────
@Serializable
data class Activity(
    val id: Int,
    val nom: String,
    val description: String,
    val tarif: Double,
    val duree: String,
    val type_id: Int,
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

// ─────────────────────────────────────────────
// API
// ─────────────────────────────────────────────
interface ActivityApiService {
    @GET("api/activities/{id}")
    suspend fun getActivity(@Path("id") id: Int): Activity

    @GET("api/activities/{id}/availability")
    suspend fun getAvailability(@Path("id") id: Int): Availability
}

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────
class ActivityDetailViewModel : ViewModel() {
    var activity     by mutableStateOf<Activity?>(null)
    var availability by mutableStateOf<Availability?>(null)
    var isLoading    by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/")
        .addConverterFactory(
            Json { ignoreUnknownKeys = true }
                .asConverterFactory("application/json".toMediaType())
        )
        .build()

    private val service = retrofit.create(ActivityApiService::class.java)

    fun load(activityId: Int) {
        viewModelScope.launch {
            isLoading    = true
            errorMessage = null
            try {
                activity = service.getActivity(activityId)
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun loadAvailability(activityId: Int, date: String) {
        viewModelScope.launch {
            try {
                availability = service.getAvailability(activityId)
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
    }
}

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: Int,
    onBack: () -> Unit = {},
    onGoToCart: () -> Unit = {},          // navigation vers le panier après ajout
    viewModel: ActivityDetailViewModel = viewModel()
) {
    LaunchedEffect(activityId) { viewModel.load(activityId) }

    var selectedDate    by remember { mutableStateOf("") }
    var selectedHeure   by remember { mutableStateOf("") }
    var participants    by remember { mutableIntStateOf(1) }
    var showDatePicker  by remember { mutableStateOf(false) }
    var addedToCart     by remember { mutableStateOf(false) }   // snackbar feedback
    val datePickerState = rememberDatePickerState()
    val snackbarHost    = remember { SnackbarHostState() }

    val frFormatter  = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val slots           = viewModel.availability?.slots ?: emptyList()
    val selectedSlot    = slots.firstOrNull { it.heure == selectedHeure }
    val placesRestantes = selectedSlot?.places_restantes ?: 14
    val prixTotal       = (viewModel.activity?.tarif ?: 0.0) * participants

    // Affiche le snackbar après ajout
    LaunchedEffect(addedToCart) {
        if (addedToCart) {
            snackbarHost.showSnackbar("Activité ajoutée au panier !")
            addedToCart = false
        }
    }

    Scaffold(
        containerColor = CalanquesTheme.Background,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            Box(
                modifier = Modifier
                    .shadow(elevation = 2.dp, spotColor = CalanquesTheme.BlackAlpha12)
                    .background(CalanquesTheme.White)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text       = viewModel.activity?.nom ?: "",
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            color      = CalanquesTheme.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector        = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                tint               = CalanquesTheme.Blue
                            )
                        }
                    },
                    actions = {
                        // Badge panier dans la TopBar
                        val cartCount = CartManager.items.size
                        Box(modifier = Modifier.padding(end = 12.dp)) {
                            IconButton(onClick = onGoToCart) {
                                Icon(
                                    painter            = painterResource(android.R.drawable.ic_menu_agenda),
                                    contentDescription = "Panier",
                                    tint               = CalanquesTheme.Blue
                                )
                            }
                            if (cartCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(CalanquesTheme.Red, RoundedCornerShape(8.dp))
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text     = cartCount.toString(),
                                        fontSize = 9.sp,
                                        color    = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(CalanquesTheme.Red)
                        .align(Alignment.BottomStart)
                )
            }
        }
    ) { innerPadding ->

        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CalanquesTheme.Blue, strokeWidth = 2.5.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Chargement…", fontSize = 13.sp, fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Grey)
                }
            }
            return@Scaffold
        }

        val act = viewModel.activity ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Image hero ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(CalanquesTheme.LightGrey)
            ) {
                if (!act.image_url.isNullOrBlank()) {
                    AsyncImage(
                        model              = "http://webngo.sio.bts:8001/${act.image_url}",
                        contentDescription = act.nom,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop,
                        error              = painterResource(android.R.drawable.ic_menu_gallery)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.BottomStart)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000))))
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            painter            = painterResource(android.R.drawable.ic_menu_gallery),
                            contentDescription = null,
                            tint               = CalanquesTheme.Grey,
                            modifier           = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(CalanquesTheme.Red))

            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {

                // ── Description + badge durée ─────────────
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        text       = act.description,
                        fontSize   = 14.sp,
                        fontFamily = CalanquesTheme.CalibriFamily,
                        color      = CalanquesTheme.Black,
                        lineHeight = 21.sp,
                        modifier   = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CalanquesTheme.Blue.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            painter            = painterResource(android.R.drawable.ic_menu_recent_history),
                            contentDescription = "Durée",
                            tint               = CalanquesTheme.Blue,
                            modifier           = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text       = act.duree.take(5).replace(":", "h").trimStart('0'),
                            fontSize   = 13.sp,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            color      = CalanquesTheme.Blue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("RÉSERVATION")
                Spacer(Modifier.height(12.dp))

                // ── Sélecteur de date ─────────────────────
                OutlinedTextField(
                    value         = selectedDate,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Date", fontFamily = CalanquesTheme.CalibriFamily, fontSize = 12.sp) },
                    placeholder   = {
                        Text("Sélectionner une date", fontSize = 14.sp,
                            fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.LightGrey)
                    },
                    trailingIcon  = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(painterResource(android.R.drawable.ic_menu_today), "Calendrier", tint = CalanquesTheme.Blue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    shape    = RoundedCornerShape(8.dp),
                    colors   = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CalanquesTheme.LightGrey,
                        focusedBorderColor   = CalanquesTheme.Blue
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = CalanquesTheme.CalibriFamily, fontSize = 14.sp)
                )

                Spacer(Modifier.height(10.dp))

                // ── Créneaux ou saisie heure ──────────────
                if (selectedDate.isNotEmpty()) {
                    if (slots.isNotEmpty()) {
                        SlotSelector(
                            slots           = slots,
                            selectedHeure   = selectedHeure,
                            onHeureSelected = { selectedHeure = it }
                        )
                    } else {
                        OutlinedTextField(
                            value         = selectedHeure,
                            onValueChange = { selectedHeure = it },
                            label         = { Text("Heure", fontFamily = CalanquesTheme.CalibriFamily, fontSize = 12.sp) },
                            placeholder   = {
                                Text("Ex : 09:00", fontSize = 14.sp,
                                    fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.LightGrey)
                            },
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(8.dp),
                            colors    = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = CalanquesTheme.LightGrey,
                                focusedBorderColor   = CalanquesTheme.Blue
                            ),
                            textStyle = LocalTextStyle.current.copy(fontFamily = CalanquesTheme.CalibriFamily, fontSize = 14.sp)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("DÉTAILS")
                Spacer(Modifier.height(12.dp))

                // ── Chips tarif / places / compteur ───────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(icon = "€", value = "${act.tarif.toInt()} / pers.")
                    InfoChip(iconRes = android.R.drawable.ic_menu_myplaces, value = "$placesRestantes places")
                    ParticipantCounter(
                        value   = participants,
                        onMinus = { if (participants > 1) participants-- },
                        onPlus  = { if (participants < placesRestantes) participants++ }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ── Prix total ────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CalanquesTheme.Blue.copy(alpha = 0.07f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", fontSize = 14.sp, fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Grey)
                    Text(
                        text       = "${prixTotal.toInt()} €",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CalanquesTheme.CalibriFamily,
                        color      = CalanquesTheme.Blue
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── Bouton Ajouter au panier ──────────────
                val canAdd = selectedDate.isNotEmpty() && selectedHeure.isNotEmpty()

                Button(
                    onClick = {
                        if (canAdd) {
                            CartManager.add(
                                CartItem(
                                    activity     = act,
                                    date         = selectedDate,
                                    heure        = selectedHeure,
                                    participants = participants
                                )
                            )
                            addedToCart = true
                        }
                    },
                    enabled  = canAdd,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = CalanquesTheme.Red,
                        disabledContainerColor = CalanquesTheme.LightGrey
                    )
                ) {
                    Icon(painterResource(android.R.drawable.ic_menu_add), null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ajouter au panier", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        fontFamily = CalanquesTheme.CalibriFamily, color = Color.White)
                }

                // ── Lien vers le panier si déjà des items ─
                if (CartManager.items.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick  = onGoToCart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text       = "Voir le panier (${CartManager.items.size} activité${if (CartManager.items.size > 1) "s" else ""})",
                            color      = CalanquesTheme.Blue,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            fontSize   = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // ── DatePickerDialog ──────────────────────────────
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton    = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = java.time.Instant
                                .ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                            selectedDate  = date.format(frFormatter)
                            selectedHeure = ""
                            viewModel.loadAvailability(activityId, date.format(isoFormatter))
                        }
                        showDatePicker = false
                    }) {
                        Text("OK", color = CalanquesTheme.Blue, fontFamily = CalanquesTheme.CalibriFamily, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Annuler", color = CalanquesTheme.Grey, fontFamily = CalanquesTheme.CalibriFamily)
                    }
                }
            ) { DatePicker(state = datePickerState) }
        }
    }
}

// ─────────────────────────────────────────────
// Composants partagés
// ─────────────────────────────────────────────
@Composable
fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(13.dp).background(CalanquesTheme.Red, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Grey, letterSpacing = 2.sp)
    }
}

@Composable
fun SlotSelector(slots: List<AvailabilitySlot>, selectedHeure: String, onHeureSelected: (String) -> Unit) {
    Column {
        SectionLabel("CRÉNEAUX DISPONIBLES")
        Spacer(Modifier.height(8.dp))
        slots.forEach { slot ->
            val isFull     = slot.places_restantes == 0
            val isSelected = slot.heure == selectedHeure
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CalanquesTheme.Blue else CalanquesTheme.LightGrey,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(when {
                        isSelected -> CalanquesTheme.Blue.copy(alpha = 0.08f)
                        isFull     -> Color(0xFFF5F5F5)
                        else       -> CalanquesTheme.White
                    })
                    .clickable(enabled = !isFull) { onHeureSelected(slot.heure) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(slot.heure, fontSize = 14.sp, fontFamily = CalanquesTheme.CalibriFamily,
                    color = if (isFull) CalanquesTheme.LightGrey else CalanquesTheme.Black,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                Text(
                    text = if (isFull) "Complet" else "${slot.places_restantes} places",
                    fontSize = 12.sp, fontFamily = CalanquesTheme.CalibriFamily,
                    color = when { isFull -> CalanquesTheme.Red; isSelected -> CalanquesTheme.Blue; else -> CalanquesTheme.Grey }
                )
            }
        }
    }
}

@Composable
fun InfoChip(icon: String? = null, iconRes: Int? = null, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.clip(RoundedCornerShape(20.dp))
            .background(CalanquesTheme.LightGrey.copy(alpha = 0.30f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (icon != null) Text(icon, fontSize = 13.sp, fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Grey)
        else if (iconRes != null) Icon(painterResource(iconRes), null, tint = CalanquesTheme.Grey, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Black)
    }
}

@Composable
fun ParticipantCounter(value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, CalanquesTheme.LightGrey, RoundedCornerShape(20.dp))
    ) {
        Box(modifier = Modifier.size(34.dp).clickable(onClick = onMinus), contentAlignment = Alignment.Center) {
            Text("−", fontSize = 18.sp, fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Blue, fontWeight = FontWeight.Bold)
        }
        Text(value.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold,
            fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Black, modifier = Modifier.padding(horizontal = 8.dp))
        Box(modifier = Modifier.size(34.dp).clickable(onClick = onPlus), contentAlignment = Alignment.Center) {
            Text("+", fontSize = 18.sp, fontFamily = CalanquesTheme.CalibriFamily, color = CalanquesTheme.Blue, fontWeight = FontWeight.Bold)
        }
    }
}