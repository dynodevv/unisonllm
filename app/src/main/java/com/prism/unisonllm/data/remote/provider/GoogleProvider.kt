package com.prism.unisonllm.data.remote.provider

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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
 * Google Gemini Provider implementation.
 * Supports Gemini Pro and other Google AI models via the Generative AI API.
 */
class GoogleProvider(
    private val httpClient: HttpClient,
    private val config: ProviderConfig
) : ChatProvider {

    override val providerType: ProviderType = ProviderType.GOOGLE

    private var streamJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun validateApiKey(): ProviderResult<Boolean> {
        return try {
            val response = httpClient.get("${config.getEffectiveBaseUrl()}/models") {
                parameter("key", config.apiKey)
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
                parameter("key", config.apiKey)
            }
            if (response.status.isSuccess()) {
                val modelsResponse: GoogleModelsResponse = response.body()
                val chatModels = modelsResponse.models
                    .filter { it.supportedGenerationMethods?.contains("generateContent") == true }
                    .map { model ->
                        ModelInfo(
                            id = model.name.removePrefix("models/"),
                            name = model.displayName ?: model.name,
                            description = model.description,
                            contextWindow = model.inputTokenLimit,
                            provider = ProviderType.GOOGLE
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
            val geminiRequest = buildGeminiRequest(request)
            val modelId = request.model
            val response = httpClient.post("${config.getEffectiveBaseUrl()}/models/$modelId:generateContent") {
                parameter("key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(geminiRequest)
            }

            if (response.status.isSuccess()) {
                val geminiResponse: GeminiResponse = response.body()
                val content = geminiResponse.candidates?.firstOrNull()?.content?.parts
                    ?.filterIsInstance<GeminiPart.Text>()
                    ?.joinToString("") { it.text } ?: ""

                ProviderResult.Success(
                    ChatResponse(
                        content = content,
                        finishReason = geminiResponse.candidates?.firstOrNull()?.finishReason,
                        promptTokens = geminiResponse.usageMetadata?.promptTokenCount ?: 0,
                        completionTokens = geminiResponse.usageMetadata?.candidatesTokenCount ?: 0,
                        totalTokens = geminiResponse.usageMetadata?.totalTokenCount ?: 0
                    )
                )
            } else {
                val errorBody: GeminiErrorResponse = response.body()
                ProviderResult.Error(errorBody.error.message, response.status.value)
            }
        } catch (e: Exception) {
            ProviderResult.Error(e.message ?: "Request failed")
        }
    }

    override fun streamChatCompletion(request: ChatRequest): Flow<ProviderResult<ChatStreamChunk>> = flow {
        try {
            val geminiRequest = buildGeminiRequest(request)
            val modelId = request.model
            httpClient.preparePost("${config.getEffectiveBaseUrl()}/models/$modelId:streamGenerateContent") {
                parameter("key", config.apiKey)
                parameter("alt", "sse")
                contentType(ContentType.Application.Json)
                setBody(geminiRequest)
            }.execute { response ->
                if (response.status.isSuccess()) {
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ")) {
                            try {
                                val data = line.removePrefix("data: ")
                                val chunk: GeminiResponse = json.decodeFromString(data)
                                val text = chunk.candidates?.firstOrNull()?.content?.parts
                                    ?.filterIsInstance<GeminiPart.Text>()
                                    ?.joinToString("") { it.text }

                                if (text != null) {
                                    emit(ProviderResult.Success(ChatStreamChunk(
                                        delta = text,
                                        finishReason = chunk.candidates?.firstOrNull()?.finishReason
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

    private fun buildGeminiRequest(request: ChatRequest): GeminiRequest {
        val contents = buildList {
            request.messages.forEach { msg ->
                add(GeminiContent(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "model"
                        MessageRole.SYSTEM -> "user"
                    },
                    parts = listOf(GeminiPart.Text(msg.content))
                ))
            }
        }

        val systemInstruction = request.systemPrompt?.let {
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart.Text(it))
            )
        }

        return GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(
                temperature = request.temperature,
                topP = request.topP,
                maxOutputTokens = request.maxTokens
            )
        )
    }
}

// Google Gemini API Data Classes
@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
private data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

@Serializable
sealed class GeminiPart {
    @Serializable
    data class Text(val text: String) : GeminiPart()
}

@Serializable
private data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxOutputTokens: Int? = null
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val usageMetadata: GeminiUsageMetadata? = null
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@Serializable
private data class GeminiUsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null
)

@Serializable
private data class GoogleModelsResponse(
    val models: List<GoogleModel>
)

@Serializable
private data class GoogleModel(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val inputTokenLimit: Int? = null,
    val supportedGenerationMethods: List<String>? = null
)

@Serializable
private data class GeminiErrorResponse(
    val error: GeminiError
)

@Serializable
private data class GeminiError(
    val code: Int,
    val message: String
)
