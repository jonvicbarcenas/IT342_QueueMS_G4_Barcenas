package edu.cit.barcenas.queuems.api

import edu.cit.barcenas.queuems.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

object BackendUrlProvider {
    val primaryBaseUrl: String = BuildConfig.QUEUE_MS_API_BASE_URL.ensureTrailingSlash()

    val fallbackBaseUrls: List<String> = BuildConfig.QUEUE_MS_API_FALLBACK_URLS
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { it.ensureTrailingSlash() }
        .filter { it != primaryBaseUrl }
        .distinct()

    val allBaseUrls: List<String> = listOf(primaryBaseUrl) + fallbackBaseUrls

    fun replaceBaseUrl(originalUrl: HttpUrl, baseUrl: String): HttpUrl {
        val parsedBase = baseUrl.toHttpUrl()
        return originalUrl.newBuilder()
            .scheme(parsedBase.scheme)
            .host(parsedBase.host)
            .port(parsedBase.port)
            .build()
    }

    private fun String.ensureTrailingSlash(): String {
        return if (endsWith("/")) this else "$this/"
    }
}
