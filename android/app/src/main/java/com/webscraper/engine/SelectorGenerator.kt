package com.webscraper.engine

import com.webscraper.data.model.FieldConfig

/**
 * Generates optimized CSS selectors from visual selection results.
 */
object SelectorGenerator {

    /**
     * Analyze a list of similar elements and generate the best list selector.
     * Returns a CSS selector that matches all similar items.
     */
    fun generateListSelector(sampleCssSelector: String, matchedCount: Int): String {
        // The JS already generates a good selector, just return it
        return sampleCssSelector
    }

    /**
     * Generate a field selector relative to the list item.
     * If the absolute selector is "div.product > h2.title", and list is "div.product",
     * the relative selector should be "h2.title".
     */
    fun generateFieldSelector(
        absoluteSelector: String,
        listSelector: String
    ): String {
        // Remove the list selector prefix to get relative selector
        val relative = absoluteSelector
            .removePrefix(listSelector)
            .removePrefix(" > ")
            .removePrefix(">")
            .trim()

        return if (relative.isNotEmpty()) relative else absoluteSelector
    }

    /**
     * Auto-detect field type from sample text.
     */
    fun detectFieldType(sampleText: String, tagName: String, attributeName: String? = null): String {
        if (attributeName != null) return "attr"
        if (tagName == "img") return "attr"
        if (tagName == "a") return "attr"
        return "text"
    }

    /**
     * Auto-detect attribute name based on tag.
     */
    fun detectAttribute(tagName: String): String? {
        return when (tagName) {
            "img" -> "src"
            "a" -> "href"
            "input" -> "value"
            "video", "audio" -> "src"
            "source" -> "src"
            else -> null
        }
    }

    /**
     * Generate a human-readable field name from sample text or tag.
     */
    fun suggestFieldName(sampleText: String, tagName: String, index: Int): String {
        val cleanText = sampleText.trim().take(20)
        return when {
            tagName == "img" -> "image"
            tagName == "a" && cleanText.isNotBlank() -> "link"
            tagName == "a" -> "link_$index"
            cleanText.isNotBlank() -> "field_$index"
            else -> "field_$index"
        }
    }
}
