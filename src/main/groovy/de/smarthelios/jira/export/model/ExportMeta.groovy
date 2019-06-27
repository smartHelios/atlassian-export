package de.smarthelios.jira.export.model

import de.smarthelios.atlassian.export.model.Image

/**
 * Export meta data like export date and used query for JIRA exports.
 */
class ExportMeta {
    Date exportDate = new Date()
    String exportJql = 'no JQL provided'
    String baseUrl

    List<Image> images = []
}
