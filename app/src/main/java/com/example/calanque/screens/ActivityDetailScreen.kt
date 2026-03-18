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
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import com.example.calanque.R
import java.time.LocalDate
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
    var activity       by mutableStateOf<Activity?>(null)
    var availability   by mutableStateOf<Availability?>(null)
    var isLoading      by mutableStateOf(false)
    var errorMessage   by mutableStateOf<String?>(null)

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
            } catch (_: Exception) { }
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
    onAddToCart: (activity: Activity, date: String, heure: String, participants: Int) -> Unit = { _, _, _, _ -> },
    viewModel: ActivityDetailViewModel = viewModel()
) {
    LaunchedEffect(activityId) { viewModel.load(activityId) }

    // ── État local du formulaire ──────────────────────
    var selectedDate      by remember { mutableStateOf("") }
    var selectedHeure     by remember { mutableStateOf("") }
    var participants      by remember { mutableIntStateOf(1) }
    var showDatePicker    by remember { mutableStateOf(false) }
    var datePickerState   = rememberDatePickerState()

    val frFormatter       = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val isoFormatter      = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Créneaux filtrés selon la date choisie
    val slots = viewModel.availability?.slots ?: emptyList()
    val selectedSlot = slots.firstOrNull { it.heure == selectedHeure }
    val placesRestantes = selectedSlot?.places_restantes ?: 14

    val prixTotal = (viewModel.activity?.tarif ?: 0.0) * participants

    Scaffold(
        containerColor = CalanquesTheme.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = viewModel.activity?.nom ?: "",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.W600,
                        color      = CalanquesTheme.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint               = CalanquesTheme.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CalanquesTheme.White
                )
            )
        }
    ) { innerPadding ->

        if (viewModel.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CalanquesTheme.Blue, strokeWidth = 2.dp)
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
            // ── Image ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
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
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Image", color = CalanquesTheme.Grey, fontSize = 16.sp)
                    }
                }
            }

            // ── Barre rouge ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(CalanquesTheme.Red)
            )

            Column(modifier = Modifier.padding(16.dp)) {

                // ── Description + durée ───────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text       = act.description,
                        fontSize   = 14.sp,
                        color      = CalanquesTheme.Black,
                        lineHeight = 20.sp,
                        modifier   = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter            = painterResource(android.R.drawable.ic_menu_recent_history),
                            contentDescription = "Durée",
                            tint               = CalanquesTheme.Grey,
                            modifier           = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            // "02:00:00" → "2h00"
                            text       = act.duree.take(5).replace(":", "h").trimStart('0'),
                            fontSize   = 13.sp,
                            color      = CalanquesTheme.Grey,
                            fontWeight = FontWeight.W500
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Sélecteur de date ─────────────────────
                OutlinedTextField(
                    value         = selectedDate,
                    onValueChange = {},
                    readOnly      = true,
                    placeholder   = { Text("Sélectionner une date", fontSize = 14.sp) },
                    trailingIcon  = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                painter            = painterResource(android.R.drawable.ic_menu_today),
                                contentDescription = "Calendrier",
                                tint               = CalanquesTheme.Blue
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape  = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CalanquesTheme.LightGrey,
                        focusedBorderColor   = CalanquesTheme.Blue
                    )
                )

                Spacer(Modifier.height(10.dp))

                // ── Sélecteur d'heure ─────────────────────
                if (selectedDate.isNotEmpty() && slots.isNotEmpty()) {
                    SlotSelector(
                        slots           = slots,
                        selectedHeure   = selectedHeure,
                        onHeureSelected = { selectedHeure = it }
                    )
                } else {
                    OutlinedTextField(
                        value         = selectedHeure,
                        onValueChange = {},
                        readOnly      = true,
                        enabled       = selectedDate.isNotEmpty(),
                        placeholder   = { Text("Sélectionner une heure", fontSize = 14.sp) },
                        trailingIcon  = {
                            Icon(
                                painter            = painterResource(android.R.drawable.ic_menu_today),
                                contentDescription = "Heure",
                                tint               = CalanquesTheme.Blue
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CalanquesTheme.LightGrey,
                            focusedBorderColor   = CalanquesTheme.Blue
                        )
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Infos : tarif / places / participants / prix ──
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tarif
                    InfoChip(
                        icon  = "€",
                        value = act.tarif.toInt().toString()
                    )

                    // Places restantes
                    InfoChip(
                        iconRes = android.R.drawable.ic_menu_myplaces,
                        value   = placesRestantes.toString()
                    )

                    // Compteur participants
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter            = painterResource(android.R.drawable.ic_menu_myplaces),
                            contentDescription = "Participants",
                            tint               = CalanquesTheme.Grey,
                            modifier           = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        ParticipantCounter(
                            value     = participants,
                            onMinus   = { if (participants > 1) participants-- },
                            onPlus    = { if (participants < placesRestantes) participants++ }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Prix total
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter            = painterResource(android.R.drawable.ic_menu_edit),
                        contentDescription = "Prix",
                        tint               = CalanquesTheme.Grey,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text       = "${prixTotal.toInt()} €",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.W600,
                        color      = CalanquesTheme.Black
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── Bouton Ajouter au panier ──────────────
                val canAdd = selectedDate.isNotEmpty() && selectedHeure.isNotEmpty()

                Button(
                    onClick = {
                        if (canAdd) onAddToCart(act, selectedDate, selectedHeure, participants)
                    },
                    enabled  = canAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = CalanquesTheme.Blue,
                        disabledContainerColor = CalanquesTheme.LightGrey
                    )
                ) {
                    Icon(
                        painter            = painterResource(android.R.drawable.ic_menu_add),
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = "Ajouter au panier",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.W600,
                        color      = Color.White
                    )
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
                    }) { Text("OK", color = CalanquesTheme.Blue) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Annuler", color = CalanquesTheme.Grey)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Composant : sélecteur de créneaux horaires
// ─────────────────────────────────────────────
@Composable
fun SlotSelector(
    slots: List<AvailabilitySlot>,
    selectedHeure: String,
    onHeureSelected: (String) -> Unit
) {
    Column {
        Text(
            text          = "CRÉNEAUX DISPONIBLES",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.W600,
            color         = CalanquesTheme.Grey,
            letterSpacing = 1.sp,
            modifier      = Modifier.padding(bottom = 8.dp)
        )
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
                        color = when {
                            isSelected -> CalanquesTheme.Blue
                            isFull     -> CalanquesTheme.LightGrey
                            else       -> CalanquesTheme.LightGrey
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        when {
                            isSelected -> CalanquesTheme.Blue.copy(alpha = 0.08f)
                            isFull     -> Color(0xFFF5F5F5)
                            else       -> CalanquesTheme.White
                        }
                    )
                    .clickable(enabled = !isFull) { onHeureSelected(slot.heure) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text      = slot.heure,
                    fontSize  = 14.sp,
                    color     = if (isFull) CalanquesTheme.LightGrey else CalanquesTheme.Black,
                    fontWeight = if (isSelected) FontWeight.W600 else FontWeight.Normal
                )
                Text(
                    text     = if (isFull) "Complet" else "${slot.places_restantes} places",
                    fontSize = 12.sp,
                    color    = when {
                        isFull     -> CalanquesTheme.Red
                        isSelected -> CalanquesTheme.Blue
                        else       -> CalanquesTheme.Grey
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Composant : chip info (tarif, places)
// ─────────────────────────────────────────────
@Composable
fun InfoChip(
    icon: String? = null,
    iconRes: Int? = null,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CalanquesTheme.LightGrey.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        if (icon != null) {
            Text(text = icon, fontSize = 13.sp, color = CalanquesTheme.Grey)
        } else if (iconRes != null) {
            Icon(
                painter            = painterResource(iconRes),
                contentDescription = null,
                tint               = CalanquesTheme.Grey,
                modifier           = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text       = value,
            fontSize   = 14.sp,
            fontWeight = FontWeight.W600,
            color      = CalanquesTheme.Black
        )
    }
}

// ─────────────────────────────────────────────
// Composant : compteur de participants
// ─────────────────────────────────────────────
@Composable
fun ParticipantCounter(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, CalanquesTheme.LightGrey, RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier         = Modifier
                .size(32.dp)
                .clickable(onClick = onMinus),
            contentAlignment = Alignment.Center
        ) {
            Text("−", fontSize = 18.sp, color = CalanquesTheme.Blue, fontWeight = FontWeight.Bold)
        }
        Text(
            text       = value.toString(),
            fontSize   = 15.sp,
            fontWeight = FontWeight.W600,
            color      = CalanquesTheme.Black,
            modifier   = Modifier.padding(horizontal = 8.dp)
        )
        Box(
            modifier         = Modifier
                .size(32.dp)
                .clickable(onClick = onPlus),
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 18.sp, color = CalanquesTheme.Blue, fontWeight = FontWeight.Bold)
        }
    }
}
