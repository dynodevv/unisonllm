package com.prism.unisonllm.data.remote.provider

import io.ktor.client.HttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory class for creating ChatProvider instances based on provider type.
 * Uses Hilt for dependency injection of the HttpClient.
 */
@Singleton
class ChatProviderFactory @Inject constructor(
    private val httpClient: HttpClient
) {
    /**
     * Creates a ChatProvider instance for the given configuration.
     *
     * @param config The provider configuration
     * @return A ChatProvider implementation for the specified provider type
     */
    fun createProvider(config: ProviderConfig): ChatProvider {
        return when (config.type) {
            ProviderType.OPENAI -> OpenAIProvider(httpClient, config)
            ProviderType.ANTHROPIC -> AnthropicProvider(httpClient, config)
            ProviderType.GOOGLE -> GoogleProvider(httpClient, config)
            ProviderType.OPENROUTER -> OpenRouterProvider(httpClient, config)
            ProviderType.CUSTOM -> CustomProvider(httpClient, config)
        }
    }

    /**
     * Returns the default models for a provider type when model fetching fails.
     */
    fun getDefaultModels(providerType: ProviderType): List<ModelInfo> {
        return when (providerType) {
            ProviderType.OPENAI -> listOf(
                ModelInfo("gpt-4o", "GPT-4o", "Most capable GPT-4 model", 128000, ProviderType.OPENAI),
                ModelInfo("gpt-4o-mini", "GPT-4o Mini", "Affordable GPT-4 variant", 128000, ProviderType.OPENAI),
                ModelInfo("gpt-4-turbo", "GPT-4 Turbo", "GPT-4 with vision capabilities", 128000, ProviderType.OPENAI),
                ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", "Fast and efficient model", 16385, ProviderType.OPENAI)
            )
            ProviderType.ANTHROPIC -> listOf(
                ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Best balance of intelligence and speed", 200000, ProviderType.ANTHROPIC),
                ModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "Fastest Claude model", 200000, ProviderType.ANTHROPIC),
                ModelInfo("claude-3-opus-20240229", "Claude 3 Opus", "Most capable Claude model", 200000, ProviderType.ANTHROPIC)
            )
            ProviderType.GOOGLE -> listOf(
                ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", "Advanced reasoning model", 1000000, ProviderType.GOOGLE),
                ModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", "Fast and efficient model", 1000000, ProviderType.GOOGLE),
                ModelInfo("gemini-pro", "Gemini Pro", "General purpose model", 32000, ProviderType.GOOGLE)
            )
            ProviderType.OPENROUTER -> listOf(
                ModelInfo("openai/gpt-4o", "GPT-4o (via OpenRouter)", "OpenAI GPT-4o", 128000, ProviderType.OPENROUTER),
                ModelInfo("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet (via OpenRouter)", "Anthropic Claude", 200000, ProviderType.OPENROUTER),
                ModelInfo("google/gemini-pro-1.5", "Gemini Pro 1.5 (via OpenRouter)", "Google Gemini", 1000000, ProviderType.OPENROUTER)
            )
            ProviderType.CUSTOM -> emptyList()
        }
    }
}
