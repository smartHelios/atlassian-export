package de.smarthelios.jira.export.model

import de.smarthelios.atlassian.export.model.Image

/**
 * Decorator to ease the access to a list of issue types.
 */
class IssueTypes {
    List<IssueType> issueTypes

    IssueType forId(String id) {
        issueTypes.find { it.id == id }
    }

    List<Image> getImages() {
        issueTypes.collect {
            it.iconImage
        }
    }
}
