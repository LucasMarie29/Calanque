package com.example.calanque.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import com.example.calanque.R
import androidx.compose.foundation.Image

// ─────────────────────────────────────────────
// Design Tokens — Calanques Style Guide v2
// Palette & typographie conformes au guide de style
// ─────────────────────────────────────────────
object CalanquesTheme {

    // ── Palette principale (StyleGuide p.2) ──
    val Red        = Color(0xFFE51A2E)
    val Black      = Color(0xFF000000)
    val LightGrey  = Color(0xFFBBBBBB)
    val Grey       = Color(0xFF555555)
    val Blue       = Color(0xFF4472C4)
    val LightGreen = Color(0xFFA8D08D)

    // ── Dérivées UI ──────────────────────────
    val White        = Color(0xFFFFFFFF)
    val Background   = Color(0xFFF2F4F8)
    val CardBg       = Color(0xFFFFFFFF)
    val DividerColor = Color(0xFFE8EAF0)
    val RedLight     = Color(0xFFFFEBEE)
    val BlackAlpha12 = Color(0x1F000000)

    val CalibriFamily = FontFamily(
        Font(R.font.calibri,       FontWeight.Normal),
        Font(R.font.calibri_bold,  FontWeight.Bold),
        Font(R.font.calibri_bold,  FontWeight.SemiBold)
    )
}

// ─────────────────────────────────────────────
// Data / Network layer (inchangé)
// ─────────────────────────────────────────────
@Serializable
data class ActivityType(
    val id: Int,
    val libelle: String,
    val image_url: String?
)

interface MyApiService {
    @GET("api/activity-types/")
    suspend fun getActivities(): List<ActivityType>
}

class ActivitiesViewModel : ViewModel() {
    var activities   by mutableStateOf<List<ActivityType>>(emptyList())
    var isLoading    by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/")
        .addConverterFactory(
            Json { ignoreUnknownKeys = true }
                .asConverterFactory("application/json".toMediaType())
        )
        .build()

    private val service = retrofit.create(MyApiService::class.java)

    init { fetchData() }

