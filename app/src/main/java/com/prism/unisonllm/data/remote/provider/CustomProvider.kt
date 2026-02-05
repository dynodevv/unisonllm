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
 * Custom Provider implementation.
 * Supports user-defined endpoints that follow the OpenAI-compatible API schema.
 * This allows connection to local LLMs (like Ollama, LM Studio) or other compatible services.
 */
class CustomProvider(
    private val httpClient: HttpClient,
    private val config: ProviderConfig
) : ChatProvider {

    override val providerType: ProviderType = ProviderType.CUSTOM

    private var streamJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun validateApiKey(): ProviderResult<Boolean> {
        return try {
            // Try to fetch models to validate connectivity
            val modelsEndpoint = "${config.getEffectiveBaseUrl()}/models"
            val response = httpClient.get(modelsEndpoint) {
                if (config.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${config.apiKey}")
                }
            }
            if (response.status.isSuccess()) {
                ProviderResult.Success(true)
            } else if (response.status.value == 401) {
                ProviderResult.Error("Invalid API key", 401)
            } else {
                // Some custom endpoints might not have a models endpoint
                // Consider it valid if we can connect
                ProviderResult.Success(true)
            }
        } catch (e: Exception) {
            ProviderResult.Error(e.message ?: "Connection failed")
        }
    }

    override suspend fun listModels(): ProviderResult<List<ModelInfo>> {
        return try {
            val response = httpClient.get("${config.getEffectiveBaseUrl()}/models") {
                if (config.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${config.apiKey}")
                }
            }
            if (response.status.isSuccess()) {
                val modelsResponse: CustomModelsResponse = response.body()
                val models = modelsResponse.data.map { model ->
                    ModelInfo(
                        id = model.id,
                        name = model.id,
                        description = null,
                        contextWindow = null,
                        provider = ProviderType.CUSTOM
                    )
                }
                ProviderResult.Success(models)
            } else {
                // Return empty list if models endpoint not available
                ProviderResult.Success(emptyList())
            }
        } catch (e: Exception) {
            // Custom endpoints might not have a models endpoint
            ProviderResult.Success(emptyList())
        }
    }

    override suspend fun chatCompletion(request: ChatRequest): ProviderResult<ChatResponse> {
        return try {
            val customRequest = buildCustomRequest(request)
            val response = httpClient.post("${config.getEffectiveBaseUrl()}/chat/completions") {
                if (config.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${config.apiKey}")
                }
                contentType(ContentType.Application.Json)
                setBody(customRequest)
            }

            if (response.status.isSuccess()) {
                val completionResponse: CustomCompletionResponse = response.body()
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
                try {
                    val errorBody: CustomErrorResponse = response.body()
                    ProviderResult.Error(errorBody.error?.message ?: "Request failed", response.status.value)
                } catch (_: Exception) {
                    ProviderResult.Error("Request failed with status ${response.status.value}", response.status.value)
                }
            }
        } catch (e: Exception) {
            ProviderResult.Error(e.message ?: "Request failed")
        }
    }

    override fun streamChatCompletion(request: ChatRequest): Flow<ProviderResult<ChatStreamChunk>> = flow {
        try {
            val customRequest = buildCustomRequest(request.copy(stream = true))
            httpClient.preparePost("${config.getEffectiveBaseUrl()}/chat/completions") {
                if (config.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${config.apiKey}")
                }
                contentType(ContentType.Application.Json)
                setBody(customRequest)
            }.execute { response ->
                if (response.status.isSuccess()) {
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ") && line != "data: [DONE]") {
                            try {
                                val data = line.removePrefix("data: ")
                                val chunk: CustomStreamChunk = json.decodeFromString(data)
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

    private fun buildCustomRequest(request: ChatRequest): CustomCompletionRequest {
        val messages = buildList {
            request.systemPrompt?.let { systemPrompt ->
                add(CustomMessage("system", systemPrompt))
            }
            addAll(request.messages.map { msg ->
                CustomMessage(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    },
                    content = msg.content
                )
            })
        }

        return CustomCompletionRequest(
            model = request.model,
            messages = messages,
            temperature = request.temperature,
            topP = request.topP,
            maxTokens = request.maxTokens,
            stream = request.stream
        )
    }
}

// Custom API Data Classes (OpenAI compatible format)
@Serializable
private data class CustomCompletionRequest(
    val model: String,
    val messages: List<CustomMessage>,
    val temperature: Float = 0.7f,
    @SerialName("top_p") val topP: Float = 1.0f,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false
)

@Serializable
private data class CustomMessage(
    val role: String,
    val content: String
)

@Serializable
private data class CustomCompletionResponse(
    val id: String? = null,
    val choices: List<CustomChoice>,
    val usage: CustomUsage? = null
)

@Serializable
private data class CustomChoice(
    val index: Int? = null,
    val message: CustomMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class CustomUsage(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null
)

@Serializable
private data class CustomModelsResponse(
    val data: List<CustomModel> = emptyList()
)

@Serializable
private data class CustomModel(
    val id: String
)

@Serializable
private data class CustomStreamChunk(
    val choices: List<CustomStreamChoice>
)

@Serializable
private data class CustomStreamChoice(
    val delta: CustomStreamDelta,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class CustomStreamDelta(
    val content: String? = null
)

@Serializable
private data class CustomErrorResponse(
    val error: CustomError? = null
)

@Serializable
private data class CustomError(
    val message: String? = null,
    val type: String? = null
)
