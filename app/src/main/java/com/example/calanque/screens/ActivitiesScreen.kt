package com.example.calanque.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import com.example.calanque.R
import com.example.calanque.models.Activity
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

enum class SortOption(val label: String) {
    NONE("Aucun"),
    PRIX_ASC("Prix ↑"),
    PRIX_DESC("Prix ↓"),
    NOM_AZ("A → Z"),
    NOM_ZA("Z → A")
}

interface ActivitiesService {
    @GET("api/activities")
    suspend fun getActivities(): List<Activity>
}

class ActivitiesModel : ViewModel() {

    var activities by mutableStateOf<List<Activity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    var searchQuery by mutableStateOf("")
    var sortOption by mutableStateOf(SortOption.NONE)
    var minPrix by mutableStateOf("")
    var maxPrix by mutableStateOf("")
    var maxDuree by mutableStateOf<Int?>(null)

    fun resetFilters() {
        sortOption = SortOption.NONE
        minPrix = ""
        maxPrix = ""
        maxDuree = null
    }

    val filteredActivities: List<Activity>
        get() {
            var result = if (searchQuery.isEmpty()) activities
            else activities.filter { it.nom.contains(searchQuery, ignoreCase = true) }

            minPrix.toDoubleOrNull()?.let { min -> result = result.filter { it.prix >= min } }
            maxPrix.toDoubleOrNull()?.let { max -> result = result.filter { it.prix <= max } }
            maxDuree?.let { max ->
                result = result.filter {
                    (it.duree?.substringBefore(":")?.toIntOrNull() ?: Int.MAX_VALUE) <= max
                }
            }

            return when (sortOption) {
                SortOption.PRIX_ASC  -> result.sortedBy { it.prix }
                SortOption.PRIX_DESC -> result.sortedByDescending { it.prix }
                SortOption.NOM_AZ    -> result.sortedBy { it.nom }
                SortOption.NOM_ZA    -> result.sortedByDescending { it.nom }
                SortOption.NONE      -> result
            }
        }

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/")
        .addConverterFactory(
            Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())
        )
        .build()

    private val service = retrofit.create(ActivitiesService::class.java)

    init { fetchData() }

    fun fetchData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                activities = service.getActivities()
            } catch (e: Exception) {
                errorMessage = e.message
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}

