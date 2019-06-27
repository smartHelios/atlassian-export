package de.smarthelios.jira.export.model

/**
 * Model of a person representing an author, a commenter, a reporter etc.
 */
class Person {
    String key

    String displayName
    String emailAddress

    String getDisplayMail() {
        "$displayName <$emailAddress>"
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Person person = (Person) o

        if (displayName != person.displayName) return false
        if (emailAddress != person.emailAddress) return false
        if (key != person.key) return false

        return true
    }

    int hashCode() {
        int result
        result = (key != null ? key.hashCode() : 0)
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0)
        result = 31 * result + (emailAddress != null ? emailAddress.hashCode() : 0)
        return result
    }
}
