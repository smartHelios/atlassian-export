package de.smarthelios.confluence.export.model

/**
 * Export meta data like export date and used query for Confluence exports.
 */
class ExportMeta {
    Date exportDate = new Date()
    String baseUrl
    List<SpaceKeyPageTitle> spaceKeyPageTitles
}
