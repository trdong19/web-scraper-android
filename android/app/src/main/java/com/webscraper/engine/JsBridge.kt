package com.webscraper.engine

import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * JavaScript interface injected into WebView for element selection communication.
 * Methods annotated with @JavascriptInterface are callable from JS via window.AndroidBridge.
 */
class JsBridge(
    private val onListSelected: (selector: String, count: Int, elementInfo: String) -> Unit,
    private val onFieldSelected: (fieldInfo: String, totalFields: Int) -> Unit,
    private val onElementClicked: (elementInfo: String) -> Unit
) {
    private val gson = Gson()

    @JavascriptInterface
    fun onListSelected(selector: String, count: Int, elementInfo: String) {
        onListSelected.invoke(selector, count, elementInfo)
    }

    @JavascriptInterface
    fun onFieldSelected(fieldInfo: String, totalFields: Int) {
        onFieldSelected.invoke(fieldInfo, totalFields)
    }

    @JavascriptInterface
    fun onElementClicked(elementInfo: String) {
        onElementClicked.invoke(elementInfo)
    }

    /**
     * Parse field info JSON from JS.
     */
    fun parseFieldInfo(json: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
