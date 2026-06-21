package com.webscraper.engine

import com.webscraper.data.model.FieldConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * HTML parser using Jsoup for CSS selectors.
 */
object HtmlParser {

    fun extract(
        html: String,
        listSelector: String,
        fields: List<FieldConfig>,
        selectorType: String = "css"
    ): List<Map<String, String?>> {
        if (selectorType == "xpath") {
            return extractWithXPath(html, listSelector, fields)
        }
        return extractWithCss(html, listSelector, fields)
    }

    private fun extractWithCss(
        html: String,
        listSelector: String,
        fields: List<FieldConfig>
    ): List<Map<String, String?>> {
        val doc = Jsoup.parse(html)
        val items = doc.select(listSelector)
        val results = mutableListOf<Map<String, String?>>()

        for (item in items) {
            val row = mutableMapOf<String, String?>()
            for (field in fields) {
                try {
                    val el = item.selectFirst(field.selector)
                    row[field.name] = when {
                        el == null -> null
                        field.type == "attr" && field.attribute != null -> el.attr(field.attribute)
                        field.type == "html" -> el.html()
                        else -> el.text().trim()
                    }
                } catch (e: Exception) {
                    row[field.name] = null
                }
            }
            results.add(row)
        }
        return results
    }

    /**
     * Simple XPath support via Jsoup's sibling/parent traversal.
     * For complex XPath, converts common patterns to CSS.
     */
    private fun extractWithXPath(
        html: String,
        listSelector: String,
        fields: List<FieldConfig>
    ): List<Map<String, String?>> {
        // Jsoup doesn't support XPath natively.
        // Convert common XPath patterns to CSS selectors as fallback.
        val cssSelector = xpathToCss(listSelector)
        val convertedFields = fields.map { field ->
            if (field.selector.startsWith("//") || field.selector.startsWith(".//")) {
                field.copy(selector = xpathToCss(field.selector))
            } else field
        }
        return extractWithCss(html, cssSelector, convertedFields)
    }

    /**
     * Convert simple XPath expressions to CSS selectors.
     * Handles common patterns like //div[@class='xxx'], //h2, etc.
     */
    fun xpathToCss(xpath: String): String {
        var css = xpath
            .replace("//", " ")
            .replace("./", " > ")
            .replace("/@", "")

        // Handle [@class='xxx']
        css = Regex("""\[@class='([^']+)'\]""").replace(css) { matchResult ->
            ".${matchResult.groupValues[1].replace(" ", ".")}"
        }

        // Handle [@id='xxx']
        css = Regex("""\[@id='([^']+)'\]""").replace(css) { matchResult ->
            "#${matchResult.groupValues[1]}"
        }

        // Handle [position()=N]
        css = Regex("""\[position\(\)=(\d+)\]""").replace(css) { matchResult ->
            ":nth-child(${matchResult.groupValues[1]})"
        }

        return css.trim()
    }
}