// ─────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyActivitiesListScreen(
    viewModel: ActivitiesModel = viewModel(),
    onBack: () -> Unit,
    onActivityClick: (Int) -> Unit
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val activeFilterCount = listOfNotNull(
        viewModel.minPrix.takeIf { it.isNotEmpty() },
        viewModel.maxPrix.takeIf { it.isNotEmpty() },
        viewModel.maxDuree,
        viewModel.sortOption.takeIf { it != SortOption.NONE }
    ).size

    Scaffold(
        containerColor = CalanquesTheme.Background,
        topBar = {
            // ── TopBar soignée avec ombre et barre rouge ──
            Box(
                modifier = Modifier
                    .shadow(elevation = 3.dp, spotColor = CalanquesTheme.BlackAlpha12)
                    .background(CalanquesTheme.White)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text       = "Activités",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CalanquesTheme.CalibriFamily,
                                color      = CalanquesTheme.Black
                            )
                            AnimatedVisibility(visible = !viewModel.isLoading && viewModel.activities.isNotEmpty()) {
                                Text(
                                    text       = "${viewModel.filteredActivities.size} disponible(s)",
                                    fontSize   = 11.sp,
                                    fontFamily = CalanquesTheme.CalibriFamily,
                                    color      = CalanquesTheme.Grey
                                )
                            }
                        }
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
                        // Bouton filtre avec badge
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    painter            = painterResource(android.R.drawable.ic_menu_sort_by_size),
                                    contentDescription = "Filtres",
                                    tint               = if (activeFilterCount > 0) CalanquesTheme.Red else CalanquesTheme.Blue,
                                    modifier           = Modifier.size(22.dp)
                                )
                            }
                            if (activeFilterCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .offset(x = (-4).dp, y = 4.dp)
                                        .background(CalanquesTheme.Red, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text     = activeFilterCount.toString(),
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
                // Barre rouge identitaire
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Barre de recherche ───────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CalanquesTheme.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value         = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            "Rechercher une activité…",
                            fontSize   = 14.sp,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            color      = CalanquesTheme.LightGrey
                        )
                    },
                    singleLine    = true,
                    leadingIcon   = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CalanquesTheme.Blue)
                    },
                    trailingIcon  = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = CalanquesTheme.Grey)
                            }
                        }
                    },
                    shape  = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CalanquesTheme.LightGrey,
                        focusedBorderColor   = CalanquesTheme.Blue,
                        unfocusedTextColor   = CalanquesTheme.Black,
                        focusedTextColor     = CalanquesTheme.Black
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = CalanquesTheme.CalibriFamily,
                        fontSize   = 14.sp
                    )
                )
            }

            // ── Chips de tri ─────────────────────────────
            LazyRow(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier            = Modifier.background(CalanquesTheme.White)
            ) {
                items(SortOption.entries.drop(1)) { option ->
                    val isSelected = viewModel.sortOption == option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) CalanquesTheme.Blue else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CalanquesTheme.Blue else CalanquesTheme.LightGrey,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                viewModel.sortOption =
                                    if (viewModel.sortOption == option) SortOption.NONE else option
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text       = option.label,
                            fontSize   = 12.sp,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) Color.White else CalanquesTheme.Grey
                        )
                    }
                }
            }

            // Séparateur discret
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CalanquesTheme.DividerColor)
            )

            // ── Contenu principal ────────────────────────
            when {
                viewModel.isLoading            -> ActivitiesLoadingState()
                viewModel.errorMessage != null -> ActivitiesErrorState(
                    message = viewModel.errorMessage!!,
                    onRetry = { viewModel.fetchData() }
                )
                viewModel.filteredActivities.isEmpty() -> ActivitiesEmptyState(
                    hasSearch = viewModel.searchQuery.isNotEmpty()
                )
                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(viewModel.filteredActivities, key = { it.id }) { activity ->
                            ActivityCard(
                                activity        = activity,
                                onActivityClick = onActivityClick
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Bottom sheet filtres ──────────────────────
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest    = { showFilterSheet = false },
            sheetState          = sheetState,
            containerColor      = CalanquesTheme.White,
            shape               = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(CalanquesTheme.LightGrey, RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Filtres avancés",
                        fontWeight = FontWeight.Bold,
                        fontFamily = CalanquesTheme.CalibriFamily,
                        fontSize   = 17.sp,
                        color      = CalanquesTheme.Black
                    )
                    TextButton(onClick = { viewModel.resetFilters() }) {
                        Text(
                            "Réinitialiser",
                            color      = CalanquesTheme.Red,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            fontSize   = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = CalanquesTheme.DividerColor)

                // Label Prix
                SectionLabel("PRIX (€)")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value         = viewModel.minPrix,
                        onValueChange = { viewModel.minPrix = it },
                        label         = { Text("Min", fontFamily = CalanquesTheme.CalibriFamily) },
                        modifier      = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine    = true,
                        shape         = RoundedCornerShape(8.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = CalanquesTheme.Blue,
                            unfocusedBorderColor = CalanquesTheme.LightGrey
                        )
                    )
                    OutlinedTextField(
                        value         = viewModel.maxPrix,
                        onValueChange = { viewModel.maxPrix = it },
                        label         = { Text("Max", fontFamily = CalanquesTheme.CalibriFamily) },
                        modifier      = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine    = true,
                        shape         = RoundedCornerShape(8.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = CalanquesTheme.Blue,
                            unfocusedBorderColor = CalanquesTheme.LightGrey
                        )
                    )
                }

                // Label Durée
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    SectionLabel("DURÉE MAX")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CalanquesTheme.Blue.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text       = viewModel.maxDuree?.let { "$it h" } ?: "Toutes",
                            fontSize   = 12.sp,
                            fontFamily = CalanquesTheme.CalibriFamily,
                            fontWeight = FontWeight.Bold,
                            color      = CalanquesTheme.Blue
                        )
                    }
                }

                Slider(
                    value         = viewModel.maxDuree?.toFloat() ?: 24f,
                    onValueChange = { viewModel.maxDuree = it.toInt().takeIf { v -> v < 24 } },
                    valueRange    = 1f..24f,
                    steps         = 22,
                    colors        = SliderDefaults.colors(
                        thumbColor       = CalanquesTheme.Blue,
                        activeTrackColor = CalanquesTheme.Blue
                    )
                )

                Button(
                    onClick  = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = CalanquesTheme.Blue)
                ) {
                    Text(
                        "Voir ${viewModel.filteredActivities.size} résultat(s)",
                        fontFamily = CalanquesTheme.CalibriFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = Color.White
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Carte activité moderne
// ─────────────────────────────────────────────
@Composable
fun ActivityCard(
    activity: Activity,
    onActivityClick: (Int) -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp), spotColor = CalanquesTheme.BlackAlpha12)
            .clickable { onActivityClick(activity.id) },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CalanquesTheme.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // ── Image carrée à gauche ──────────────────
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(CalanquesTheme.LightGrey)
            ) {
                if (!activity.image_url.isNullOrBlank()) {
                    AsyncImage(
                        model              = "http://webngo.sio.bts:8001/${activity.image_url}",
                        contentDescription = activity.nom,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop,
                        error              = painterResource(android.R.drawable.ic_menu_gallery)
                    )
                    // Dégradé latéral pour la transition vers le texte
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color(0x22000000))
                                )
                            )
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            painter            = painterResource(android.R.drawable.ic_menu_gallery),
                            contentDescription = null,
                            tint               = CalanquesTheme.Grey,
                            modifier           = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // ── Contenu texte ──────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Nom de l'activité
                Text(
                    text       = activity.nom,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CalanquesTheme.CalibriFamily,
                    color      = CalanquesTheme.Black,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Badges durée & prix
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Badge durée
                    val dureeAffichee = activity.duree
                        ?.substringBeforeLast(":")
                        ?.trimStart('0')
                        ?: "—"

                    ActivityBadge(
                        text  = "${dureeAffichee}h",
                        color = CalanquesTheme.Blue.copy(alpha = 0.10f),
                        textColor = CalanquesTheme.Blue
                    )
                    // Badge prix
                    ActivityBadge(
                        text  = "${activity.prix.toInt()} €",
                        color = CalanquesTheme.LightGreen.copy(alpha = 0.30f),
                        textColor = Color(0xFF3A7D44)
                    )
                }

                Spacer(Modifier.height(4.dp))
            }

            // ── Chevron ───────────────────────────────
            Box(
                modifier         = Modifier
                    .fillMaxHeight()
                    .padding(end = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "›",
                    fontSize   = 24.sp,
                    color      = CalanquesTheme.Blue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Barre rouge en bas de la carte
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(CalanquesTheme.Red)
        )
    }
}