    fun fetchData() {
        viewModelScope.launch {
            isLoading    = true
            errorMessage = null
            try {
                activities = service.getActivities().sortedBy { it.libelle }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
}

// ─────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────
@Composable
fun HomeScreen(
    viewModel: ActivitiesViewModel = viewModel(),
    onNavigate: () -> Unit
) {
    Scaffold(
        containerColor = CalanquesTheme.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CalanquesHeader()

            // ── Label de section ─────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trait rouge décoratif
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(14.dp)
                        .background(
                            color = CalanquesTheme.Red,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text          = "TYPES D'ACTIVITÉS",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    fontFamily    = CalanquesTheme.CalibriFamily,
                    color         = CalanquesTheme.Grey,
                    letterSpacing = 2.sp
                )
            }

            // ── Zone de contenu ──────────────────────────
            when {
                viewModel.isLoading            -> LoadingIndicator()
                viewModel.errorMessage != null -> ErrorCard(
                    message = viewModel.errorMessage!!,
                    onRetry = { viewModel.fetchData() }
                )
                viewModel.activities.isEmpty() -> EmptyState()
                else -> ActivityTypeList(
                    activities  = viewModel.activities,
                    onItemClick = { onNavigate() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Header — logo + barre rouge inférieure
// Fidèle au guide : logo à gauche, titre Calibri Bold bleu
// ─────────────────────────────────────────────
@Composable
fun CalanquesHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, spotColor = CalanquesTheme.BlackAlpha12)
            .background(CalanquesTheme.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {

            // Logo
            Image(
                painter            = painterResource(id = R.drawable.logo),
                contentDescription = "Parc National des Calanques",
                modifier           = Modifier.size(72.dp),
                contentScale       = ContentScale.Fit
            )

            Spacer(Modifier.width(14.dp))

            // Titre + sous-titre (Calibri Bold, couleur Blue du guide)
            Column {
                Text(
                    text          = "Calanques",
                    fontSize      = 26.sp,
                    fontWeight    = FontWeight.Bold,
                    fontFamily    = CalanquesTheme.CalibriFamily,
                    color         = CalanquesTheme.Blue,
                    letterSpacing = (-0.5).sp,
                    lineHeight    = 28.sp
                )
                Text(
                    text          = "Parc National",
                    fontSize      = 12.sp,
                    fontFamily    = CalanquesTheme.CalibriFamily,
                    fontWeight    = FontWeight.Normal,
                    color         = CalanquesTheme.Grey,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Ligne rouge en bas du header (identité Calanques Red)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(CalanquesTheme.Red)
                .align(Alignment.BottomStart)
        )
    }
}

// ─────────────────────────────────────────────
// Liste d'activités
// ─────────────────────────────────────────────
@Composable
fun ActivityTypeList(
    activities: List<ActivityType>,
    onItemClick: (ActivityType) -> Unit
) {
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(activities, key = { it.id }) { activity ->
            ActivityTypeCard(activity = activity, onClick = { onItemClick(activity) })
        }
    }
}

// ─────────────────────────────────────────────
// Carte activité
// Image pleine largeur · barre Rouge · label Calibri
// ─────────────────────────────────────────────
@Composable
fun ActivityTypeCard(
    activity: ActivityType,
    onClick: () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(
                elevation   = 3.dp,
                shape       = RoundedCornerShape(10.dp),
                spotColor   = CalanquesTheme.BlackAlpha12
            )
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = CalanquesTheme.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // ── Image ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(CalanquesTheme.LightGrey)
            ) {
                if (!activity.image_url.isNullOrBlank()) {
                    AsyncImage(
                        model              = "http://webngo.sio.bts:8001/${activity.image_url}",
                        contentDescription = activity.libelle,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop,
                        placeholder        = painterResource(id = android.R.drawable.ic_menu_gallery),
                        error              = painterResource(id = android.R.drawable.ic_menu_gallery)
                    )
                    // Dégradé bas pour lisibilité
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomStart)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0x55000000))
                                )
                            )
                    )
                } else {
                    Box(
                        modifier          = Modifier
                            .fillMaxSize()
                            .background(CalanquesTheme.LightGrey),
                        contentAlignment  = Alignment.Center
                    ) {
                        Icon(
                            painter            = painterResource(id = android.R.drawable.ic_menu_gallery),
                            contentDescription = null,
                            tint               = CalanquesTheme.Grey,
                            modifier           = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // ── Barre rouge (identité visuelle) ───────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(CalanquesTheme.Red)
            )

            // ── Ligne de label ─────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = activity.libelle,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = CalanquesTheme.CalibriFamily,
                    color      = CalanquesTheme.Black,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f)
                )
                // Chevron — couleur Blue du guide
                Text(
                    text       = "›",
                    fontSize   = 22.sp,
                    color      = CalanquesTheme.Blue,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CalanquesTheme.CalibriFamily
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// États — Chargement / Erreur / Vide
// ─────────────────────────────────────────────
@Composable
fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color       = CalanquesTheme.Blue,
                strokeWidth = 2.5.dp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text       = "Chargement…",
                fontSize   = 13.sp,
                fontFamily = CalanquesTheme.CalibriFamily,
                color      = CalanquesTheme.Grey
            )
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CalanquesTheme.RedLight)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Trait rouge en haut de la carte erreur
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .background(CalanquesTheme.Red, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text       = "Impossible de charger les activités",
            fontWeight = FontWeight.Bold,
            fontFamily = CalanquesTheme.CalibriFamily,
            color      = CalanquesTheme.Red,
            fontSize   = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = message,
            fontSize   = 12.sp,
            fontFamily = CalanquesTheme.CalibriFamily,
            color      = CalanquesTheme.Grey,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors  = ButtonDefaults.buttonColors(containerColor = CalanquesTheme.Red),
            shape   = RoundedCornerShape(6.dp)
        ) {
            Text(
                text       = "Réessayer",
                color      = Color.White,
                fontSize   = 13.sp,
                fontFamily = CalanquesTheme.CalibriFamily
            )
        }
    }
}

@Composable
fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter            = painterResource(id = android.R.drawable.ic_menu_info_details),
                contentDescription = null,
                tint               = CalanquesTheme.LightGrey,
                modifier           = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text       = "Aucune activité disponible",
                fontSize   = 14.sp,
                fontFamily = CalanquesTheme.CalibriFamily,
                color      = CalanquesTheme.Grey
            )
        }
    }
}