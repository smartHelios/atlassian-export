package de.smarthelios.jira.export.model

/**
 * Decorator for easier access to list of fields.
 */
class Fields {
    List<Field> fields

    String idFor(String name) {
        fields.find { it.name == name }?.id
    }
}
