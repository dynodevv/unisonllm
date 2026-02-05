package com.prism.unisonllm.data.remote.provider

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Represents different LLM provider types supported by UnisonLLM.
 */
@Serializable
enum class ProviderType {
    OPENAI,
    ANTHROPIC,
    GOOGLE,
    OPENROUTER,
    CUSTOM
}

/**
 * Configuration for a chat provider.
 *
 * @property type The type of provider
 * @property apiKey The API key for authentication (stored encrypted)
 * @property baseUrl Custom base URL (used for Custom providers)
 * @property organizationId Optional organization ID (OpenAI specific)
 * @property projectId Optional project ID (Google specific)
 */
@Serializable
data class ProviderConfig(
    val type: ProviderType,
    val apiKey: String = "",
    val baseUrl: String = "",
    val organizationId: String? = null,
    val projectId: String? = null
) {
    /**
     * Returns the effective base URL for the provider.
     */
    fun getEffectiveBaseUrl(): String = when (type) {
        ProviderType.OPENAI -> "https://api.openai.com/v1"
        ProviderType.ANTHROPIC -> "https://api.anthropic.com/v1"
        ProviderType.GOOGLE -> "https://generativelanguage.googleapis.com/v1beta"
        ProviderType.OPENROUTER -> "https://openrouter.ai/api/v1"
        ProviderType.CUSTOM -> baseUrl.trimEnd('/')
    }
}

/**
 * Represents a single message in a chat conversation.
 */
@Serializable
data class ChatMessage(
    val role: MessageRole,
    val content: String
)

/**
 * Roles a message can have in a conversation.
 */
@Serializable
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Request parameters for chat completion.
 */
@Serializable
data class ChatRequest(
    val messages: List<ChatMessage>,
    val model: String,
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val maxTokens: Int? = null,
    val stream: Boolean = false,
    val systemPrompt: String? = null
)

/**
 * Response from a chat completion.
 */
@Serializable
data class ChatResponse(
    val content: String,
    val finishReason: String? = null,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

/**
 * Represents a streaming chunk from a chat completion.
 */
@Serializable
data class ChatStreamChunk(
    val delta: String,
    val finishReason: String? = null
)

/**
 * Represents an available model from a provider.
 */
@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    val contextWindow: Int? = null,
    val provider: ProviderType
)

/**
 * Result wrapper for API responses.
 */
sealed class ProviderResult<out T> {
    data class Success<T>(val data: T) : ProviderResult<T>()
    data class Error(val message: String, val code: Int? = null) : ProviderResult<Nothing>()
}

/**
 * Interface defining the contract for all chat providers.
 * Each provider (OpenAI, Anthropic, Google, OpenRouter, Custom) implements this interface.
 */
interface ChatProvider {
    /**
     * The type of this provider.
     */
    val providerType: ProviderType

    /**
     * Validates that the provider configuration is correct and the API key is valid.
     *
     * @return True if validation succeeds, false otherwise
     */
    suspend fun validateApiKey(): ProviderResult<Boolean>

    /**
     * Fetches the list of available models from the provider.
     *
     * @return List of available models
     */
    suspend fun listModels(): ProviderResult<List<ModelInfo>>

    /**
     * Sends a chat completion request and returns the full response.
     *
     * @param request The chat completion request parameters
     * @return The complete response from the model
     */
    suspend fun chatCompletion(request: ChatRequest): ProviderResult<ChatResponse>

    /**
     * Sends a streaming chat completion request.
     *
     * @param request The chat completion request parameters (stream should be true)
     * @return A Flow emitting response chunks as they arrive
     */
    fun streamChatCompletion(request: ChatRequest): Flow<ProviderResult<ChatStreamChunk>>

    /**
     * Cancels any ongoing streaming request.
     */
    fun cancelStream()
}
