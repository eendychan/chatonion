package com.flxrs.dankchat.ui.main.dialog

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.donations.DonationProvider
import com.flxrs.dankchat.preferences.donations.DonationWidget

/**
 * In-app browser for donation widgets. Widgets bound to the active channel are preferred;
 * if none match, all configured widgets are shown. Multiple widgets stack top-to-bottom.
 */
@Composable
fun DonationsDialog(
    widgets: List<DonationWidget>,
    activeChannel: UserName?,
    onDismiss: () -> Unit,
) {
    val visibleWidgets = visibleWidgetsFor(widgets, activeChannel)
    if (visibleWidgets.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

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
                    .fillMaxHeight(0.85f),
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
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.back))
                    }
                }

                visibleWidgets.forEach { widget ->
                    DonationWidgetWebView(
                        widget = widget,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    )
                }
            }
        }
    }
}

private fun visibleWidgetsFor(
    widgets: List<DonationWidget>,
    activeChannel: UserName?,
): List<DonationWidget> {
    val configured = widgets.filter { it.isConfigured }
    val forChannel =
        activeChannel?.let { channel ->
            configured.filter { it.channel.equals(channel.value, ignoreCase = true) }
        }.orEmpty()
    return forChannel.ifEmpty { configured }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DonationWidgetWebView(
    widget: DonationWidget,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = WebViewClient()
                when (widget.provider) {
                    DonationProvider.StreamElements -> {
                        loadDataWithBaseURL(
                            "https://streamelements.com/",
                            buildStreamElementsTipsPage(widget.urlOrToken),
                            "text/html",
                            "UTF-8",
                            null,
                        )
                    }

                    else -> loadUrl(widget.urlOrToken)
                }
            }
        },
    )
}

/**
 * StreamElements has no public tip widget URL, so a minimal widget page is built locally:
 * it authenticates against the SE realtime gateway with the JWT token and renders incoming
 * tip events (topics channel-tips / channel-tips-moderation are delivered as "event"/"event:test").
 */
private fun buildStreamElementsTipsPage(jwtToken: String): String {
    val token = jwtToken.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "")
    return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  html, body { margin: 0; padding: 8px; background: transparent; font-family: sans-serif; }
  .tip {
    background: #1f1f23; color: #efeff1; border-left: 4px solid #9147ff;
    border-radius: 8px; padding: 10px 12px; margin-bottom: 8px;
    animation: pop 0.25s ease-out; word-break: break-word;
  }
  .tip .head { font-weight: 700; font-size: 15px; }
  .tip .amount { color: #57ff8f; }
  .tip .msg { margin-top: 4px; font-size: 14px; color: #c8c8d0; }
  .status { color: #8b8b93; font-size: 13px; text-align: center; margin-top: 12px; }
  @keyframes pop { from { transform: scale(0.9); opacity: 0; } to { transform: scale(1); opacity: 1; } }
</style>
<script src="https://cdn.socket.io/4.7.5/socket.io.min.js"></script>
</head>
<body>
<div id="status" class="status">StreamElements: connecting…</div>
<div id="tips"></div>
<script>
  var statusEl = document.getElementById('status');
  var tipsEl = document.getElementById('tips');
  function setStatus(t) { statusEl.textContent = t; }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  function addTip(d) {
    setStatus('');
    var el = document.createElement('div');
    el.className = 'tip';
    var amount = esc(d.amount) + ' ' + esc(d.currency || '');
    el.innerHTML = '<div class="head">' + esc(d.username || d.name || 'anon') +
      ' — <span class="amount">' + amount + '</span></div>' +
      (d.message ? '<div class="msg">' + esc(d.message) + '</div>' : '');
    tipsEl.insertBefore(el, tipsEl.firstChild);
  }
  function onEvent(event) {
    if (!event) return;
    var type = event.type || (event.listener === 'tip-latest' ? 'tip' : null);
    if (type === 'tip' && event.data) addTip(event.data);
  }
  try {
    var socket = io('https://realtime.streamelements.com', { transports: ['websocket'] });
    socket.on('connect', function () {
      setStatus('StreamElements: authenticating…');
      socket.emit('authenticate', { method: 'jwt', token: '$token' });
    });
    socket.on('authenticated', function () { setStatus(''); });
    socket.on('unauthorized', function () { setStatus('StreamElements: invalid JWT token'); });
    socket.on('connect_error', function () { setStatus('StreamElements: connection error'); });
    socket.on('event', onEvent);
    socket.on('event:test', onEvent);
  } catch (e) {
    setStatus('StreamElements: failed to load socket.io');
  }
</script>
</body>
</html>
    """.trimIndent()
}
