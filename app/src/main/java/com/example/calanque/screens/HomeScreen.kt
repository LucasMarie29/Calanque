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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import com.example.calanque.R
import androidx.compose.foundation.Image

// ─────────────────────────────────────────────
// Design Tokens — Calanques Style Guide
// ─────────────────────────────────────────────
object CalanquesTheme {
    // Main Colors (from StyleGuide)
    val Red = Color(0xFFE51A2E)
    val Black = Color(0xFF000000)
    val LightGrey = Color(0xFFBBBBBB)
    val Grey = Color(0xFF555555)
    val Blue = Color(0xFF4472C4)
    val LightGreen = Color(0xFFA8D08D)

    // Derived / UI
    val White = Color(0xFFFFFFFF)
    val Background = Color(0xFFF5F5F5)
    val CardBg = Color(0xFFFFFFFF)
    val DividerColor = Color(0xFFEEEEEE)
    val NavBarBg = Color(0xFFFFFFFF)
    val HeaderBg = Color(0xFF4472C4) // Blue header bar
}

// ─────────────────────────────────────────────
// Data / Network layer
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
    var activities by mutableStateOf<List<ActivityType>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://webngo.sio.bts:8001/")
        .addConverterFactory(
            Json { ignoreUnknownKeys = true }
                .asConverterFactory("application/json".toMediaType())
        )
        .build()

    private val service = retrofit.create(MyApiService::class.java)

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            isLoading = true
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
    onActivityTypeClick: (ActivityType) -> Unit = {}
) {
    Scaffold(
        containerColor = CalanquesTheme.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Top header with logo ──────────────────────
            CalanquesHeader()

            // ── Section label ─────────────────────────────
            Text(
                text = "TYPES D'ACTIVITÉS",
                fontSize = 11.sp,
                fontWeight = FontWeight.W600,
                color = CalanquesTheme.Grey,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // ── Content area ──────────────────────────────
            when {
                viewModel.isLoading -> LoadingIndicator()
                viewModel.errorMessage != null -> ErrorCard(
                    message = viewModel.errorMessage!!,
                    onRetry = { viewModel.fetchData() }
                )

                viewModel.activities.isEmpty() -> EmptyState()
                else -> ActivityTypeList(
                    activities = viewModel.activities,
                    onItemClick = onActivityTypeClick
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Header — Blue bar + logo
// ─────────────────────────────────────────────
@Composable
fun CalanquesHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CalanquesTheme.White)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth()
        ) {
            // ── Logo plus grand ───────────────────────────
            Image(
                painter            = painterResource(id = R.drawable.logo),
                contentDescription = "Parc National des Calanques",
                modifier           = Modifier.size(80.dp),
                contentScale       = ContentScale.Fit
            )


            // ── Titre + sous-titre ────────────────────────
            Column {
                Text(
                    text          = "Calanques",
                    fontSize      = 28.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = CalanquesTheme.Blue,
                    letterSpacing = (-0.5).sp,
                    lineHeight    = 30.sp
                )
                Text(
                    text          = "Parc National",
                    fontSize      = 13.sp,
                    color         = CalanquesTheme.Grey,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))

        // ── Ligne rouge en bas ────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(CalanquesTheme.Red)
                .align(Alignment.BottomStart)
        )
    }
}

// ─────────────────────────────────────────────
// Activity type list
// ─────────────────────────────────────────────
@Composable
fun ActivityTypeList(
    activities: List<ActivityType>,
    onItemClick: (ActivityType) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(activities, key = { it.id }) { activity ->
            ActivityTypeCard(activity = activity, onClick = { onItemClick(activity) })
        }
    }
}

// ─────────────────────────────────────────────
// Single activity-type card
// ─────────────────────────────────────────────
@Composable
fun ActivityTypeCard(
    activity: ActivityType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CalanquesTheme.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Activity image ────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(CalanquesTheme.LightGrey)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            ) {
                if (!activity.image_url.isNullOrBlank()) {
                    AsyncImage(
                        model = "http://webngo.sio.bts:8001/${activity.image_url}",
                        contentDescription = activity.libelle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                        error = painterResource(id = android.R.drawable.ic_menu_gallery)
                    )
                } else {
                    // Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CalanquesTheme.LightGrey),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                            contentDescription = null,
                            tint = CalanquesTheme.Grey,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // ── Red accent bar ────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(CalanquesTheme.Red)
            )

            // ── Label row ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.libelle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W600,
                    color = CalanquesTheme.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Chevron →
                Text(
                    text = "›",
                    fontSize = 22.sp,
                    color = CalanquesTheme.Blue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// ─────────────────────────────────────────────
// States — Loading / Error / Empty
// ─────────────────────────────────────────────
@Composable
fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = CalanquesTheme.Blue, strokeWidth = 2.dp)
            Spacer(Modifier.height(12.dp))
            Text("Chargement…", fontSize = 13.sp, color = CalanquesTheme.Grey)
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFEBEE))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Impossible de charger les activités",
            fontWeight = FontWeight.W600,
            color = CalanquesTheme.Red,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = CalanquesTheme.Grey,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = CalanquesTheme.Red),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("Réessayer", color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Aucune activité disponible",
            fontSize = 14.sp,
            color = CalanquesTheme.Grey
        )
    }
}