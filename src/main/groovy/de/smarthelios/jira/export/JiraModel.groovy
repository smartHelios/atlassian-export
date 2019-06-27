package de.smarthelios.jira.export

import de.smarthelios.atlassian.export.AtlassianModel
import de.smarthelios.jira.export.model.Attachment
import de.smarthelios.jira.export.model.Comment
import de.smarthelios.jira.export.model.Component
import de.smarthelios.jira.export.model.Issue
import de.smarthelios.jira.export.model.IssueLink
import de.smarthelios.jira.export.model.IssueLinkType
import de.smarthelios.jira.export.model.IssueType
import de.smarthelios.jira.export.model.Person
import de.smarthelios.jira.export.model.Sprint

/**
 * JSON Serialization model for storing and loading JIRA specific model classes as raw data in exports.
 */
class JiraModel {

    static Map toMap(Attachment attachment) {
        [
                id: attachment.id,
                filename: attachment.filename,
                author: toMap(attachment.author),
                created: attachment.created,
                size: attachment.size,
                mimeType: attachment.mimeType,
                content: attachment.content,
                bytes: attachment.bytes ? attachment.bytes.encodeBase64().toString() : null
        ]
    }

    static Attachment attachmentFromMap(def attachmentMap) {
        if(attachmentMap) {
            new Attachment(
                    id: attachmentMap.id,
                    filename: attachmentMap.filename,
                    author: attachmentMap.author,
                    created: attachmentMap.created,
                    size: attachmentMap.size,
                    mimeType: attachmentMap.mimeType,
                    content: attachmentMap.content,
                    bytes: attachmentMap.bytes.decodeBase64()
            )
        }
        else {
            null
        }
    }

    static Map toMap(Comment comment) {
        [
                id: comment.id,
                author: toMap(comment.author),
                bodyHtml: comment.bodyHtml,
                updateAuthor: toMap(comment.updateAuthor),
                created: comment.created,
                updated: comment.updated,
                images: comment.images.collect { AtlassianModel.toMap(it) }
        ]
    }

    static Comment commentFromMap(def commentMap) {
        if(commentMap) {
            new Comment(
                    id: commentMap.id,
                    author: personFromMap(commentMap.author),
                    bodyHtml: commentMap.bodyHtml,
                    updateAuthor: personFromMap(commentMap.updateAuthor),
                    created: commentMap.created,
                    updated: commentMap.updated,
                    images: commentMap.images.collect { AtlassianModel.imageFromMap(it) }
            )
        }
        else {
            null
        }
    }

    static Map toMap(Component component) {
        [
                id: component.id,
                name: component.name,
                description: component.description
        ]
    }

    static Component componentFromMap(def componentMap) {
        if(componentMap) {
            new Component(
                    id: componentMap.id,
                    name: componentMap.name,
                    description: componentMap.description
            )
        }
        else {
            null
        }
    }

    static Map toMap(Issue issue) {
        [
                id: issue.id,
                key: issue.key,
                //details
                issueType: issue.issueType ? toMap(issue.issueType) : null,
                summary: issue.summary,
                components: issue.components.collect { toMap(it) },
                labels: issue.labels,
                design: issue.design,
                epicKey: issue.epicKey,
                sprints: issue.sprints.collect { toMap(it) },
                changeEvaluation: issue.changeEvaluation,
                riskEvaluation: issue.riskEvaluation,
                // attachments
                attachments: issue.attachments.collect { toMap(it) },
                // comments
                comments: issue.comments.collect { toMap(it) },
                // people
                reporter: issue.reporter ? toMap(issue.reporter) : null,
                // dates
                created: issue.created,
                updated: issue.updated,
                descriptionHtml: issue.descriptionHtml,
                // relations
                subTasks: issue.subTasks.collect { toMap(it) },
                issueLinks: issue.issueLinks.collect { toMap(it) },
                // images
                images: issue.images.collect { AtlassianModel.toMap(it) }
        ]
    }

