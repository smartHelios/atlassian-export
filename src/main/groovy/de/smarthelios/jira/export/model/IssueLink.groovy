package de.smarthelios.jira.export.model

/**
 * Model of issue links describing relations between issue like "duplicates", "relates" etc.
 */
class IssueLink {
    String id
    IssueLinkType type
    String outwardIssueKey
    String inwardIssueKey
    Issue outward
    Issue inward
}
