package com.flxrs.dankchat.ui.main.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.donations.DonationProvider
import com.flxrs.dankchat.preferences.donations.DonationWidget

/**
 * Donations overlay: shows each donation widget (DonationAlerts, DonateX, DonatePay, StreamElements)
 * in an embedded browser tab. Widget pages render the full donation history themselves and update live,
 * exactly like they do in a regular browser or OBS.
 */
@Composable
fun DonationsDialog(
    widgets: List<DonationWidget>,
    activeChannel: UserName?,
    onDismiss: () -> Unit,
) {
    val visibleWidgets = remember(widgets, activeChannel) { visibleWidgetsFor(widgets, activeChannel) }
    if (visibleWidgets.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var reloadCounter by rememberSaveable { mutableIntStateOf(0) }
    val selectedWidget = visibleWidgets[selectedIndex.coerceIn(visibleWidgets.indices)]
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.75f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.donations_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (selectedWidget.provider != DonationProvider.StreamElements) {
                        IconButton(onClick = { reloadCounter++ }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.donation_widget_refresh),
                            )
                        }
                        IconButton(onClick = {
                            CustomTabsIntent
                                .Builder()
                                .build()
                                .launchUrl(context, Uri.parse(normalizeWidgetUrl(selectedWidget.urlOrToken)))
                        }) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = stringResource(R.string.donation_widget_open_browser),
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.back))
                    }
                }

                if (visibleWidgets.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        visibleWidgets.forEachIndexed { index, _ ->
                            WidgetTabButton(
                                number = index + 1,
                                selected = index == selectedIndex,
                                onClick = { selectedIndex = index },
                            )
                        }
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    key(selectedWidget.urlOrToken, reloadCounter) {
                        DonationWidgetWebView(
                            widget = selectedWidget,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetTabButton(
    number: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(onClick = onClick),
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

@Composable
private fun DonationWidgetWebView(
    widget: DonationWidget,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    // Retrying recreates the WebView from scratch instead of reloading it, because a WebView
    // whose renderer crashed (onRenderProcessGone) must never be used again.
    var retryCounter by remember { mutableIntStateOf(0) }
    val webViewVersion = remember { webViewVersion() }

    Box(modifier = modifier) {
        key(retryCounter) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    createDonationWebView(
                        context = viewContext,
                        widget = widget,
                        onLoadingChange = { isLoading = it },
                        onMainFrameError = { message ->
                            isLoading = false
                            loadError = message
                        },
                        onMainFrameFinish = { isLoading = false },
                    )
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.destroy()
                },
            )
        }

        if (isLoading && loadError == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }

        loadError?.let { error ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.donation_widget_load_error, error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.donation_widget_webview_version, webViewVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = {
                    loadError = null
                    isLoading = true
                    retryCounter++
                }) {
                    Text(text = stringResource(R.string.donation_widget_retry))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createDonationWebView(
    context: Context,
    widget: DonationWidget,
    onLoadingChange: (Boolean) -> Unit,
    onMainFrameError: (String) -> Unit,
    onMainFrameFinish: () -> Unit,
): WebView {
    // WebViews behave most reliably with an Activity context; the Dialog context is a wrapper around it
    val activityContext = context.findActivity() ?: context
    return WebView(activityContext).apply {
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )

        with(settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            offscreenPreRaster = true
            // Strip the WebView markers ("; wv", "Version/4.0") so bot protection treats the
            // request like a regular Chrome mobile browser, which is what the widgets expect
            userAgentString =
                userAgentString
                    .replace("; wv", "")
                    .replace("Version/4.0 ", "")
        }

        // Older WebView builds append "X-Requested-With: <package name>" to requests, which WAFs
        // (e.g. Cloudflare) use to detect and block in-app browsers. Disable it where supported.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebViewCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
        }

        // Never serve a stale (possibly failed) page cached by an earlier attempt
        clearCache(true)
        clearFormData()

        with(CookieManager.getInstance()) {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@apply, true)
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webChromeClient = WebChromeClient()

        webViewClient =
            object : WebViewClient() {
                override fun onPageStarted(
                    view: WebView?,
                    url: String?,
                    favicon: Bitmap?,
                ) {
                    onLoadingChange(true)
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?,
                ) {
                    onMainFrameFinish()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    if (request?.isForMainFrame == true) {
                        onMainFrameError(error?.description?.toString().orEmpty())
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?,
                ) {
                    if (request?.isForMainFrame == true) {
                        onMainFrameError("HTTP ${errorResponse?.statusCode}")
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?,
                ) {
                    // Default behaviour is to silently cancel, leaving a blank page — surface it instead
                    handler?.cancel()
                    onMainFrameError("SSL (${error?.primaryError})")
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?,
                ): Boolean {
                    // Acknowledge the crash; the retry button recreates the WebView
                    onMainFrameError("renderer crashed")
                    return true
                }
            }

        if (widget.provider == DonationProvider.StreamElements) {
            // StreamElements has no public widget URL; the JWT token authorizes a live tips feed
            loadDataWithBaseURL(
                "https://streamelements.com/",
                buildStreamElementsTipsPage(widget.urlOrToken.trim()),
                "text/html",
                "utf-8",
                null,
            )
        } else {
            loadUrl(normalizeWidgetUrl(widget.urlOrToken))
        }
    }
}

private fun buildStreamElementsTipsPage(jwtToken: String): String =
    """
    <!doctype html>
    <html>
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            body { background: #121212; color: #eee; font-family: sans-serif; margin: 0; padding: 12px; }
            .tip { background: #1e1e1e; border-radius: 8px; margin-bottom: 8px; padding: 10px; }
            .amount { color: #4CAF50; font-weight: bold; }
            .muted { color: #888; font-size: 13px; }
        </style>
    </head>
    <body>
        <div id="status" class="muted">Connecting…</div>
        <div id="tips"></div>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/socket.io/2.3.0/socket.io.js"></script>
        <script>
            var statusEl = document.getElementById('status');
            var tipsEl = document.getElementById('tips');
            var socket = io('https://realtime.streamelements.com', { transports: ['websocket'] });
            socket.on('connect', function() {
                socket.emit('authenticate', { method: 'jwt', token: '$jwtToken' });
            });
            socket.on('authenticated', function() {
                statusEl.textContent = 'Connected. New donations will appear here.';
            });
            socket.on('unauthorized', function() {
                statusEl.textContent = 'Invalid JWT token.';
            });
            socket.on('event', function(event) { addTip(event); });
            socket.on('event:test', function(event) { addTip(event); });
            function addTip(event) {
                if (!event || event.type !== 'tip' || !event.data) return;
                var d = event.data;
                var el = document.createElement('div');
                el.className = 'tip';
                var amount = document.createElement('div');
                amount.className = 'amount';
                amount.textContent = d.amount + ' ' + (d.currency || '');
                var name = document.createElement('div');
                name.textContent = d.username || d.name || '';
                el.appendChild(amount);
                el.appendChild(name);
                if (d.message) {
                    var msg = document.createElement('div');
                    msg.textContent = d.message;
                    el.appendChild(msg);
                }
                tipsEl.insertBefore(el, tipsEl.firstChild);
            }
        </script>
    </body>
    </html>
    """.trimIndent()

private fun normalizeWidgetUrl(urlOrToken: String): String {
    val trimmed = urlOrToken.trim()
    return if ("://" in trimmed) trimmed else "https://$trimmed"
}

private fun webViewVersion(): String = WebView
    .getCurrentWebViewPackage()
    ?.let { "${it.packageName} ${it.versionName}" }
    ?: "?"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun visibleWidgetsFor(
    widgets: List<DonationWidget>,
    activeChannel: UserName?,
): List<DonationWidget> = widgets.filter { widget ->
    widget.isConfigured &&
        (widget.channel.isBlank() || widget.channel.equals(activeChannel?.value, ignoreCase = true))
}