    static Issue issueFromMap(def issueMap) {
        if(issueMap) {
            new Issue(
                    id: issueMap.id,
                    key: issueMap.key,
                    //details
                    issueType: issueTypeFromMap(issueMap.issueType),
                    summary: issueMap.summary,
                    components: issueMap.components ? issueMap.components.collect { componentFromMap(it) } : [],
                    labels: issueMap.labels,
                    design: issueMap.design,
                    epicKey: issueMap.epicKey,
                    sprints: issueMap.sprints ? issueMap.sprints.collect { sprintFromMap(it) } : [],
                    changeEvaluation: issueMap.changeEvaluation,
                    riskEvaluation: issueMap.riskEvaluation,
                    // attachments
                    attachments: issueMap.attachments ? issueMap.attachments.collect { attachmentFromMap(it) } : [],
                    // comments
                    comments: issueMap.comments ? issueMap.comments.collect { commentFromMap(it) } : [],
                    // people
                    reporter: personFromMap(issueMap.reporter),
                    // dates
                    created: issueMap.created,
                    updated: issueMap.updated,
                    descriptionHtml: issueMap.descriptionHtml,
                    // relations
                    subTasks: issueMap.subTasks ? issueMap.subTasks.collect { issueFromMap(it) } : [],
                    issueLinks: issueMap.issueLinks ? issueMap.issueLinks.collect { issueLinkFromMap(it) } : [],
                    // images
                    images: issueMap.images.collect { AtlassianModel.imageFromMap(it) }
            )
        }
        else {
            null
        }
    }

    static Map toMap(IssueLink issueLink) {
        [
                id: issueLink.id,
                type: issueLink.type ? toMap(issueLink.type) : null,
                outwardIssueKey: issueLink.outwardIssueKey,
                inwardIssueKey: issueLink.inwardIssueKey,
                outward: issueLink.outward ? toMap(issueLink.outward) : null,
                inward: issueLink.inward ? toMap(issueLink.inward) : null
        ]
    }

    static IssueLink issueLinkFromMap(def issueLinkMap) {
        if(issueLinkMap) {
            new IssueLink(
                    id: issueLinkMap.id,
                    type: issueLinkTypeFromMap(issueLinkMap.type),
                    outwardIssueKey: issueLinkMap.outwardIssueKey,
                    inwardIssueKey: issueLinkMap.inwardIssueKey,
                    outward: issueFromMap(issueLinkMap.outward),
                    inward: issueFromMap(issueLinkMap.inward)
            )
        }
        else {
            null
        }
    }

    static Map toMap(IssueLinkType issueLinkType) {
        [
                id: issueLinkType.id,
                name: issueLinkType.name,
                inward: issueLinkType.inward,
                outward: issueLinkType.outward
        ]
    }

    static IssueLinkType issueLinkTypeFromMap(def issueLinkTypeMap) {
        if(issueLinkTypeMap) {
            new IssueLinkType(
                    id: issueLinkTypeMap.id,
                    name: issueLinkTypeMap.name,
                    inward: issueLinkTypeMap.inward,
                    outward: issueLinkTypeMap.outward
            )
        }
        else {
            null
        }
    }

    static Map toMap(IssueType issueType) {
        [
                id: issueType.id,
                name: issueType.name,
                description: issueType.description,
                subtask: issueType.subtask,
                iconUrl: issueType.iconUrl,
                avatarId: issueType.avatarId,
                iconImage: AtlassianModel.toMap(issueType.iconImage)
        ]
    }

    static IssueType issueTypeFromMap(def issueTypeMap) {
        if(issueTypeMap) {
            new IssueType(
                    id: issueTypeMap.id,
                    name: issueTypeMap.name,
                    description: issueTypeMap.description,
                    subtask: issueTypeMap.subtask,
                    iconUrl: issueTypeMap.iconUrl,
                    avatarId: issueTypeMap.avatarId,
                    iconImage: AtlassianModel.imageFromMap(issueTypeMap.iconImage)
            )
        }
        else {
            null
        }
    }

    static Map toMap(Person person) {
        [
                key: person.key,
                displayName: person.displayName,
                emailAddress: person.emailAddress
        ]
    }

    static Person personFromMap(def personMap) {
        if(personMap) {
            new Person(
                    key: personMap.key,
                    displayName: personMap.displayName,
                    emailAddress: personMap.emailAddress
            )
        }
        else {
            null
        }
    }

    static Map toMap(Sprint sprint) {
        [
                id: sprint.id,
                name: sprint.name,
                state: sprint.state,
                startDate: sprint.startDate,
                endDate: sprint.endDate,
                completeDate: sprint.completeDate,
                rapidViewId: sprint.rapidViewId,
                goal: sprint.goal
        ]
    }

    static Sprint sprintFromMap(def sprintMap) {
        if(sprintMap) {
            new Sprint(
                    id: sprintMap.id,
                    name: sprintMap.name,
                    state: sprintMap.state,
                    startDate: sprintMap.startDate,
                    endDate: sprintMap.endDate,
                    completeDate: sprintMap.completeDate,
                    rapidViewId: sprintMap.rapidViewId,
                    goal: sprintMap.goal
            )
        }
        else {
            null
        }
    }
}
