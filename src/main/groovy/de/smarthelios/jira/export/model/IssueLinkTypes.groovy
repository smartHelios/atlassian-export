package de.smarthelios.jira.export.model

/**
 * Decorator to ease access to list of issue link types.
 */
class IssueLinkTypes {
    List<IssueLinkType> types

    IssueLinkType forId(String id) {
        types.find { it.id == id }
    }
}
