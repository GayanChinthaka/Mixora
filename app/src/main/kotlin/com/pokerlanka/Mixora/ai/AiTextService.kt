/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.pokerlanka.mixora.constants.AiProvider
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiServiceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

object AiTextService {
    private const val OpenAiEndpoint = "https://api.openai.com/v1/chat/completions"
    private const val OpenAiModelsEndpoint = "https://api.openai.com/v1/models"
    private const val OpenRouterEndpoint = "https://openrouter.ai/api/v1/chat/completions"
    private const val OpenRouterModelsEndpoint = "https://openrouter.ai/api/v1/models"
    private const val GeminiBaseEndpoint = "https://generativelanguage.googleapis.com/v1beta"

    // Enough headroom for a thinking model to reason and still emit the health-check answer.
    private const val HealthCheckMaxTokens = 512
    private const val GeminiModelsPageSize = 200
    private const val GeminiModelsMaxPages = 10


    private val client =
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(20, TimeUnit.SECONDS)
                    readTimeout(60, TimeUnit.SECONDS)
                    writeTimeout(60, TimeUnit.SECONDS)
                    retryOnConnectionFailure(true)
                }
            }
        }

    suspend fun test(config: AiServiceConfig) {
        val response =
            complete(
                config = config,
                systemPrompt = "You are a health check endpoint. Reply with OK only.",
                userPrompt = "Reply exactly OK.",
                temperature = 0.0,
                maxTokens = HealthCheckMaxTokens,
            ).trim()
        if (!response.contains("OK", ignoreCase = true)) {
            throw AiServiceException("AI API returned an unexpected test response")
        }
    }

    /**
     * Transliterates [lines] into Latin script, returning one result per input in the same order.
     *
     * Lines are sent as `{"i": <index>, "t": <text>}` and read back by `i` rather than by array
     * position: a model that reorders or renumbers is then a detectable error instead of a silent
     * lyric/timestamp desync. Callers must treat any thrown exception as "no romanization" and
     * leave the original text on screen.
     */
    suspend fun romanizeLines(
        config: AiServiceConfig,
        lines: List<String>,
        pinyinToneMarks: Boolean,
    ): List<String> {
        if (lines.isEmpty()) return emptyList()
        val payload = JSONArray()
        lines.forEachIndexed { index, text ->
            payload.put(JSONObject().put("i", index).put("t", text))
        }
        val response =
            complete(
                config = config,
                systemPrompt = romanizationSystemPrompt(pinyinToneMarks),
                userPrompt = payload.toString(),
                // Transliteration is mechanical: any creativity here is a defect.
                temperature = 0.0,
                maxTokens = 8192,
                jsonSchema = RomanizationResponseSchema,
            )
        val array = extractJsonArray(response)
        require(array.length() == lines.size) {
            "AI response changed the lyric line count (${array.length()} for ${lines.size})"
        }
        val byIndex = HashMap<Int, String>(lines.size)
        for (position in 0 until array.length()) {
            val obj = array.optJSONObject(position) ?: throw AiServiceException("AI response item $position was not an object")
            val index = obj.optInt("i", -1)
            require(index in lines.indices) { "AI response returned out-of-range line index $index" }
            // Newlines are flattened here, at the trust boundary: one lyric line must stay one
            // line, and the cache stores results newline-joined, so an embedded break would
            // shift every later line against its timestamp on the next read.
            val romanized = obj.optString("r").replace('\n', ' ').replace('\r', ' ').trim()
            require(byIndex.put(index, romanized) == null) { "AI response repeated line index $index" }
        }
        return List(lines.size) { index ->
            // A blank result means the model had nothing to add; the original is the safe answer.
            byIndex[index]?.takeIf { it.isNotBlank() } ?: lines[index]
        }
    }

    suspend fun complete(
        config: AiServiceConfig,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.2,
        maxTokens: Int = 4096,
        jsonSchema: JSONObject? = null,
    ): String {
        if (!config.canCallApi) throw AiServiceException("AI provider is not configured")
        // Never substitute a hardcoded fallback: model ids get retired, and a guessed id
        // fails as an opaque 404 instead of telling the user to pick a model.
        val model =
            config.model.trim().ifBlank {
                throw AiServiceException("No AI model selected")
            }
        return when (config.provider) {
            AiProvider.CHATGPT -> {
                completeOpenAiCompatible(
                    endpoint = OpenAiEndpoint,
                    apiKey = config.apiKey,
                    model = model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    jsonSchema = jsonSchema,
                )
            }

            AiProvider.OPENROUTER -> {
                completeOpenAiCompatible(
                    endpoint = OpenRouterEndpoint,
                    apiKey = config.apiKey,
                    model = model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    jsonSchema = jsonSchema,
                )
            }

            AiProvider.CUSTOM -> {
                completeOpenAiCompatible(
                    endpoint = config.customEndpoint,
                    apiKey = config.apiKey,
                    model = model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    jsonSchema = jsonSchema,
                )
            }

            AiProvider.GEMINI -> {
                completeGemini(
                    apiKey = config.apiKey,
                    model = model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    jsonSchema = jsonSchema,
                )
            }

            AiProvider.NONE -> {
                throw AiServiceException("AI provider is disabled")
            }
        }
    }

    suspend fun fetchModels(config: AiServiceConfig): List<AiModelOption> {
        if (!config.canCallApi) throw AiServiceException("AI provider is not configured")
        return when (config.provider) {
            AiProvider.CHATGPT -> fetchOpenAiModels(OpenAiModelsEndpoint, config.apiKey)
            AiProvider.OPENROUTER -> fetchOpenAiModels(OpenRouterModelsEndpoint, config.apiKey)
            AiProvider.GEMINI -> fetchGeminiModels(config.apiKey)
            AiProvider.CUSTOM, AiProvider.NONE -> emptyList()
        }
    }

    private suspend fun completeOpenAiCompatible(
        endpoint: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
        maxTokens: Int,
        jsonSchema: JSONObject?,
    ): String {
        val messages =
            JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
                .put(JSONObject().put("role", "user").put("content", userPrompt))
        val body =
            JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("temperature", temperature)
                .put("max_tokens", maxTokens)
                .apply {
                    // Schema-shaped constrained decoding is not portable across OpenAI-compatible
                    // gateways, so ask only for "must be JSON" and let the caller validate shape.
                    if (jsonSchema != null) {
                        put("response_format", JSONObject().put("type", "json_object"))
                    }
                }.toString()
        val response =
            client.post(endpoint.trim()) {
                header("Authorization", "Bearer ${apiKey.trim()}")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) throw apiException(response.status.value, raw)
        val json = JSONObject(raw)
        val content =
            json
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.takeIf { it.isNotBlank() }
        return content ?: throw AiServiceException("AI API returned an empty response")
    }

    private suspend fun completeGemini(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
        maxTokens: Int,
        jsonSchema: JSONObject?,
    ): String {
        val trimmedModel = model.trim()
        val endpoint = "$GeminiBaseEndpoint/models/$trimmedModel:generateContent?key=${apiKey.trim()}"
        // No thinkingConfig here on purpose: a zero budget is rejected outright by several
        // current models (gemini-3.6-flash, gemini-flash-lite-latest, the Gemma 4 family)
        // and there is no identifier pattern that predicts which. A generous
        // maxOutputTokens leaves room for thinking plus the answer on every model instead.
        val generationConfig =
            JSONObject()
                .put("temperature", temperature)
                .put("maxOutputTokens", maxTokens)
                .apply {
                    // Constrained decoding: the model cannot emit prose, fences, or a wrong shape,
                    // which removes the whole class of "explained itself instead of answering" bugs.
                    if (jsonSchema != null) {
                        put("responseMimeType", "application/json")
                        put("responseSchema", jsonSchema)
                    }
                }
        val body =
            JSONObject()
                .put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", "$systemPrompt\n\n$userPrompt"),
                            ),
                        ),
                    ),
                ).put("generationConfig", generationConfig)
                .toString()
        val response =
            client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) throw apiException(response.status.value, raw)
        val candidate = JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)
        val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
        val content =
            (0 until (parts?.length() ?: 0))
                .mapNotNull { parts?.optJSONObject(it)?.optString("text")?.takeIf { text -> text.isNotBlank() } }
                .joinToString("")
                .takeIf { it.isNotBlank() }
        if (content != null) return content
        // A thinking model can burn the whole output budget before emitting any text,
        // which comes back as a 200 with a part-less candidate.
        throw when (candidate?.optString("finishReason")?.takeIf { it.isNotBlank() }) {
            null -> AiServiceException("AI API returned an empty response")
            "MAX_TOKENS" ->
                AiServiceException(
                    "AI API returned no text: $trimmedModel hit the output token limit before answering",
                )
            else ->
                AiServiceException(
                    "AI API returned no text (finishReason: ${candidate.optString("finishReason")})",
                )
        }
    }

    private suspend fun fetchOpenAiModels(
        endpoint: String,
        apiKey: String,
    ): List<AiModelOption> {
        val response =
            client.get(endpoint) {
                header("Authorization", "Bearer ${apiKey.trim()}")
            }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) throw apiException(response.status.value, raw)
        val data = JSONObject(raw).optJSONArray("data") ?: return emptyList()
        return buildList {
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                add(
                    AiModelOption(
                        id = id,
                        displayName = obj.optString("name").ifBlank { id },
                        category = openAiCategoryFor(id),
                    ),
                )
            }
        }.sortedWith(compareBy({ it.category.ordinal }, { it.id }))
    }

    private suspend fun fetchGeminiModels(apiKey: String): List<AiModelOption> {
        val key = apiKey.trim()
        var pageToken: String? = null
        var page = 0
        return buildList {
            do {
                val response =
                    client.get("$GeminiBaseEndpoint/models") {
                        parameter("key", key)
                        parameter("pageSize", GeminiModelsPageSize)
                        pageToken?.takeIf { it.isNotBlank() }?.let { parameter("pageToken", it) }
                    }
                val raw = response.bodyAsText()
                if (response.status.value !in 200..299) throw apiException(response.status.value, raw)
                val json = JSONObject(raw)
                val models = json.optJSONArray("models")
                for (i in 0 until (models?.length() ?: 0)) {
                    val obj = models?.optJSONObject(i) ?: continue
                    val id = obj.optString("name").removePrefix("models/").takeIf { it.isNotBlank() } ?: continue
                    val methodsArray = obj.optJSONArray("supportedGenerationMethods")
                    val methods =
                        (0 until (methodsArray?.length() ?: 0))
                            .mapNotNull { methodsArray?.optString(it)?.takeIf { m -> m.isNotBlank() } }
                            .toSet()
                    add(
                        AiModelOption(
                            id = id,
                            displayName = obj.optString("displayName").ifBlank { id },
                            category = geminiCategoryFor(id, methods),
                        ),
                    )
                }
                pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() }
                page++
            } while (pageToken != null && page < GeminiModelsMaxPages)
        }.sortedWith(compareBy({ it.category.ordinal }, { it.displayName }))
    }

    /**
     * Gemini's listing advertises every model the key can see, including ones that need a
     * different endpoint or extra response modalities. Identifier markers are checked before
     * the method set because several text models also advertise `bidiGenerateContent`, and
     * treating those as Live-only would wrongly lock them out of the picker.
     */
    private fun geminiCategoryFor(
        id: String,
        methods: Set<String>,
    ): AiModelCategory {
        val lower = id.lowercase()
        return when {
            lower.contains("computer-use") -> AiModelCategory.COMPUTER_USE
            // Listed with generateContent, but answer "This model only supports Interactions API".
            lower.contains("deep-research") || lower.contains("antigravity") -> AiModelCategory.AGENT
            lower.contains("native-audio") ||
                lower.contains("audio-dialog") ||
                lower.contains("-live-") -> AiModelCategory.LIVE
            lower.contains("-tts") -> AiModelCategory.SPEECH
            lower.contains("veo") -> AiModelCategory.VIDEO
            lower.contains("lyria") -> AiModelCategory.MUSIC
            // "nano-banana-pro-preview" carries no -image marker, so match the family name too.
            lower.contains("imagen") ||
                lower.contains("nano-banana") ||
                lower.contains("-image") ||
                lower.contains("image-generation") -> AiModelCategory.IMAGE
            "embedContent" in methods || "embedText" in methods -> AiModelCategory.EMBEDDING
            "generateContent" in methods -> AiModelCategory.TEXT
            "bidiGenerateContent" in methods -> AiModelCategory.LIVE
            "predictLongRunning" in methods -> AiModelCategory.VIDEO
            "predict" in methods -> AiModelCategory.IMAGE
            "generateAnswer" in methods -> AiModelCategory.GROUNDED_QA
            else -> AiModelCategory.OTHER
        }
    }

    /** OpenAI-style listings carry no capability metadata, so fall back to the identifier. */
    private fun openAiCategoryFor(id: String): AiModelCategory {
        val lower = id.lowercase()
        return when {
            lower.contains("computer-use") -> AiModelCategory.COMPUTER_USE
            lower.contains("embed") -> AiModelCategory.EMBEDDING
            lower.contains("realtime") -> AiModelCategory.LIVE
            lower.contains("whisper") || lower.contains("tts") || lower.contains("transcribe") ||
                lower.contains("audio") -> AiModelCategory.SPEECH
            lower.contains("sora") || lower.contains("veo") -> AiModelCategory.VIDEO
            lower.contains("dall-e") || lower.contains("imagen") ||
                lower.contains("-image") || lower.contains("image-") -> AiModelCategory.IMAGE
            lower.contains("moderation") -> AiModelCategory.OTHER
            else -> AiModelCategory.TEXT
        }
    }

    private fun apiException(
        status: Int,
        raw: String,
    ): AiServiceException {
        val message =
            runCatching { JSONObject(raw).readErrorMessage() }.getOrNull()
                ?: raw.take(240).ifBlank { "HTTP $status" }
        return AiServiceException("AI API failed ($status): $message")
    }
}
