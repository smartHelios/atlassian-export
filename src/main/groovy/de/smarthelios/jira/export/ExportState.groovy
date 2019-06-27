package de.smarthelios.jira.export

import de.smarthelios.jira.export.model.ExportMeta
import de.smarthelios.jira.export.model.IssueLinkTypes
import de.smarthelios.jira.export.model.IssueTypes

/**
 * Holds data while export. Extracting state to this class makes it easy to reset state in JiraExport
 */
class ExportState {
    IssueTypes issueTypes
    IssueLinkTypes issueLinkTypes
    Map<String,String> customFieldMap
    List<String> queryFields
    List<String> expand

    ExportMeta meta

}
