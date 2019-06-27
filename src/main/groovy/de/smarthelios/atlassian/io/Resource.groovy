package de.smarthelios.atlassian.io

/**
 * Provides and loads resources like css, templates for the exports
 */
class Resource {

    static String atlassianExport(String path) {
        resolve('/atlassian/export' + path)
    }

    static String confluenceExport(String path) {
        resolve('/confluence/export' + path)
    }

    static String jiraExport(String path) {
        resolve('/jira/export' + path)
    }

    private static String resolve(String path) {
        Resource.class.getResourceAsStream('/de/smarthelios' + path).text
    }
}
