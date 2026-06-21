package com.webscraper.engine

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * In-memory cookie jar for maintaining session state across requests.
 */
class MemoryCookieJar : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = url.host
        cookieStore.getOrPut(key) { mutableListOf() }.apply {
            // Remove old cookies with same name
            val newNames = cookies.map { it.name }.toSet()
            removeAll { it.name in newNames }
            addAll(cookies)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val key = url.host
        return cookieStore[key]?.filter { cookie ->
            !cookie.expiresAt.let { it < System.currentTimeMillis() }
        } ?: emptyList()
    }

    fun clear() {
        cookieStore.clear()
    }

    fun getCookiesForHost(host: String): List<Cookie> {
        return cookieStore[host] ?: emptyList()
    }
}
