package com.webscraper.engine

/**
 * JavaScript code injected into WebView for visual element selection.
 */
object InjectedJS {

    /**
     * Initialize the visual selector overlay and event listeners.
     * Call this after page load.
     */
    fun initSelector(): String = """
        (function() {
            if (window.__scraperInitialized) return;
            window.__scraperInitialized = true;
            window.__selectorMode = 'browse'; // browse | pickList | pickField
            window.__selectedListSelector = '';
            window.__selectedFields = [];

            // Create overlay container
            var overlay = document.createElement('div');
            overlay.id = '__scraper_overlay';
            overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:999999;';
            document.body.appendChild(overlay);

            // Style for highlighted elements
            var style = document.createElement('style');
            style.textContent = `
                .__scraper-highlight {
                    outline: 3px solid #2196F3 !important;
                    outline-offset: 2px !important;
                    background-color: rgba(33, 150, 243, 0.15) !important;
                    pointer-events: auto !important;
                    cursor: crosshair !important;
                }
                .__scraper-highlight-list {
                    outline: 3px solid #FF9800 !important;
                    outline-offset: 2px !important;
                    background-color: rgba(255, 152, 0, 0.15) !important;
                    pointer-events: auto !important;
                    cursor: crosshair !important;
                }
                .__scraper-highlight-field {
                    outline: 3px solid #4CAF50 !important;
                    outline-offset: 2px !important;
                    background-color: rgba(76, 175, 80, 0.2) !important;
                    pointer-events: auto !important;
                    cursor: crosshair !important;
                }
                .__scraper-selected-field {
                    outline: 3px solid #E91E63 !important;
                    outline-offset: 2px !important;
                    background-color: rgba(233, 30, 99, 0.2) !important;
                }
                .__scraper-tooltip {
                    position: fixed;
                    background: rgba(0,0,0,0.85);
                    color: white;
                    padding: 6px 12px;
                    border-radius: 4px;
                    font-size: 12px;
                    z-index: 1000000;
                    pointer-events: none;
                    max-width: 300px;
                    word-break: break-all;
                }
            `;
            document.head.appendChild(style);

            // Tooltip
            var tooltip = document.createElement('div');
            tooltip.className = '__scraper-tooltip';
            tooltip.style.display = 'none';
            document.body.appendChild(tooltip);

            // Helper: generate CSS selector for an element
            window.__generateSelector = function(el) {
                if (el.id) return '#' + CSS.escape(el.id);

                var path = [];
                var current = el;
                while (current && current !== document.body) {
                    var seg = current.tagName.toLowerCase();
                    if (current.id) {
                        path.unshift('#' + CSS.escape(current.id));
                        break;
                    }
                    if (current.className && typeof current.className === 'string') {
                        var classes = current.className.trim().split(/\s+/)
                            .filter(c => !c.startsWith('__scraper'))
                            .map(c => '.' + CSS.escape(c));
                        if (classes.length > 0) {
                            seg += classes.join('');
                        }
                    }
                    // Add nth-child for disambiguation
                    var parent = current.parentElement;
                    if (parent) {
                        var siblings = Array.from(parent.children).filter(c => c.tagName === current.tagName);
                        if (siblings.length > 1) {
                            var idx = siblings.indexOf(current) + 1;
                            seg += ':nth-of-type(' + idx + ')';
                        }
                    }
                    path.unshift(seg);
                    current = current.parentElement;
                }
                return path.join(' > ');
            };

            // Helper: generate XPath for an element
            window.__generateXPath = function(el) {
                var path = [];
                var current = el;
                while (current && current !== document.body) {
                    var seg = current.tagName.toLowerCase();
                    var parent = current.parentElement;
                    if (parent) {
                        var siblings = Array.from(parent.children).filter(c => c.tagName === current.tagName);
                        if (siblings.length > 1) {
                            var idx = siblings.indexOf(current) + 1;
                            seg += '[' + idx + ']';
                        }
                    }
                    path.unshift(seg);
                    current = current.parentElement;
                }
                return '//' + path.join('/');
            };

            // Helper: find similar elements (same tag + same classes)
            window.__findSimilar = function(el) {
                var tag = el.tagName;
                var classes = Array.from(el.classList).filter(c => !c.startsWith('__scraper'));
                if (classes.length === 0) {
                    // Fall back to parent's children with same tag
                    var parent = el.parentElement;
                    if (parent) {
                        return Array.from(parent.children).filter(c => c.tagName === tag);
                    }
                    return [el];
                }
                var selector = classes.map(c => '.' + CSS.escape(c)).join('');
                return Array.from(document.querySelectorAll(tag + selector));
            };

            // Helper: get element info
            window.__getElementInfo = function(el) {
                return {
                    tag: el.tagName.toLowerCase(),
                    classes: Array.from(el.classList).filter(c => !c.startsWith('__scraper')).join(' '),
                    id: el.id || '',
                    text: (el.textContent || '').trim().substring(0, 100),
                    cssSelector: window.__generateSelector(el),
                    xpath: window.__generateXPath(el)
                };
            };

            // Click handler
            document.addEventListener('click', function(e) {
                if (window.__selectorMode === 'browse') return;

                e.preventDefault();
                e.stopPropagation();

                var el = e.target;
                if (el.id === '__scraper_overlay' || el.classList.contains('__scraper-tooltip')) return;

                var info = window.__getElementInfo(el);

                if (window.__selectorMode === 'pickList') {
                    // Find all similar elements and highlight them
                    var similar = window.__findSimilar(el);
                    window.__clearHighlights();

                    similar.forEach(function(item) {
                        item.classList.add('__scraper-highlight-list');
                    });

                    // Use parent as list container if it has multiple similar children
                    var listSelector = info.cssSelector;
                    window.__selectedListSelector = listSelector;
                    window.__matchedCount = similar.length;

                    // Notify Android
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onListSelected(listSelector, similar.length, JSON.stringify(info));
                    }
                } else if (window.__selectorMode === 'pickField') {
                    el.classList.add('__scraper-selected-field');

                    var fieldName = 'field_' + (window.__selectedFields.length + 1);
                    var fieldInfo = {
                        name: fieldName,
                        selector: info.cssSelector,
                        type: 'text',
                        sampleText: info.text
                    };
                    window.__selectedFields.push(fieldInfo);

                    // Notify Android
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onFieldSelected(JSON.stringify(fieldInfo), window.__selectedFields.length);
                    }
                }

                return false;
            }, true);

            // Hover handler — show tooltip
            document.addEventListener('mouseover', function(e) {
                if (window.__selectorMode === 'browse') return;
                var el = e.target;
                if (el.id === '__scraper_overlay' || el.classList.contains('__scraper-tooltip')) return;

                el.classList.add('__scraper-highlight');

                var info = window.__getElementInfo(el);
                tooltip.textContent = '<' + info.tag + '> ' + (info.text.substring(0, 50) || '(empty)');
                tooltip.style.display = 'block';
                tooltip.style.left = Math.min(e.clientX + 10, window.innerWidth - 300) + 'px';
                tooltip.style.top = (e.clientY - 30) + 'px';
            });

            document.addEventListener('mouseout', function(e) {
                if (window.__selectorMode === 'browse') return;
                e.target.classList.remove('__scraper-highlight');
                tooltip.style.display = 'none';
            });

            // Clear all highlights
            window.__clearHighlights = function() {
                document.querySelectorAll('.__scraper-highlight-list, .__scraper-highlight-field, .__scraper-highlight, .__scraper-selected-field')
                    .forEach(el => {
                        el.classList.remove('__scraper-highlight-list', '__scraper-highlight-field', '__scraper-highlight', '__scraper-selected-field');
                    });
            };

            // Set mode
            window.__setMode = function(mode) {
                window.__selectorMode = mode;
                window.__clearHighlights();
                if (mode === 'browse') {
                    document.body.style.cursor = '';
                } else {
                    document.body.style.cursor = 'crosshair';
                }
            };

            // Get current state
            window.__getState = function() {
                return JSON.stringify({
                    mode: window.__selectorMode,
                    listSelector: window.__selectedListSelector,
                    matchedCount: window.__matchedCount || 0,
                    fields: window.__selectedFields
                });
            };

            // Reset
            window.__resetSelection = function() {
                window.__clearHighlights();
                window.__selectedListSelector = '';
                window.__selectedFields = [];
                window.__matchedCount = 0;
            };

            console.log('[Scraper] Visual selector initialized');
        })();
    """.trimIndent()

    /** Switch to list picking mode */
    fun setPickListMode(): String = "window.__setMode('pickList');"

    /** Switch to field picking mode */
    fun setPickFieldMode(): String = "window.__setMode('pickField');"

    /** Switch back to browse mode */
    fun setBrowseMode(): String = "window.__setMode('browse');"

    /** Clear all highlights */
    fun clearHighlights(): String = "window.__clearHighlights();"

    /** Reset all selection state */
    fun resetSelection(): String = "window.__resetSelection();"

    /** Get current selection state as JSON */
    fun getState(): String = "window.__getState();"

    /** Highlight elements matching a CSS selector */
    fun highlightSelector(selector: String, color: String = "list"): String {
        val className = when (color) {
            "field" -> "__scraper-highlight-field"
            "selected" -> "__scraper-selected-field"
            else -> "__scraper-highlight-list"
        }
        return """
            (function() {
                window.__clearHighlights();
                try {
                    var els = document.querySelectorAll('$selector');
                    els.forEach(function(el) { el.classList.add('$className'); });
                    return els.length;
                } catch(e) { return 0; }
            })();
        """.trimIndent()
    }
}
