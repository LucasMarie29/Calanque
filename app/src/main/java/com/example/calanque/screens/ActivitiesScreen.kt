package com.example.calanque.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import com.example.calanque.R
import kotlinx.serialization.SerialName

// 1. Le modèle de données (Activity et non ActivityType)
@Serializable
data class Activity(
    val id: Int,
    @SerialName("nom")
    val nom: String = "Inconnu",
    val duree: String? = null,
    @SerialName("tarif")
    val prix: Double = 0.0,
    @SerialName("image_url")
    val image_url: String?
)

// 2. L'interface avec le bon nom
interface ActivitiesService {
    @GET("api/activities")
    suspend fun getActivities(): List<Activity>
}

// 3. Le ViewModel corrigé
class ActivitiesModel : ViewModel() {
    var activities by mutableStateOf<List<Activity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

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

// 4. L'écran renommé pour éviter le conflit "Conflicting overloads"
@Composable
fun MyActivitiesListScreen(viewModel: ActivitiesModel = viewModel()) {
    if (viewModel.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        viewModel.errorMessage?.let {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Erreur : $it", color = MaterialTheme.colorScheme.error)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {

            Text(
                text = "Activités Disponibles",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(viewModel.activities) { activity ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "http://webngo.sio.bts:8001/${activity.image_url}",
                                contentDescription = activity.nom,
                                modifier = Modifier.size(80.dp),
                                contentScale = ContentScale.Crop,
                                error = painterResource(android.R.drawable.ic_dialog_alert)
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