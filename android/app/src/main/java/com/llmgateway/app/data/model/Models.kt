package com.llmgateway.app.data.model

import com.google.gson.annotations.SerializedName

// ===== Auth =====
data class AuthRequest(val username: String, val password: String, val email: String? = null)
data class ChangePasswordRequest(@SerializedName("old_password") val oldPassword: String, @SerializedName("new_password") val newPassword: String)
data class AuthResponse(val token: String, val user: User)
data class UserResponse(val user: User)
data class User(
    val id: Int,
    val username: String,
    val role: String,
    val balance: Double
)

// ===== Chat =====
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val temperature: Double? = null,
    @SerializedName("max_tokens") val maxTokens: Int? = null
)

data class ChatMessage(val role: String, val content: String)

data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice>,
    val usage: Usage? = null
) {
    data class Choice(val message: ChatMessage, @SerializedName("finish_reason") val finishReason: String?)
    data class Usage(
        @SerializedName("prompt_tokens") val promptTokens: Int,
        @SerializedName("completion_tokens") val completionTokens: Int,
        @SerializedName("total_tokens") val totalTokens: Int
    )
}

// ===== Models =====
data class ModelCatalogResponse(val models: List<ModelInfo>)
data class ModelInfo(
    val id: String,
    val name: String,
    val provider: String,
    val category: String,
    val context: String,
    val description: String
)

// ===== API Keys =====
data class KeysResponse(val keys: List<ApiKey>)
data class ApiKey(
    val id: Int,
    val name: String,
    val key: String,
    val status: String,
    @SerializedName("used_tokens") val usedTokens: Long,
    @SerializedName("usage_limit") val usageLimit: Long,
    @SerializedName("created_at") val createdAt: Long
)
data class CreateKeyRequest(val name: String, @SerializedName("usage_limit") val usageLimit: Long = 0)

// ===== Channels =====
data class ChannelsResponse(val channels: List<Channel>)
data class Channel(
    val id: Int,
    val name: String,
    val provider: String,
    @SerializedName("base_url") val baseUrl: String,
    val models: String,
    val weight: Int,
    val priority: Int,
    val status: String,
    @SerializedName("fail_count") val failCount: Int,
    @SerializedName("last_used_at") val lastUsedAt: Long
)
data class CreateChannelRequest(
    val name: String, val provider: String, @SerializedName("base_url") val baseUrl: String,
    @SerializedName("api_key") val apiKey: String, val models: List<String>,
    val weight: Int = 1, val priority: Int = 0
)
data class UpdateChannelRequest(
    val name: String? = null, @SerializedName("base_url") val baseUrl: String? = null,
    @SerializedName("api_key") val apiKey: String? = null, val models: List<String>? = null,
    val weight: Int? = null, val priority: Int? = null, val status: String? = null
)

// ===== Usage & Logs =====
data class UsageResponse(val stats: UsageStats, @SerializedName("by_model") val byModel: List<ModelUsage>)
data class UsageStats(
    val requests: Int,
    @SerializedName("prompt_tokens") val promptTokens: Long,
    @SerializedName("completion_tokens") val completionTokens: Long,
    @SerializedName("total_tokens") val totalTokens: Long,
    val cost: Double
)
data class ModelUsage(val model: String, val requests: Int, val tokens: Long, val cost: Double)

data class LogsResponse(val logs: List<RequestLog>, val total: Int)
data class RequestLog(
    val id: Int,
    val model: String,
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int,
    val cost: Double,
    val status: String,
    val error: String?,
    @SerializedName("latency_ms") val latencyMs: Int,
    @SerializedName("created_at") val createdAt: Long
)

// ===== Fine-tunes =====
data class FineTunesResponse(val tasks: List<FineTune>)
data class FineTune(
    val id: Int,
    val model: String,
    val suffix: String?,
    @SerializedName("training_file") val trainingFile: String,
    val status: String,
    @SerializedName("fine_tuned_model") val fineTunedModel: String?,
    val error: String?,
    @SerializedName("created_at") val createdAt: Long
)
data class CreateFineTuneRequest(
    val model: String, val suffix: String?, @SerializedName("training_file") val trainingFile: String,
    @SerializedName("channel_id") val channelId: Int? = null
)

data class GenericResponse(val ok: Boolean)
