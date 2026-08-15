/**
 * YTmusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.ytmusic.ui.screens

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton as Material3IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.pokerlanka.innertube.YouTube
import com.pokerlanka.innertube.utils.parseCookieString
import com.pokerlanka.ytmusic.LocalPlayerAwareWindowInsets
import com.pokerlanka.ytmusic.R
import com.pokerlanka.ytmusic.constants.AccountChannelHandleKey
import com.pokerlanka.ytmusic.constants.AccountEmailKey
import com.pokerlanka.ytmusic.constants.AccountNameKey
import com.pokerlanka.ytmusic.constants.DataSyncIdKey
import com.pokerlanka.ytmusic.constants.InnerTubeCookieKey
import com.pokerlanka.ytmusic.constants.VisitorDataKey
import com.pokerlanka.ytmusic.ui.component.IconButton
import com.pokerlanka.ytmusic.ui.utils.backToMain
import com.pokerlanka.ytmusic.utils.reportException
import com.pokerlanka.ytmusic.utils.safeDataStoreEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

private const val MUSIC_HOST = "music.youtube.com"

/**
 * Google sign-in entry point.
 *
 * `passive=true` is what makes an existing `.google.com` session fall straight through to
 * music.youtube.com instead of prompting again — it is the half that pays off the cookie
 * retention in [com.pokerlanka.ytmusic.App.forgetAccount]. Routing `continue` through
 * youtube.com/signin?action_handle_signin=true is YouTube's own session-establishment
 * endpoint, which hands off to music.youtube.com more reliably than landing there directly.
 * `ltmpl=music` gets the branded "continue to YouTube" page rather than a bare sign-in form.
 */
private const val SIGN_IN_URL =
    "https://accounts.google.com/ServiceLogin" +
        "?ltmpl=music" +
        "&service=youtube" +
        "&passive=true" +
        "&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue" +
        "%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"

/**
 * A YouTube Music cookie only proves a *session*, not a *login* — an anonymous visit already
 * sets YSC and VISITOR_INFO1_LIVE. SAPISID is the cookie the app actually authenticates with
 * (see InnerTube's SAPISIDHASH header), so it is the only honest test for "signed in", and it
 * matches how the rest of the app derives login state.
 */
private fun CookieManager.hasSignedInSession(): Boolean =
    "SAPISID" in parseCookieString(getCookie("https://$MUSIC_HOST").orEmpty())

/** True while Google is asking which account to sign in as. */
private fun isIdentifierPage(url: String?): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    return uri.host == "accounts.google.com" && uri.path.orEmpty().contains("signin/identifier")
}

/**
 * Fill the email field with [email] without submitting.
 *
 * Google ignores every server-side prefill parameter (`login_hint`, `Email`, `identifier`, …) on
 * this form, so the value has to be written client-side. Assigning through the native
 * `HTMLInputElement.value` setter rather than `field.value = …` is what makes the page's own
 * change-tracking notice the write; a plain assignment can be silently reverted on submit.
 *
 * Deliberately does not click "Next" — a wrong or stale account should be correctable, and
 * auto-submitting a guessed identity is worse than leaving one tap to the user.
 */
private fun emailPrefillScript(email: String): String {
    val quoted = JSONObject.quote(email)
    return """
        (function() {
            var field = document.querySelector('input[name=identifier]')
                || document.querySelector('#identifierId');
            if (!field) return 'no-field';
            var setter = Object.getOwnPropertyDescriptor(
                window.HTMLInputElement.prototype, 'value'
            ).set;
            setter.call(field, $quoted);
            field.dispatchEvent(new Event('input', { bubbles: true }));
            field.dispatchEvent(new Event('change', { bubbles: true }));
            return 'ok';
        })()
    """.trimIndent()
}

