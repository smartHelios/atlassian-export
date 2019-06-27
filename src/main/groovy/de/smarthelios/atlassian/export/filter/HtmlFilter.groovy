package de.smarthelios.atlassian.export.filter

/**
 * An HTMLFilter is an interface for filters applied to Atlassian generated export HTML. These are used to replace links
 * or sanitize content in output HTML.
 */
interface HtmlFilter {
    String apply(String pageHtml)
}
