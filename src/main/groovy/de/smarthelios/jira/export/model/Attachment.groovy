package de.smarthelios.jira.export.model

/**
 * A JIRA issue attachment.
 */
class Attachment {
    String id
    String filename
    Person author
    Date created
    long size
    String mimeType
    String content //url

    // TODO can be critical for large projects with big or lots of attachments
    byte[] bytes
}
