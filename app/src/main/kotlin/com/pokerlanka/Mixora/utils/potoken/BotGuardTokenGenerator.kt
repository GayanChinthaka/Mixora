/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.utils.potoken

import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object BotGuardTokenGenerator {
    private const val TAG = "BotGuardTokenGen"
    private const val CREATE_URL = "https://www.youtube.com/api/jnn/v1/Create"
    private const val GENERATE_IT_URL = "https://www.youtube.com/api/jnn/v1/GenerateIT"
    private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
    private const val API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
    private const val WV_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
    private const val JS_BRIDGE = "BotGuardBridge"

    private const val COLD_START_TIMEOUT_MS = 45_000L
    private const val WARM_TIMEOUT_MS = 5_000L
    private const val PLAYER_TOKEN_CACHE_SIZE = 200

    private val httpClient =
        OkHttpClient
            .Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .build()

    private var appContext: Context? = null
    private var permanentlyBroken = false

    private val mutex = Mutex()
    private var engine: BotGuardEngine? = null
    private var engineSessionId: String? = null
    private var cachedSessionToken: String? = null
    private var engineReady = false

    private val playerTokenCache: LinkedHashMap<String, String> =
        object : LinkedHashMap<String, String>(0, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean = size > PLAYER_TOKEN_CACHE_SIZE
        }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        Timber.tag(TAG).d("Initialized")
    }

    suspend fun mintToken(
        videoId: String,
        sessionId: String,
    ): PoTokenResult? {
        val ctx = appContext ?: return null
        if (permanentlyBroken) return null

        val cachedResult = mutex.withLock {
            if (!isEngineReadyForSession(sessionId)) return@withLock null
            val cachedPlayer = playerTokenCache[videoId] ?: return@withLock null
            val sessionToken = cachedSessionToken ?: return@withLock null
            PoTokenResult(playerRequestPoToken = sessionToken, streamingDataPoToken = cachedPlayer)
        }
        if (cachedResult != null) return cachedResult

        val timeout = if (mutex.withLock { !isEngineReadyForSession(sessionId) }) COLD_START_TIMEOUT_MS else WARM_TIMEOUT_MS

        return try {
            withTimeout(timeout) {
                val result = mintTokenInternal(ctx, videoId, sessionId, forceNewEngine = false)
                mutex.withLock {
                    playerTokenCache[videoId] = result.streamingDataPoToken
                }
                result
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "mintToken failed")
            null
        }
    }

    private suspend fun mintTokenInternal(
        ctx: Context,
        videoId: String,
        sessionId: String,
        forceNewEngine: Boolean,
    ): PoTokenResult {
        val (eng, sessionTok, wasNew) = getOrCreateEngine(ctx, sessionId, forceNewEngine)
        val playerTok = try {
            eng.mint(videoId)
        } catch (e: Throwable) {
            if (wasNew) throw e
            return mintTokenInternal(ctx, videoId, sessionId, forceNewEngine = true)
        }
        return PoTokenResult(playerRequestPoToken = sessionTok, streamingDataPoToken = playerTok)
    }

    private suspend fun getOrCreateEngine(
        ctx: Context,
        sessionId: String,
        forceNewEngine: Boolean,
    ): Triple<BotGuardEngine, String, Boolean> = mutex.withLock {
        val needsNew = forceNewEngine || !isEngineReadyForSession(sessionId)
        if (needsNew) {
            withContext(Dispatchers.Main) { engine?.close() }
            engine = null
            engineSessionId = null
            cachedSessionToken = null
            engineReady = false
            playerTokenCache.clear()

            val newEngine = BotGuardEngine.create(ctx)
            val newSessionToken = try {
                newEngine.mint(sessionId)
            } catch (error: Throwable) {
                withContext(NonCancellable + Dispatchers.Main) { newEngine.close() }
                throw error
            }
            engine = newEngine
            engineSessionId = sessionId
            cachedSessionToken = newSessionToken
            engineReady = true
        }
        Triple(engine!!, cachedSessionToken!!, needsNew)
    }

    private fun isEngineReadyForSession(sessionId: String): Boolean =
        engineReady && engineSessionId == sessionId && cachedSessionToken != null && engine?.isExpired == false

    private class BotGuardEngine private constructor(
        private val webView: WebView,
        private val readySignal: CancellableContinuation<BotGuardEngine>,
    ) {
        private val scope = MainScope()
        private val closed = AtomicBoolean(false)
        private val terminalErrorSignaled = AtomicBoolean(false)
        private val readyCompleted = AtomicBoolean(false)
        private val pendingMints = Collections.synchronizedMap(ArrayMap<String, CancellableContinuation<String>>())
        private lateinit var expiry: Instant

        val isExpired: Boolean get() = Instant.now().isAfter(expiry)

        fun startBootstrap() {
            scope.launch {
                val html = withContext(Dispatchers.IO) {
                    webView.context.assets.open("po_token.html").bufferedReader().use { it.readText() }
                }
                val patched = html.replaceFirst("</script>", "\n$JS_BRIDGE.onPageLoaded()</script>")
                webView.loadDataWithBaseURL("https://www.youtube.com", patched, "text/html", "utf-8", null)
            }
        }

        @JavascriptInterface
        fun onPageLoaded() {
            postToBotGuard(CREATE_URL, "[ \"$REQUEST_KEY\" ]") { body ->
                val challengeJson = ChallengeParser.parseCreateChallenge(body)
                webView.evaluateJavascript(
                    """
                    try {
                        var data = $challengeJson;
                        runBotGuard(data).then(function(r) {
                            this.webPoSignalOutput = r.webPoSignalOutput;
                            $JS_BRIDGE.onBotGuardReady(r.botguardResponse);
                        }, function(e) {
                            $JS_BRIDGE.onFatalError(e + "\n" + e.stack);
                        });
                    } catch(e) { $JS_BRIDGE.onFatalError(e + "\n" + e.stack); }
                    """.trimIndent(),
                    null
                )
            }
        }

        @JavascriptInterface
        fun onBotGuardReady(botguardResponse: String) {
            postToBotGuard(GENERATE_IT_URL, "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]") { body ->
                try {
                    val (tokenU8, lifetimeSec) = ChallengeParser.parseIntegrityToken(body)
                    expiry = Instant.now().plusSeconds(lifetimeSec).minus(10, ChronoUnit.MINUTES)
                    webView.evaluateJavascript(
                        """
                        try {
                            this.integrityToken = $tokenU8;
                            createPoTokenMinter(webPoSignalOutput, integrityToken).then(function() {
                                $JS_BRIDGE.onMinterReady();
                            }).catch(function(e) {
                                $JS_BRIDGE.onFatalError(e + "\n" + (e.stack || ''));
                            });
                        } catch(e) { $JS_BRIDGE.onFatalError(e + "\n" + e.stack); }
                        """.trimIndent(),
                        null
                    )
                } catch (e: Exception) {
                    signalError(e)
                }
            }
        }

        @JavascriptInterface
        fun onMinterReady() {
            if (readyCompleted.compareAndSet(false, true)) readySignal.resume(this)
        }

        @JavascriptInterface
        fun onFatalError(error: String) {
            signalError(Exception(error))
        }

        suspend fun mint(identifier: String): String = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                pendingMints[identifier] = cont
                val u8Arg = ChallengeParser.stringToJsUint8Array(identifier)
                webView.evaluateJavascript(
                    """
                    try {
                        obtainPoToken($u8Arg).then(function(u8) {
                            $JS_BRIDGE.onMintOk("$identifier", u8.join(","));
                        }).catch(function(e) {
                            $JS_BRIDGE.onMintErr("$identifier", e + "\n" + (e.stack || ''));
                        });
                    } catch(e) { $JS_BRIDGE.onMintErr("$identifier", e + "\n" + e.stack); }
                    """.trimIndent(),
                    null
                )
            }
        }

        @JavascriptInterface
        fun onMintOk(identifier: String, csvBytes: String) {
            val base64 = ChallengeParser.commaSeparatedBytesToBase64(csvBytes)
            pendingMints.remove(identifier)?.resume(base64)
        }

        @JavascriptInterface
        fun onMintErr(identifier: String, error: String) {
            pendingMints.remove(identifier)?.resumeWithException(Exception(error))
        }

        private fun postToBotGuard(url: String, jsonBody: String, onSuccess: (String) -> Unit) {
            scope.launch {
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody())
                    .headers(mapOf(
                        "User-Agent" to WV_USER_AGENT,
                        "Accept" to "application/json",
                        "Content-Type" to "application/json+protobuf",
                        "x-goog-api-key" to API_KEY,
                        "x-user-agent" to "grpc-web-javascript/0.1",
                    ).toHeaders())
                    .build()
                val response = withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
                if (response.code == 200) onSuccess(response.body!!.string())
                else signalError(Exception("BotGuard HTTP ${response.code}"))
            }
        }

        private fun signalError(error: Throwable) {
            if (terminalErrorSignaled.compareAndSet(false, true)) {
                close()
                if (readyCompleted.compareAndSet(false, true)) readySignal.resumeWithException(error)
            }
        }

        fun close() {
            if (!closed.compareAndSet(false, true)) return
            scope.cancel()
            webView.post {
                webView.destroy()
            }
        }

        companion object {
            suspend fun create(context: Context): BotGuardEngine = withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val wv = WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.userAgentString = WV_USER_AGENT
                    }
                    val engine = BotGuardEngine(wv, cont)
                    wv.addJavascriptInterface(engine, JS_BRIDGE)
                    engine.startBootstrap()
                }
            }
        }
    }
}