/** System dialog listing the Google accounts already present on the device. */
private fun chooseGoogleAccountIntent(): Intent =
    AccountManager.newChooseAccountIntent(
        null,
        null,
        arrayOf("com.google"),
        null,
        null,
        null,
        null,
    )

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCompletingLogin by remember { mutableStateOf(false) }

    // Must survive recomposition: the AndroidView factory runs once, so a plain local would be
    // reset to null on the next composition and strand every later reader (back navigation,
    // WebView cleanup, account prefill) with a null reference.
    var webView by remember { mutableStateOf<WebView?>(null) }
    var visitorDataFromWeb by remember { mutableStateOf("") }
    var dataSyncIdFromWeb by remember { mutableStateOf("") }

    // The picker is offered once per visit. Re-prompting after a dismissal would trap the user
    // in a dialog they already declined, and Google re-renders the identifier page on every
    // validation error.
    var hasOfferedAccountPicker by remember { mutableStateOf(false) }

    val accountPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Timber.d("Login: Account picker dismissed without a selection")
                return@rememberLauncherForActivityResult
            }
            val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (email.isNullOrBlank()) {
                Timber.d("Login: Account picker returned no account name")
                return@rememberLauncherForActivityResult
            }
            Timber.d("Login: Prefilling sign-in form with the selected device account")
            webView?.evaluateJavascript(emailPrefillScript(email)) { outcome ->
                // Best effort by design — Google can rename the field at any time, and a failed
                // prefill must degrade to "type it yourself", never to a broken login.
                Timber.d("Login: Email prefill result=$outcome")
            }
        }

    fun offerAccountPicker() {
        runCatching { accountPickerLauncher.launch(chooseGoogleAccountIntent()) }
            .onFailure { Timber.w(it, "Login: Could not open the device account picker") }
    }

    fun completeLogin(onClose: () -> Unit) {
        if (isCompletingLogin) return

        isCompletingLogin = true
        coroutineScope.launch {
            val currentCookie = CookieManager.getInstance().getCookie("https://$MUSIC_HOST").orEmpty()
            if ("SAPISID" !in parseCookieString(currentCookie)) {
                Timber.d("Login: No signed-in YouTube Music session on close, leaving login screen")
                isCompletingLogin = false
                onClose()
                return@launch
            }

            // Save extracted values from the WebView before validating
            val savedVisitorData = visitorDataFromWeb
            val savedDataSyncId = dataSyncIdFromWeb

            // Initialize YouTube object with selected authentication data
            YouTube.cookie = currentCookie
            YouTube.dataSyncId = savedDataSyncId
            YouTube.visitorData = savedVisitorData

            Timber.d("Login: Manual close detected, validating selected account...")

            YouTube
                .accountInfo()
                .onSuccess { info ->
                    Timber.d("Login: Successfully logged in as ${info.name}, restarting app...")

                    // Clean up WebView
                    webView?.apply {
                        stopLoading()
                        clearHistory()
                        clearCache(true)
                        clearFormData()
                    }

                    // Save ALL credentials atomically to DataStore, then restart the app.
                    // The write must complete before the process is killed, otherwise the
                    // async DataStore coroutines lose the credentials; only then start the
                    // launch intent and exit so all services reinitialize cleanly.
                    val saved = withContext(Dispatchers.IO) {
                        context.safeDataStoreEdit { settings ->
                            settings[InnerTubeCookieKey] = currentCookie
                            settings[VisitorDataKey] = savedVisitorData
                            settings[DataSyncIdKey] = savedDataSyncId
                            settings[AccountNameKey] = info.name
                            settings[AccountEmailKey] = info.email.orEmpty()
                            settings[AccountChannelHandleKey] = info.channelHandle.orEmpty()
                        }
                    }

                    if (!saved) {
                        Timber.e("Login: Failed to persist account data")
                        isCompletingLogin = false
                        return@onSuccess
                    }

                    withContext(Dispatchers.Main) {
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    }
                }.onFailure {
                    Timber.e(it, "Login: Authentication validation failed after manual close")
                    reportException(it)
                    isCompletingLogin = false
                    onClose()
                }
        }
    }

    AndroidView(
        modifier =
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
        factory = { webViewContext ->
            WebView(webViewContext).apply {
                webViewClient =
                    object : WebViewClient() {
                        /**
                         * Once sign-in succeeds Google tries to deep-link into the official
                         * YouTube Music app with an `intent://` URL. A WebView cannot resolve
                         * that scheme and fails the page with ERR_UNKNOWN_URL_SCHEME, stranding
                         * the user on an error instead of finishing the login (upstream
                         * Metrolist #3183). Swallow any non-HTTP scheme and go where the flow
                         * was heading anyway — the session cookies are already set by then.
                         */
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val scheme = request?.url?.scheme?.lowercase()
                            if (scheme == "http" || scheme == "https") return false

                            Timber.d("Login: Intercepted non-HTTP scheme '$scheme', redirecting to $MUSIC_HOST")
                            view.loadUrl("https://$MUSIC_HOST/")
                            return true
                        }

                        override fun onPageFinished(
                            view: WebView,
                            url: String?,
                        ) {
                            loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                            loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

                            // Only offer the picker once Google actually asks who is signing in.
                            // With a retained Google session, passive=true skips this page
                            // entirely and the user is never interrupted.
                            if (isIdentifierPage(url) && !hasOfferedAccountPicker) {
                                hasOfferedAccountPicker = true
                                offerAccountPicker()
                            }

                            // Match on the parsed host, not a substring: the sign-in continue
                            // URL carries "music.youtube.com" inside its own query string, so a
                            // substring test fires while still on accounts.google.com. And gate
                            // on SAPISID rather than "cookie is non-empty", since an anonymous
                            // visit already sets cookies for this host.
                            val host = runCatching { Uri.parse(url).host }.getOrNull()
                            if (host == MUSIC_HOST &&
                                !isCompletingLogin &&
                                CookieManager.getInstance().hasSignedInSession()
                            ) {
                                Timber.d("Login: Detected authenticated session on $MUSIC_HOST, completing login...")
                                completeLogin(navController::navigateUp)
                            }
                        }
                    }
                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            if (newVisitorData != null) {
                                visitorDataFromWeb = newVisitorData
                            }
                        }

                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            if (newDataSyncId != null) {
                                dataSyncIdFromWeb = newDataSyncId.substringBefore("||")
                            }
                        }
                    },
                    "Android",
                )
                webView = this
                loadUrl(SIGN_IN_URL)
            }
        },
    )

    TopAppBar(
        title = { Text(stringResource(R.string.login)) },
        navigationIcon = {
            IconButton(
                onClick = { completeLogin(navController::navigateUp) },
                onLongClick = { completeLogin(navController::backToMain) },
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            // Escape hatch: the automatic offer fires once, and only on the identifier page.
            // This lets the user summon the picker again — after a mistyped address, or to
            // switch accounts once Google has already moved past that step.
            Material3IconButton(onClick = { offerAccountPicker() }) {
                Icon(
                    painterResource(R.drawable.person),
                    contentDescription = stringResource(R.string.choose_device_account),
                )
            }
        },
    )

    BackHandler {
        val currentWebView = webView
        if (currentWebView?.canGoBack() == true) {
            currentWebView.goBack()
        } else {
            completeLogin(navController::navigateUp)
        }
    }
}
