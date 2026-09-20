package com.flxrs.dankchat.data.donations

import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.di.UPLOAD_OKHTTP_CLIENT
import com.flxrs.dankchat.preferences.donations.DonationProvider
import com.flxrs.dankchat.preferences.donations.DonationWidget
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val logger = KotlinLogging.logger("DonationAggregator")

/**
 * Fetches recent donations (history) directly from each provider's API, using the
 * token embedded in the widget link — no embedded browser involved:
 * - DonationAlerts: widget page exposes a scoped access_token, used against api/v1/alerts/donations
 * - DonateX: /api/v1/donations accepts the widget token as a query parameter
 * - DonatePay: /api/v1/transactions accepts the widget token as access_token
 * - StreamElements: JWT authorizes /kappa/v2/channels/me and /kappa/v2/tips/{channelId}
 */
@Single
class DonationAggregator(
    @Named(UPLOAD_OKHTTP_CLIENT) private val httpClient: OkHttpClient,
    private val dispatchersProvider: DispatchersProvider,
) {
    /** Returns donations from all given widgets merged and sorted oldest-first. Failing providers are skipped. */
    suspend fun fetchRecent(widgets: List<DonationWidget>): List<DonationEvent> = coroutineScope {
        widgets
            .map { widget ->
                async {
                    runCatching { fetchForWidget(widget) }.getOrElse {
                        logger.debug(it) { "Donation fetch failed" }
                        emptyList()
                    }
                }
            }.flatMap { it.await() }
            .distinctBy { it.id }
            .sortedBy { it.timestampEpochMs }
    }

    private suspend fun fetchForWidget(widget: DonationWidget): List<DonationEvent> = when (widget.provider) {
        DonationProvider.DonationAlerts -> fetchDonationAlerts(widget.urlOrToken)
        DonationProvider.DonateX -> fetchDonateX(widget.urlOrToken)
        DonationProvider.DonatePay -> fetchDonatePay(widget.urlOrToken)
        DonationProvider.StreamElements -> fetchStreamElements(widget.urlOrToken)
        null -> emptyList()
    }

    private suspend fun fetchDonationAlerts(widgetUrl: String): List<DonationEvent> {
        val widgetToken = extractQueryParam(widgetUrl, "token") ?: return emptyList()
        // Any widget page embeds a scoped access token for the internal API
        val widgetPage = httpGet("https://www.donationalerts.com/widget/alerts?token=$widgetToken") ?: return emptyList()
        val accessToken = DA_ACCESS_TOKEN_REGEX.find(widgetPage)?.groupValues?.getOrNull(1) ?: return emptyList()
        val body = httpGet("https://www.donationalerts.com/api/v1/alerts/donations", bearer = accessToken) ?: return emptyList()
        val data = Json.parseToJsonElement(body).objectValue("data") as? JsonArray ?: return emptyList()
        return data.mapNotNull { element ->
            val donation = element as? JsonObject ?: return@mapNotNull null
            val id = donation.textValue("id") ?: return@mapNotNull null
            DonationEvent(
                id = "da:$id",
                provider = DonationProvider.DonationAlerts,
                username = donation.textValue("username").orEmpty(),
                amountText =
                    listOfNotNull(
                        donation.textValue("amount_formatted") ?: donation.textValue("amount"),
                        donation.textValue("currency"),
                    ).joinToString(" "),
                message = donation.textValue("message").orEmpty(),
                timestampEpochMs = parseTimestamp(donation.textValue("date_created")),
            )
        }
    }

    private suspend fun fetchDonateX(widgetUrl: String): List<DonationEvent> {
        val token = extractQueryParam(widgetUrl, "token") ?: return emptyList()
        val body = httpGet("https://donatex.gg/api/v1/donations?skip=0&take=$PAGE_SIZE&token=$token") ?: return emptyList()
        val donations = Json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return donations.mapNotNull { element ->
            val donation = element as? JsonObject ?: return@mapNotNull null
            val id = donation.textValue("id") ?: return@mapNotNull null
            DonationEvent(
                id = "dx:$id",
                provider = DonationProvider.DonateX,
                username = donation.textValue("username").orEmpty(),
                amountText =
                    listOfNotNull(
                        donation.textValue("amount"),
                        donation.textValue("currency"),
                    ).joinToString(" "),
                message = donation.textValue("message").orEmpty(),
                timestampEpochMs = parseTimestamp(donation.textValue("timestamp")),
            )
        }
    }

    private suspend fun fetchDonatePay(widgetUrl: String): List<DonationEvent> {
        val token = extractQueryParam(widgetUrl, "token") ?: return emptyList()
        val body = httpGet("https://donatepay.ru/api/v1/transactions?access_token=$token&limit=$PAGE_SIZE") ?: return emptyList()
        val data = Json.parseToJsonElement(body).objectValue("data") as? JsonArray ?: return emptyList()
        return data.mapNotNull { element ->
            val donation = element as? JsonObject ?: return@mapNotNull null
            val id = donation.textValue("id") ?: return@mapNotNull null
            DonationEvent(
                id = "dp:$id",
                provider = DonationProvider.DonatePay,
                username = donation.textValue("what") ?: donation.textValue("username").orEmpty(),
                amountText =
                    listOfNotNull(
                        donation.textValue("sum"),
                        donation.textValue("currency"),
                    ).joinToString(" "),
                message = donation.textValue("comment") ?: donation.textValue("message").orEmpty(),
                timestampEpochMs = parseTimestamp(donation.textValue("created_at")),
            )
        }
    }

    private suspend fun fetchStreamElements(jwtToken: String): List<DonationEvent> {
        val jwt = jwtToken.trim().takeIf { it.isNotBlank() } ?: return emptyList()
        val channelBody = httpGet("https://api.streamelements.com/kappa/v2/channels/me", bearer = jwt) ?: return emptyList()
        val channelId = Json.parseToJsonElement(channelBody).textValue("_id") ?: return emptyList()
        val tipsBody = httpGet("https://api.streamelements.com/kappa/v2/tips/$channelId?limit=$PAGE_SIZE", bearer = jwt) ?: return emptyList()
        val root = Json.parseToJsonElement(tipsBody)
        val tips = (root as? JsonArray) ?: (root.objectValue("docs") as? JsonArray) ?: return emptyList()
        return tips.mapNotNull { element ->
            val tip = element as? JsonObject ?: return@mapNotNull null
            val donation = tip.objectValue("donation") as? JsonObject
            val id = tip.textValue("_id") ?: return@mapNotNull null
            DonationEvent(
                id = "se:$id",
                provider = DonationProvider.StreamElements,
                username =
                    donation?.objectValue("user")?.textValue("username")
                        ?: donation?.textValue("username")
                        ?: tip.textValue("username").orEmpty(),
                amountText =
                    listOfNotNull(
                        donation?.textValue("amount"),
                        donation?.textValue("currency"),
                    ).joinToString(" "),
                message = donation?.textValue("message").orEmpty(),
                timestampEpochMs = parseTimestamp(tip.textValue("createdAt")),
            )
        }
    }

    private suspend fun httpGet(
        url: String,
        bearer: String? = null,
    ): String? = withContext(dispatchersProvider.io) {
        runCatching {
            val request =
                Request
                    .Builder()
                    .url(url)
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "application/json, text/html, */*")
                    .apply { if (bearer != null) header("Authorization", "Bearer $bearer") }
                    .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body.string()
            }
        }.getOrElse {
            logger.debug(it) { "Donation request failed: $url" }
            null
        }
    }

    private fun extractQueryParam(
        url: String,
        name: String,
    ): String? {
        val query = url.substringAfter('?', missingDelimiterValue = "").substringBefore('#')
        return query.split('&').firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=')
            if (key.equals(name, ignoreCase = true)) part.substringAfter('=', "").takeIf { it.isNotBlank() } else null
        }
    }

    private fun JsonElement.objectValue(key: String): JsonElement? = (this as? JsonObject)?.get(key)

    private fun JsonElement.textValue(key: String): String? = (objectValue(key) as? JsonPrimitive)?.contentOrNull

    private fun parseTimestamp(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        for (pattern in TIMESTAMP_PATTERNS) {
            val parsed =
                runCatching {
                    SimpleDateFormat(pattern, Locale.US)
                        .apply { timeZone = TimeZone.getTimeZone("UTC") }
                        .parse(value)
                        ?.time
                }.getOrNull()
            if (parsed != null) return parsed
        }
        return 0L
    }

    private companion object {
        const val PAGE_SIZE = 30
        const val BROWSER_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        val DA_ACCESS_TOKEN_REGEX = Regex("""access_token\s*=\s*['"]([\w.\-]+)['"]""")
        val TIMESTAMP_PATTERNS =
            listOf(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd HH:mm:ss",
            )
    }
}
