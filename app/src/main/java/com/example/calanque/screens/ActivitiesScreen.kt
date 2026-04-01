package com.example.calanque.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            maxDuree?.let { max -> result = result.filter {
                (it.duree?.substringBefore(":")?.toIntOrNull() ?: Int.MAX_VALUE) <= max
            }}

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
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()

    private val service = retrofit.create(ActivitiesService::class.java)

    init { fetchData() }

    private fun fetchData() {
        viewModelScope.launch {
            isLoading = true
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
        topBar = {
            TopAppBar(
                title = { Text("Activités Disponibles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barre recherche + bouton filtres
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Rechercher une activité...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    }
                )
                TextButton(onClick = { showFilterSheet = true }) {
                    Text(if (activeFilterCount > 0) "Filtres ($activeFilterCount)" else "Filtres")
                }
            }

            // Chips de tri rapide
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(SortOption.entries.drop(1)) { option ->
                    FilterChip(
                        selected = viewModel.sortOption == option,
                        onClick = {
                            viewModel.sortOption =
                                if (viewModel.sortOption == option) SortOption.NONE else option
                        },
                        label = { Text(option.label) }
                    )
                }
            }

            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.errorMessage != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Erreur : ${viewModel.errorMessage}", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(viewModel.filteredActivities) { activity ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            onClick = { onActivityClick(activity.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = "http://webngo.sio.bts:8001/${activity.image_url}",
                                    contentDescription = activity.nom,
                                    modifier = Modifier.size(80.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.padding(start = 16.dp)) {
                                    Text(
                                        text = activity.nom,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        val dureeAffichee = activity.duree?.substringBeforeLast(":") ?: "N/A"
                                        Text(text = "Durée: ${dureeAffichee}h")
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(text = "Prix: ${activity.prix}€")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet filtres
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtres", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { viewModel.resetFilters() }) { Text("Réinitialiser") }
                }

                HorizontalDivider()

                Text("Prix (€)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.minPrix,
                        onValueChange = { viewModel.minPrix = it },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.maxPrix,
                        onValueChange = { viewModel.maxPrix = it },
                        label = { Text("Max") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Text(
                    "Durée max : ${viewModel.maxDuree?.let { "$it h" } ?: "Toutes"}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = viewModel.maxDuree?.toFloat() ?: 24f,
                    onValueChange = { viewModel.maxDuree = it.toInt().takeIf { v -> v < 24 } },
                    valueRange = 1f..24f,
                    steps = 22
                )

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voir ${viewModel.filteredActivities.size} résultat(s)")
                }
            }
        }
    }
}