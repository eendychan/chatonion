package com.flxrs.dankchat.ui.chat.image

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger("EbloAlbumRepository")

/**
 * Loads all image URLs of an eblo.id post. A post can be an album with several files;
 * the post page embeds every slide as /uploads/{shortCode}/{fileId}.ext, so the page HTML
 * is fetched once (per short code) and the slide URLs are extracted in document order.
 */
@Single
class EbloAlbumRepository(
    private val ktorClient: HttpClient,
) {
    private val cache = mutableMapOf<String, List<String>>()

    suspend fun loadAlbumImages(postUrl: String): List<String> {
        val shortCode = postUrl.extractShortCode() ?: return emptyList()
        cache[shortCode]?.let { return it }

        val images =
            runCatching {
                val html = ktorClient.get("https://eblo.id/$shortCode").bodyAsText()
                parseSlideImages(html, shortCode)
            }.getOrElse {
                logger.debug(it) { "Failed to load eblo.id post $shortCode" }
                emptyList()
            }
        cache[shortCode] = images
        return images
    }

    private fun parseSlideImages(
        html: String,
        shortCode: String,
    ): List<String> {
        val slideRegex = Regex("""/uploads/${Regex.escape(shortCode)}/[A-Za-z0-9_-]+(?:\.[A-Za-z0-9]+)+""")
        return slideRegex
            .findAll(html)
            .map { it.value }
            .distinct()
            .filter { path -> IMAGE_FILE_EXTENSIONS.any { path.lowercase().endsWith(it) } }
            .map { "https://eblo.id$it" }
            .toList()
    }

    private fun String.extractShortCode(): String? {
        val withoutScheme = substringAfter("://", missingDelimiterValue = "").takeIf { it.isNotBlank() } ?: return null
        val host = withoutScheme
            .substringBefore('/')
            .substringBefore(':')
            .lowercase()
            .removePrefix("www.")
        if (host != "eblo.id") return null
        val path = withoutScheme.substringAfter('/', "").substringBefore('?').substringBefore('#')
        return path.split('/').singleOrNull { it.isNotBlank() }
    }

    private companion object {
        val IMAGE_FILE_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".avif")
    }
}
