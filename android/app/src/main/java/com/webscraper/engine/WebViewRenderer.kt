package com.webscraper.engine

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Uses Android WebView to render JS-heavy pages and extract the final HTML.
 * Runs headlessly (not visible to user) for background scraping.
 */
class WebViewRenderer(private val context: Context) {

    /**
     * Load a URL in a headless WebView, wait for JS execution, return rendered HTML.
     */
    suspend fun renderPage(
        url: String,
        waitSeconds: Int = 3,
        scrollToBottom: Boolean = false
    ): Result<String> = withContext(Dispatchers.Main) {
        try {
            val htmlDeferred = CompletableDeferred<String>()
            val webView = WebView(context)

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = userAgentString.replace("wv", "")
                blockNetworkImage = true  // Skip images for faster loading
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Wait for JS to execute
                    view?.postDelayed({
                        if (scrollToBottom) {
                            // Scroll to bottom to trigger lazy loading
                            view.evaluateJavascript(
                                "(function() { window.scrollTo(0, document.body.scrollHeight); })();"
                            ) {
                                view.postDelayed({
                                    extractHtml(view, htmlDeferred)
                                }, 2000)
                            }
                        } else {
                            extractHtml(view, htmlDeferred)
                        }
                    }, (waitSeconds * 1000).toLong())
                }
            }

            webView.loadUrl(url)

            val html = withTimeout(30_000) { htmlDeferred.await() }
            webView.destroy()
            Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractHtml(webView: WebView, deferred: CompletableDeferred<String>) {
        webView.evaluateJavascript(
            "(function() { return document.documentElement.outerHTML; })();"
        ) { html ->
            val cleaned = html
                ?.removeSurrounding("\"")
                ?.replace("\\u003C", "<")
                ?.replace("\\u003E", ">")
                ?.replace("\\\"", "\"")
                ?.replace("\\n", "\n")
                ?.replace("\\t", "\t")
                ?: ""
            deferred.complete(cleaned)
        }
    }
}
