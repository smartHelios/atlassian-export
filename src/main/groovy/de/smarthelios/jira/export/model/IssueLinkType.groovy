package de.smarthelios.jira.export.model

/**
 * Issue link type model object describing one kind of available issue relations in JIRA.
 * see https://docs.atlassian.com/software/jira/docs/api/REST/latest/#api/2/issueLinkType
 */
class IssueLinkType {
    String id
    String name
    String inward
    String outward
}
