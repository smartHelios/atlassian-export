package de.smarthelios.jira.export.convert

import de.smarthelios.atlassian.export.convert.Html
import de.smarthelios.jira.export.model.Component
import de.smarthelios.jira.export.model.Issue
import de.smarthelios.jira.export.model.Person
import de.smarthelios.jira.export.model.Sprint

/**
 * JIRA HTML export specific formatting helpers.
 */
class JiraHtml extends Html {
    String personClass = 'person'
    String componentClass = 'component'
    String sprintClass = 'sprint'
    String issueClass = 'issue'

    String format(Person person, String cssClass = '') {
        "<a class=\"${cssClass ?: personClass}\" href=\"mailto:${person.emailAddress}\">${person.displayName}</a>"
    }

    String format(Sprint sprint, String cssClass = '') {
        "<a class=\"${cssClass ?: sprintClass}\" href=\"sprint.html#sprint-${sprint.id}\">${sprint.name}</a>"
    }

    String format(Component component, String cssClass = '') {
        "<span class=\"${cssClass ?: componentClass}\">${component.name}${component.description ? '(' + component.description + ')' : ''}</span>"
    }

    String format(Issue issue, String imagesDir, String baseUrl = '', String cssClass = '') {
        String href = baseUrl ? baseUrl + '/browse/' + issue.key : issue.exportFilename
        "<span class=\"${cssClass ?: issueClass}\"><img src=\"${imagesDir}/${issue.issueType.iconImage.exportFilename}\"/> <a href=\"${href}\">${issue.key} - ${issue.summary}</a></span>"
    }
}
