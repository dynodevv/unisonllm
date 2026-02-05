package com.prism.unisonllm.data.remote.provider

import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
 * Anthropic Claude Provider implementation.
 * Supports Claude 3 models via the Messages API.
 */
class AnthropicProvider(
    private val httpClient: HttpClient,
    private val config: ProviderConfig
) : ChatProvider {

    override val providerType: ProviderType = ProviderType.ANTHROPIC

    private var streamJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val ANTHROPIC_VERSION = "2023-06-01"
    }

    override suspend fun validateApiKey(): ProviderResult<Boolean> {
        return try {
            // Anthropic doesn't have a models endpoint, so we make a simple test request
            val response = httpClient.post("${config.getEffectiveBaseUrl()}/messages") {
                header("x-api-key", config.apiKey)
                header("anthropic-version", ANTHROPIC_VERSION)
                contentType(ContentType.Application.Json)
                setBody(AnthropicRequest(
                    model = "claude-3-haiku-20240307",
                    messages = listOf(AnthropicMessage("user", "test")),
                    maxTokens = 1
                ))
            }
            if (response.status.isSuccess() || response.status.value == 400) {
                // 400 might mean bad request but valid key
                ProviderResult.Success(true)
            } else if (response.status.value == 401) {
                ProviderResult.Error("Invalid API key", 401)
            } else {
                ProviderResult.Success(true)
            }
        } catch (e: Exception) {
            ProviderResult.Error(e.message ?: "Connection failed")
        }
    }

    override suspend fun listModels(): ProviderResult<List<ModelInfo>> {
        // Anthropic doesn't have a public models API, return curated list
        return ProviderResult.Success(
            listOf(
                ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Best balance of intelligence and speed", 200000, ProviderType.ANTHROPIC),
                ModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "Fastest Claude model", 200000, ProviderType.ANTHROPIC),
                ModelInfo("claude-3-opus-20240229", "Claude 3 Opus", "Most capable Claude model", 200000, ProviderType.ANTHROPIC),
                ModelInfo("claude-3-sonnet-20240229", "Claude 3 Sonnet", "Balanced performance", 200000, ProviderType.ANTHROPIC),
                ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku", "Fast and affordable", 200000, ProviderType.ANTHROPIC)
            )
        )
    }

    override suspend fun chatCompletion(request: ChatRequest): ProviderResult<ChatResponse> {
        return try {
            val anthropicRequest = buildAnthropicRequest(request)
            val response = httpClient.post("${config.getEffectiveBaseUrl()}/messages") {
                header("x-api-key", config.apiKey)
                header("anthropic-version", ANTHROPIC_VERSION)
                contentType(ContentType.Application.Json)
                setBody(anthropicRequest)
            }

            if (response.status.isSuccess()) {
                val completionResponse: AnthropicResponse = response.body()
                val content = completionResponse.content
                    .filterIsInstance<AnthropicContentBlock.Text>()
                    .joinToString("") { it.text }

                ProviderResult.Success(
                    ChatResponse(
                        content = content,
                        finishReason = completionResponse.stopReason,
                        promptTokens = completionResponse.usage.inputTokens,
                        completionTokens = completionResponse.usage.outputTokens,
                        totalTokens = completionResponse.usage.inputTokens + completionResponse.usage.outputTokens
                    )
                )
            } else {
                val errorBody: AnthropicErrorResponse = response.body()
                ProviderResult.Error(errorBody.error.message, response.status.value)
            }
        } catch (e: Exception) {
            ProviderResult.Error(e.message ?: "Request failed")
        }
    }

    override fun streamChatCompletion(request: ChatRequest): Flow<ProviderResult<ChatStreamChunk>> = flow {
        try {
            val anthropicRequest = buildAnthropicRequest(request, stream = true)
            httpClient.preparePost("${config.getEffectiveBaseUrl()}/messages") {
                header("x-api-key", config.apiKey)
                header("anthropic-version", ANTHROPIC_VERSION)
                contentType(ContentType.Application.Json)
                setBody(anthropicRequest)
            }.execute { response ->
                if (response.status.isSuccess()) {
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ")) {
                            try {
                                val data = line.removePrefix("data: ")
                                val event: AnthropicStreamEvent = json.decodeFromString(data)
                                when (event.type) {
                                    "content_block_delta" -> {
                                        val delta = event.delta?.text
                                        if (delta != null) {
                                            emit(ProviderResult.Success(ChatStreamChunk(delta = delta)))
                                        }
                                    }
                                    "message_stop" -> {
                                        emit(ProviderResult.Success(ChatStreamChunk(delta = "", finishReason = "end_turn")))
                                    }
                                }
                            } catch (_: Exception) {
                                // Skip malformed events
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

    private fun buildAnthropicRequest(request: ChatRequest, stream: Boolean = false): AnthropicRequest {
        val messages = request.messages.map { msg ->
            AnthropicMessage(
                role = when (msg.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "user" // System messages become user messages with context
                },
                content = msg.content
            )
        }

        return AnthropicRequest(
            model = request.model,
            messages = messages,
            maxTokens = request.maxTokens ?: 4096,
            temperature = request.temperature,
            topP = request.topP,
            system = request.systemPrompt,
            stream = stream
        )
    }
}

// Anthropic API Data Classes
@Serializable
private data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 4096,
    val temperature: Float? = null,
    @SerialName("top_p") val topP: Float? = null,
    val system: String? = null,
    val stream: Boolean = false
)

@Serializable
private data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
private data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<AnthropicContentBlock>,
    @SerialName("stop_reason") val stopReason: String? = null,
    val usage: AnthropicUsage
)

@Serializable
sealed class AnthropicContentBlock {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : AnthropicContentBlock()
}

@Serializable
private data class AnthropicUsage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)

@Serializable
private data class AnthropicStreamEvent(
    val type: String,
    val delta: AnthropicDelta? = null
)

@Serializable
private data class AnthropicDelta(
    val text: String? = null
)

@Serializable
private data class AnthropicErrorResponse(
    val type: String,
    val error: AnthropicError
)

@Serializable
private data class AnthropicError(
    val type: String,
    val message: String
)
