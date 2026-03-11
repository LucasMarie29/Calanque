package com.example.calanque.network

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

private const val BASE_URL =
    "http://webngo.sio.bts:8001/"

private val retrofit = Retrofit.Builder()
    .addConverterFactory(ScalarsConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()
interface PanierApiService {
    @GET("api/reservation")
    suspend fun getReservation(): String
}

object PanierApi {
    val retrofitService: PanierApiService by lazy {
        retrofit.create(PanierApiService::class.java)
    }
}




