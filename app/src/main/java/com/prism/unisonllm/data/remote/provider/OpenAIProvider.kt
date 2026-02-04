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
 * OpenAI Chat Provider implementation.
 * Supports GPT-4, GPT-3.5-turbo and other OpenAI models via the /v1/chat/completions API.
 */
class OpenAIProvider(
    private val httpClient: HttpClient,
    private val config: ProviderConfig
) : ChatProvider {

    override val providerType: ProviderType = ProviderType.OPENAI

    private var streamJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun validateApiKey(): ProviderResult<Boolean> {
        return try {
            val response = httpClient.get("${config.getEffectiveBaseUrl()}/models") {
                header("Authorization", "Bearer ${config.apiKey}")
                config.organizationId?.let { header("OpenAI-Organization", it) }
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
                config.organizationId?.let { header("OpenAI-Organization", it) }
            }
            if (response.status.isSuccess()) {
                val modelsResponse: OpenAIModelsResponse = response.body()
                val chatModels = modelsResponse.data
                    .filter { it.id.contains("gpt") }
                    .map { model ->
                        ModelInfo(
                            id = model.id,
                            name = model.id.replaceFirstChar { it.uppercase() },
                            description = null,
                            contextWindow = null,
                            provider = ProviderType.OPENAI
                        )
                    }
                ProviderResult.Success(chatModels)
            } else {
                ProviderResult.Error("Failed to fetch models", response.status.value)
            }
        } catch (e: Exception) {
            ProviderResult.Error(e.message ?: "Failed to fetch models")
        }
    }

    override suspend fun chatCompletion(request: ChatRequest): ProviderResult<ChatResponse> {
        return try {
            val openAIRequest = buildOpenAIRequest(request)
            val response = httpClient.post("${config.getEffectiveBaseUrl()}/chat/completions") {
                header("Authorization", "Bearer ${config.apiKey}")
                config.organizationId?.let { header("OpenAI-Organization", it) }
                contentType(ContentType.Application.Json)
                setBody(openAIRequest)
            }

            if (response.status.isSuccess()) {
                val completionResponse: OpenAICompletionResponse = response.body()
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
                val errorBody: OpenAIErrorResponse = response.body()
                ProviderResult.Error(errorBody.error.message, response.status.value)
            }
        } catch (e: Exception) {
            ProviderResult.Error(e.message ?: "Request failed")
        }
    }

    override fun streamChatCompletion(request: ChatRequest): Flow<ProviderResult<ChatStreamChunk>> = flow {
        try {
            val openAIRequest = buildOpenAIRequest(request.copy(stream = true))
            httpClient.preparePost("${config.getEffectiveBaseUrl()}/chat/completions") {
                header("Authorization", "Bearer ${config.apiKey}")
                config.organizationId?.let { header("OpenAI-Organization", it) }
                contentType(ContentType.Application.Json)
                setBody(openAIRequest)
            }.execute { response ->
                if (response.status.isSuccess()) {
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ") && line != "data: [DONE]") {
                            try {
                                val data = line.removePrefix("data: ")
                                val chunk: OpenAIStreamChunk = json.decodeFromString(data)
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

    private fun buildOpenAIRequest(request: ChatRequest): OpenAICompletionRequest {
        val messages = buildList {
            request.systemPrompt?.let { systemPrompt ->
                add(OpenAIMessage("system", systemPrompt))
            }
            addAll(request.messages.map { msg ->
                OpenAIMessage(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    },
                    content = msg.content
                )
            })
        }

        return OpenAICompletionRequest(
            model = request.model,
            messages = messages,
            temperature = request.temperature,
            topP = request.topP,
            maxTokens = request.maxTokens,
            stream = request.stream
        )
    }
}

// OpenAI API Data Classes
@Serializable
private data class OpenAICompletionRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Float = 0.7f,
    @SerialName("top_p") val topP: Float = 1.0f,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false
)

@Serializable
private data class OpenAIMessage(
    val role: String,
    val content: String
)

@Serializable
private data class OpenAICompletionResponse(
    val id: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null
)

@Serializable
private data class OpenAIChoice(
    val index: Int,
    val message: OpenAIMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class OpenAIUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)

@Serializable
private data class OpenAIModelsResponse(
    val data: List<OpenAIModel>
)

@Serializable
private data class OpenAIModel(
    val id: String
)

@Serializable
private data class OpenAIStreamChunk(
    val choices: List<OpenAIStreamChoice>
)

@Serializable
private data class OpenAIStreamChoice(
    val delta: OpenAIStreamDelta,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class OpenAIStreamDelta(
    val content: String? = null
)

@Serializable
private data class OpenAIErrorResponse(
    val error: OpenAIError
)

@Serializable
private data class OpenAIError(
    val message: String,
    val type: String? = null
)
