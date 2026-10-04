package com.llmgateway.app.data.api

import com.llmgateway.app.data.model.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @GET("api/auth/me")
    suspend fun me(): UserResponse

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): GenericResponse

    // Chat (OpenAI compatible)
    @POST("v1/chat/completions")
    suspend fun chatCompletion(@Body body: ChatRequest): ChatResponse

    @Streaming
    @POST("v1/chat/completions")
    suspend fun chatCompletionStream(@Body body: ChatRequest): okhttp3.ResponseBody

    // Models
    @GET("api/catalog")
    suspend fun modelCatalog(@Query("category") category: String? = null, @Query("q") q: String? = null): ModelCatalogResponse

    // API Keys
    @GET("api/")
    suspend fun listKeys(): KeysResponse

    @POST("api/")
    suspend fun createKey(@Body body: CreateKeyRequest): ApiKey

    @DELETE("api/{id}")
    suspend fun deleteKey(@Path("id") id: Int): GenericResponse

    // Channels (admin)
    @GET("api/channels")
    suspend fun listChannels(): ChannelsResponse

    @POST("api/channels")
    suspend fun createChannel(@Body body: CreateChannelRequest): Channel

    @PUT("api/channels/{id}")
    suspend fun updateChannel(@Path("id") id: Int, @Body body: UpdateChannelRequest): GenericResponse

    @DELETE("api/channels/{id}")
    suspend fun deleteChannel(@Path("id") id: Int): GenericResponse

    // Usage & Logs
    @GET("api/usage")
    suspend fun usage(@Query("range") range: String = "today"): UsageResponse

    @GET("api/logs")
    suspend fun logs(@Query("limit") limit: Int = 50, @Query("offset") offset: Int = 0): LogsResponse

    // Fine-tunes
    @GET("api/fine-tunes")
    suspend fun listFineTunes(): FineTunesResponse

    @POST("api/fine-tunes")
    suspend fun createFineTune(@Body body: CreateFineTuneRequest): FineTune
}
