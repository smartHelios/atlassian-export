package de.smarthelios.atlassian.export.filter

import org.apache.commons.lang3.StringUtils

/**
 * Simple filter which does String replacements in HTML strings.
 */
class ReplacementFilter implements HtmlFilter {

    private String[] searchList
    private String[] replacementList

    ReplacementFilter(Map<String, String> replacements) {
        this.searchList = replacements.keySet().toArray()
        this.replacementList = searchList.collect { replacements[it] }
    }

    ReplacementFilter(List<String> searchList, List<String> replacementList) {
        this.searchList = searchList.toArray()
        this.replacementList = replacementList.toArray()
    }

    @Override
    String apply(String pageHtml) {
        return StringUtils.replaceEach(pageHtml, searchList, replacementList)
    }
}
