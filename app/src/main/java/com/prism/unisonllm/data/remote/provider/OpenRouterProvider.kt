package com.prism.unisonllm.data.remote.provider

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * OpenRouter Provider implementation.
 * Provides unified access to multiple AI providers through OpenRouter's API.
 * Uses OpenAI-compatible format with additional headers for routing.
 */
class OpenRouterProvider(
    private val httpClient: HttpClient,
    private val config: ProviderConfig
) : ChatProvider {

    override val providerType: ProviderType = ProviderType.OPENROUTER

    private var streamJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val APP_NAME = "UnisonLLM"
        private const val APP_URL = "https://github.com/prism/unisonllm"
    }

    override suspend fun validateApiKey(): ProviderResult<Boolean> {
        return try {
            val response = httpClient.get("${config.getEffectiveBaseUrl()}/models") {
                header("Authorization", "Bearer ${config.apiKey}")
                header("HTTP-Referer", APP_URL)
                header("X-Title", APP_NAME)
            }
            if (response.status.isSuccess()) {
                ProviderResult.Success(true)
            } else {
                ProviderResult.Error("Invalid API key", response.status.value)
            }
        } catch (e: Exception) {
            ProviderResult.Error(e.message ?: "Connection failed")
        }
    }

    override suspend fun listModels(): ProviderResult<List<ModelInfo>> {
        return try {
            val response = httpClient.get("${config.getEffectiveBaseUrl()}/models") {
                header("Authorization", "Bearer ${config.apiKey}")
                header("HTTP-Referer", APP_URL)
                header("X-Title", APP_NAME)
            }
            if (response.status.isSuccess()) {
                val modelsResponse: OpenRouterModelsResponse = response.body()
                val models = modelsResponse.data.map { model ->
                    ModelInfo(
                        id = model.id,
                        name = model.name ?: model.id,
                        description = model.description,
                        contextWindow = model.contextLength,
                        provider = ProviderType.OPENROUTER
                    )
                }
                ProviderResult.Success(models)
            } else {
                ProviderResult.Error("Failed to fetch models", response.status.value)
            }
        } catch (e: Exception) {
            ProviderResult.Error(e.message ?: "Failed to fetch models")
        }
    }

    override suspend fun chatCompletion(request: ChatRequest): ProviderResult<ChatResponse> {
        return try {
            val openRouterRequest = buildOpenRouterRequest(request)
            val response = httpClient.post("${config.getEffectiveBaseUrl()}/chat/completions") {
                header("Authorization", "Bearer ${config.apiKey}")
                header("HTTP-Referer", APP_URL)
                header("X-Title", APP_NAME)
                contentType(ContentType.Application.Json)
                setBody(openRouterRequest)
            }

            if (response.status.isSuccess()) {
                val completionResponse: OpenRouterCompletionResponse = response.body()
                val choice = completionResponse.choices.firstOrNull()
                ProviderResult.Success(
                    ChatResponse(
                        content = choice?.message?.content ?: "",
                        finishReason = choice?.finishReason,
                        promptTokens = completionResponse.usage?.promptTokens ?: 0,
                        completionTokens = completionResponse.usage?.completionTokens ?: 0,
                        totalTokens = completionResponse.usage?.totalTokens ?: 0
                    )
                )
            } else {
                val errorBody: OpenRouterErrorResponse = response.body()
                ProviderResult.Error(errorBody.error?.message ?: "Request failed", response.status.value)
            }
        } catch (e: Exception) {
            ProviderResult.Error(e.message ?: "Request failed")
        }
    }

    override fun streamChatCompletion(request: ChatRequest): Flow<ProviderResult<ChatStreamChunk>> = flow {
        try {
            val openRouterRequest = buildOpenRouterRequest(request.copy(stream = true))
            httpClient.preparePost("${config.getEffectiveBaseUrl()}/chat/completions") {
                header("Authorization", "Bearer ${config.apiKey}")
                header("HTTP-Referer", APP_URL)
                header("X-Title", APP_NAME)
                contentType(ContentType.Application.Json)
                setBody(openRouterRequest)
            }.execute { response ->
                if (response.status.isSuccess()) {
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ") && line != "data: [DONE]") {
                            try {
                                val data = line.removePrefix("data: ")
                                val chunk: OpenRouterStreamChunk = json.decodeFromString(data)
                                val delta = chunk.choices.firstOrNull()?.delta?.content
                                if (delta != null) {
                                    emit(ProviderResult.Success(ChatStreamChunk(
                                        delta = delta,
                                        finishReason = chunk.choices.firstOrNull()?.finishReason
                                    )))
                                }
                            } catch (_: Exception) {
                                // Skip malformed chunks
                            }
                        }
                    }
                } else {
                    emit(ProviderResult.Error("Stream failed", response.status.value))
                }
            }
        } catch (e: Exception) {
            emit(ProviderResult.Error(e.message ?: "Stream failed"))
        }
    }

    override fun cancelStream() {
        streamJob?.cancel()
        streamJob = null
    }

    private fun buildOpenRouterRequest(request: ChatRequest): OpenRouterCompletionRequest {
        val messages = buildList {
            request.systemPrompt?.let { systemPrompt ->
                add(OpenRouterMessage("system", systemPrompt))
            }
            addAll(request.messages.map { msg ->
                OpenRouterMessage(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    },
                    content = msg.content
                )
            })
        }

        return OpenRouterCompletionRequest(
            model = request.model,
            messages = messages,
            temperature = request.temperature,
            topP = request.topP,
            maxTokens = request.maxTokens,
            stream = request.stream
        )
    }
}

// OpenRouter API Data Classes (OpenAI compatible format)
@Serializable
private data class OpenRouterCompletionRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Float = 0.7f,
    @SerialName("top_p") val topP: Float = 1.0f,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false
)

@Serializable
private data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
private data class OpenRouterCompletionResponse(
    val id: String? = null,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage? = null
)

@Serializable
private data class OpenRouterChoice(
    val index: Int? = null,
    val message: OpenRouterMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class OpenRouterUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)

@Serializable
private data class OpenRouterModelsResponse(
    val data: List<OpenRouterModel>
)

@Serializable
private data class OpenRouterModel(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    @SerialName("context_length") val contextLength: Int? = null
)

@Serializable
private data class OpenRouterStreamChunk(
    val choices: List<OpenRouterStreamChoice>
)

@Serializable
private data class OpenRouterStreamChoice(
    val delta: OpenRouterStreamDelta,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class OpenRouterStreamDelta(
    val content: String? = null
)

@Serializable
private data class OpenRouterErrorResponse(
    val error: OpenRouterError? = null
)

@Serializable
private data class OpenRouterError(
    val message: String? = null,
    val code: Int? = null
)
