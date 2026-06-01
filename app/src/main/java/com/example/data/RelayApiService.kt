package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class RelayRequest(
    val subject: String,
    val question: String,
    val system_instruction: String,
    val image_b64: String? = null,
    val video_b64: String? = null,
    val audio_b64: String? = null
)

@JsonClass(generateAdapter = true)
data class RelayResponse(
    val success: Boolean,
    val answer: String?,
    val provider_used: String,
    val error_msg: String?
)

interface RelayApiService {
    @POST("api/v1/homework/solve")
    suspend fun solveHomework(
        @Header("X-Relay-Key") apiKey: String,
        @Body request: RelayRequest
    ): RelayResponse
}

object RetrofitClient {
    // Falls back to the hardcoded URL if BuildConfig is missing it for some reason
    private val BASE_URL = BuildConfig.RELAY_BASE_URL

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS) // Increased to 90s for fallback chains/video processing
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    val service: RelayApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(RelayApiService::class.java)
    }
}
