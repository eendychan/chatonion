package com.flxrs.dankchat.ui.chat.image

import androidx.compose.runtime.Immutable

/**
 * A chat link that resolves to a displayable image.
 * [url] is the original link from the message, [imageUrl] is the direct image URL to load.
 */
@Immutable
data class ImageLinkUi(
    val url: String,
    val imageUrl: String,
)

/**
 * Resolves chat links to direct image URLs, modeled after chatterino67's Ebloid support.
 * Supported:
 * - eblo.id/{postId} (via its download endpoint)
 * - s-ul.eu, kappa.lol, gachi.gay file links (served directly)
 * - imgur.com/{id} and imgur.com/gallery/{id} (via i.imgur.com)
 * - any link whose path ends in a common image extension
 */
object ChatImageLinkResolver {
    private val IMAGE_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".avif")
    private val DIRECT_IMAGE_HOSTS = setOf("s-ul.eu", "kappa.lol", "gachi.gay")
    private val EBLOID_POST_ID = Regex("[A-Za-z0-9_-]{6,16}")
    private val EBLOID_RESERVED_SECTIONS = setOf("videos", "clips", "about", "explore", "search", "settings", "login", "register", "upload", "users")

    fun resolve(url: String): String? {
        val withoutScheme = url.substringAfter("://", missingDelimiterValue = "").takeIf { it.isNotBlank() } ?: return null
        val host = withoutScheme
            .substringBefore('/')
            .substringBefore(':')
            .lowercase()
            .removePrefix("www.")
        val path = withoutScheme.substringAfter('/', "").substringBefore('?').substringBefore('#')
        if (host.isBlank()) return null

        return when {
            host == "eblo.id" -> resolveEbloid(path)
            host == "imgur.com" || host == "m.imgur.com" -> resolveImgur(path)
            host == "i.imgur.com" -> url.takeIf { path.isNotBlank() }
            host in DIRECT_IMAGE_HOSTS -> url.takeIf { path.isNotBlank() && !path.startsWith("api/") }
            IMAGE_EXTENSIONS.any { path.lowercase().endsWith(it) } -> url
            else -> null
        }
    }

    // https://eblo.id/{postId} -> https://eblo.id/download/file/{postId}
    // Post ids are generated base62-like tokens (e.g. RJQC8hq). Site sections (eblo.id/videos,
    // chat.eblo.id) and user profiles (eblo.id/@name) must not be treated as media.
    private fun resolveEbloid(path: String): String? {
        val postId = path.split('/').filter { it.isNotBlank() }.singleOrNull() ?: return null
        if (postId.startsWith("@")) return null
        if (postId.lowercase() in EBLOID_RESERVED_SECTIONS) return null
        if (!EBLOID_POST_ID.matches(postId)) return null
        // generated ids practically always contain an uppercase letter or a digit,
        // lowercase-only words like "videos" are site sections
        if (postId.none { it.isUpperCase() || it.isDigit() }) return null
        return "https://eblo.id/download/file/$postId"
    }

    // https://imgur.com/{id} or https://imgur.com/gallery/{id} -> https://i.imgur.com/{id}.png
    private fun resolveImgur(path: String): String? {
        val segments = path.split('/').filter { it.isNotBlank() }
        val id =
            when {
                segments.size == 1 -> segments[0]
                segments.size == 2 && (segments[0] == "gallery" || segments[0] == "a") -> segments[1]
                else -> return null
            }
        val imageId = id.substringBeforeLast('.')
        if (imageId.isBlank()) return null
        return "https://i.imgur.com/$imageId.png"
    }
}
