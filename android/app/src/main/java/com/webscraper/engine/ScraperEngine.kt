package com.webscraper.engine

import com.webscraper.data.model.FieldConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Core scraping engine — fetches pages and extracts data.
 * Two modes: static (OkHttp) and dynamic (WebView).
 */
class ScraperEngine(
    private val proxyHost: String? = null,
    private val proxyPort: Int = 0
) {
    private val cookieJar = MemoryCookieJar()

    private val client: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(cookieJar)

        // Proxy
        if (!proxyHost.isNullOrBlank() && proxyPort > 0) {
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
        }

        builder.build()
    }

    /**
     * Fetch page HTML via OkHttp (static, no JS rendering).
     */
    suspend fun fetchStatic(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val headers = AntiCrawlManager.buildHeaders(referer = url)
            val requestBuilder = Request.Builder().url(url).get()
            headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extract data from HTML using CSS/XPath selectors.
     */
    fun extractData(
        html: String,
        listSelector: String,
        fields: List<FieldConfig>,
        selectorType: String = "css"
    ): List<Map<String, String?>> {
        return HtmlParser.extract(html, listSelector, fields, selectorType)
    }

    /**
     * Full pipeline: fetch + extract.
     */
    suspend fun scrape(
        url: String,
        listSelector: String,
        fields: List<FieldConfig>,
        selectorType: String = "css"
    ): Result<List<Map<String, String?>>> {
        // Apply anti-crawl delay
        AntiCrawlManager.applyDelay()

        return fetchStatic(url).map { html ->
            extractData(html, listSelector, fields, selectorType)
        }
    }

    fun clearCookies() = cookieJar.clear()
}