// ─────────────────────────────────────────────
// Badge compact (durée / prix)
// ─────────────────────────────────────────────
@Composable
fun ActivityBadge(text: String, color: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text       = text,
            fontSize   = 11.sp,
            fontFamily = CalanquesTheme.CalibriFamily,
            fontWeight = FontWeight.Bold,
            color      = textColor
        )
    }
}

// ─────────────────────────────────────────────
// État : Chargement (skeleton pulsé)
// ─────────────────────────────────────────────
@Composable
fun ActivitiesLoadingState() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue   = 0.3f,
        targetValue    = 0.9f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(5) {
            // Skeleton card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CalanquesTheme.LightGrey.copy(alpha = alpha))
            ) {
                Row(Modifier.fillMaxSize()) {
                    // Image placeholder
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(CalanquesTheme.Grey.copy(alpha = alpha * 0.4f))
                    )
                    Column(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CalanquesTheme.Grey.copy(alpha = alpha * 0.3f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CalanquesTheme.Grey.copy(alpha = alpha * 0.2f))
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CalanquesTheme.Blue.copy(alpha = alpha * 0.15f))
                            )
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CalanquesTheme.LightGreen.copy(alpha = alpha * 0.25f))
                            )
                        }
                    }
                }
                // Barre rouge skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(CalanquesTheme.Red.copy(alpha = alpha * 0.4f))
                        .align(Alignment.BottomStart)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// État : Erreur
// ─────────────────────────────────────────────
@Composable
fun ActivitiesErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cercle rouge avec icône
            Box(
                modifier         = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CalanquesTheme.RedLight),
                contentAlignment = Alignment.Center
            ) {
                Text("!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CalanquesTheme.Red)
            }

            Text(
                text       = "Connexion impossible",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = CalanquesTheme.CalibriFamily,
                color      = CalanquesTheme.Black
            )
            Text(
                text       = message,
                fontSize   = 12.sp,
                fontFamily = CalanquesTheme.CalibriFamily,
                color      = CalanquesTheme.Grey,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )

            Button(
                onClick  = onRetry,
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = CalanquesTheme.Red),
                modifier = Modifier.height(46.dp)
            ) {
                Text(
                    "Réessayer",
                    fontFamily = CalanquesTheme.CalibriFamily,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// État : Vide (recherche sans résultats)
// ─────────────────────────────────────────────
@Composable
fun ActivitiesEmptyState(hasSearch: Boolean) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(CalanquesTheme.Blue.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = null,
                    tint               = CalanquesTheme.Blue,
                    modifier           = Modifier.size(28.dp)
                )
            }
            Text(
                text       = if (hasSearch) "Aucun résultat trouvé" else "Aucune activité disponible",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = CalanquesTheme.CalibriFamily,
                color      = CalanquesTheme.Black
            )
            if (hasSearch) {
                Text(
                    text       = "Essayez un autre mot-clé ou ajustez vos filtres",
                    fontSize   = 13.sp,
                    fontFamily = CalanquesTheme.CalibriFamily,
                    color      = CalanquesTheme.Grey
                )
            }
        }
    }
}