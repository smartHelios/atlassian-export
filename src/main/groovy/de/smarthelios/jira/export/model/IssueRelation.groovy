package de.smarthelios.jira.export.model

/**
 * Models a generic relation between issues. It is a generic view which has no associated
 * implementation on the side of the JIRA REST API.
 */
class IssueRelation {

    final enum Kind { ISSUE_LINK, SUB_TASK, EPIC }

    final Issue fromIssue
    final Issue toIssue
    final Kind kind
    IssueLink issueLink

    IssueRelation(Issue fromIssue, Kind kind, Issue toIssue, IssueLink issueLink = null) {
        this.fromIssue = fromIssue
        this.toIssue = toIssue
        this.kind = kind
        this.issueLink = issueLink
    }
}
