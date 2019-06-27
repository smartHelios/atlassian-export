package de.smarthelios.jira.export

import de.smarthelios.atlassian.export.model.Image
import de.smarthelios.atlassian.io.ImageCache
import de.smarthelios.atlassian.io.Resource
import de.smarthelios.jira.export.model.Attachment
import de.smarthelios.jira.export.model.Comment
import de.smarthelios.jira.export.model.Component
import de.smarthelios.jira.export.model.ExportMeta
import de.smarthelios.jira.export.model.Fields
import de.smarthelios.jira.export.model.Issue
import de.smarthelios.jira.export.model.IssueLink
import de.smarthelios.jira.export.model.Issues
import de.smarthelios.jira.export.model.Person
import de.smarthelios.jira.export.model.Sprint
import de.smarthelios.jira.io.JiraClient
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j

import static JiraExportHtml.htmlIndex
import static JiraExportHtml.htmlIssue
import static JiraExportHtml.htmlLabelIndex
import static de.smarthelios.atlassian.export.convert.Convert.toDate

/**
 * Export JIRA issues and their attachments, comments etc.
 */

@Slf4j
class JiraExport {

    static final String STYLES_DIR = 'styles'
    static final String ATTACHMENTS_DIR = 'attachments'
    static final String IMAGES_DIR = 'images'

    static final String[] CUSTOM_FIELD_NAMES = ['Design',
                                                'Epic Link',
                                                'Sprint',
                                                'Change Evaluation',
                                                'Risk Evaluation']

    private final JiraClient jiraClient

    private ExportState state

    JiraExport(JiraClient jiraClient) {
        this.jiraClient = jiraClient
    }

    boolean export(String jql, File dir) {
        if (dir.exists()) {
            log.error 'Export dir {} already exists. Will not export anything!', dir

            false
        }
        else {

            state = new ExportState()
            state.meta = new ExportMeta(exportJql: jql, baseUrl: jiraClient.baseUrl)

            state.issueLinkTypes = jiraClient.issueLinkTypes()

            Fields fields = jiraClient.fields()
            logFields(fields)

            state.issueTypes = jiraClient.issueTypes()
            jiraClient.downloadIssueTypeImages(state.issueTypes.issueTypes)
            state.meta.images.addAll(state.issueTypes.images)

            state.customFieldMap = buildCustomFieldMap(fields)
            state.queryFields = buildQueryFields(state.customFieldMap)
            state.expand = ['renderedFields', 'names', 'schema']

            List<Issue> issueList = findIssues(jql, state)
            logIssues(issueList)

            addMissingEpics(issueList, state)
            logIssues(issueList)

            jiraClient.downloadIssueAttachments(issueList)
            jiraClient.downloadIssueImages(issueList, new ImageCache())

            export(issueList, dir, state.meta)
        }
    }

    static boolean export(List<Issue> issues, File dir, ExportMeta exportMeta = new ExportMeta()) {
        if (dir.exists()) {
            log.error 'Export dir {} already exists. Will not export anything!', dir

            false
        }
        else {
            log.info 'Exporting jira content to dir {}', dir

            File attachmentsDir = new File(dir, ATTACHMENTS_DIR)
            File imagesDir = new File(dir, IMAGES_DIR)
            File cssDir = new File(dir, STYLES_DIR)

            dir.mkdirs()
            attachmentsDir.mkdirs()
            imagesDir.mkdirs()
            cssDir.mkdirs()

            generateStyles(cssDir)
            generateIndex(issues, dir, exportMeta)
            generateLabelIndex(issues, dir)

            exportJson(issues, dir)
            exportImages(exportMeta.images, imagesDir)
            exportAttachments(issues, attachmentsDir)
            exportIssueImages(issues, imagesDir)
            exportIssues(issues, dir, exportMeta)

            true
        }
    }

    static void generateStyles(File cssDir) {
        log.info 'Generating style files.'
        new File(cssDir, 'nav.css').text = Resource.atlassianExport('/style/nav.css')
    }

    static void generateIndex(List<Issue> issueList, File dir, ExportMeta exportMeta) {
        log.info 'Generating issue index.'
        new File(dir, 'index.html').text = htmlIndex(issueList, exportMeta)
    }

    static void generateLabelIndex(List<Issue> issueList, File dir) {
        log.info 'Generating issue index by labels.'
        new File(dir, 'labels.html').text = htmlLabelIndex(issueList)
    }

    static void exportJson(List<Issue> issues, File dir) {
        File jsonExportFile = new File(dir, 'atlassian-jira-export.json')
        jsonExportFile.text = JsonOutput.prettyPrint(
                JsonOutput.toJson(issues.collect { JiraModel.toMap(it) })
        )
    }

    static void exportImages(List<Image> images, File dir) {
        images.each { image ->
            log.info('Exporting image {}', image.downloadUrl)
            new File(dir, image.exportFilename).bytes = image.bytes
        }
    }

    static void exportAttachments(List<Issue> issues, File dir) {
        issues.each { issue ->
            File attachmentsDir = new File(dir, issue.key)
            attachmentsDir.mkdirs()

            issue.attachments.each { attachment ->
                log.info('Exporting attachment "{}" of issue "{}"', attachment.filename, issue.key)

                new File(attachmentsDir, attachment.filename).bytes = attachment.bytes
            }
        }
    }

    static void exportIssueImages(List<Issue> issues, File dir) {
        issues.each { issue ->

            File imagesDir = new File(dir, issue.key)
            if(issue.images) {
                imagesDir.mkdirs()

                issue.images.each { image ->
                    log.info('Exporting issue[key:{}] image {}', issue.key, image.downloadUrl)
                    new File(imagesDir, image.exportFilename).bytes = image.bytes
                }
            }

            exportCommentImages(issue.comments, imagesDir)
        }
    }

    static void exportCommentImages(List<Comment> comments, File dir) {
        comments.each { comment ->

            File imagesDir = new File(dir, comment.id)
            if(comment.images) {
                imagesDir.mkdirs()

                comment.images.each { image ->
                    log.info('Exporting comment[id:{}] image {}', comment.id, image.downloadUrl)
                    new File(imagesDir, image.exportFilename).bytes = image.bytes
                }
            }
        }
    }

    static void exportIssues(List<Issue> issues, File dir, ExportMeta exportMeta = new ExportMeta()) {
        Issues allIssues = new Issues(issues: issues)
        issues.each { issue ->
            log.info('Exporting issue {} - "{}"', issue.key, issue.summary)

            new File(dir, issue.exportFilename).text = htmlIssue(issue, allIssues, exportMeta)
        }
    }


    private static List<String> buildQueryFields(Map<String, String> customFieldMap) {
        List<String> queryFields = ['issuetype',
                                    'summary',
                                    'description',
                                    'components',
                                    'labels',
                                    'updated',
                                    'created',
                                    'reporter',
                                    'attachment',
                                    'comment',
                                    'issuelinks',
                                    'subtasks']
        CUSTOM_FIELD_NAMES.each { fieldName ->
            String fieldId = customFieldMap[fieldName]
            if(fieldId) {
                queryFields << fieldId
            }
            else {
                log.warn 'No field id to query for custom field named "{}".', fieldName
            }
        }

        queryFields
    }

    private static Map<String, String> buildCustomFieldMap(Fields fields) {
        CUSTOM_FIELD_NAMES.collectEntries { fieldName ->
            String fieldId = fields.idFor(fieldName)

            if(fieldId) {
                [fieldName, fieldId]
            }
            else {
                log.warn 'No field for custom field name "{}" found in fields.', fieldName
                []
            }
        }.findAll { it.value != null }
    }


    private List<Issue> findIssues(String jql, ExportState state) {
        def json = jiraClient.searchAll([
                jql: jql,
                fields: state.queryFields,
                expand: state.expand
        ])
        extractIssues(json, state)
    }

    static List<Issue> extractIssues(def jqlResult, ExportState state) {
        List<Issue> issues = []

        def cf = state.customFieldMap

        jqlResult.issues.each { issueJson ->

            String key = issueJson.key
            String summary = issueJson.fields.summary

            log.info('Got issue {} "{}"', key, summary)

            issues << new Issue(
                    id: issueJson.id,
                    key: key,
                    summary: summary,
                    issueType: state.issueTypes.forId(issueJson.fields.issuetype.id.toString()),
                    descriptionHtml: issueJson.renderedFields.description,
                    components: issueJson.fields.components.collect {
                        new Component(
                                id: it.id,
                                name: it.name,
                                description: it.description
                        )
                    },
                    labels: issueJson.fields.labels.collect { it.toString() },
                    design: issueJson.fields[cf.Design]?.value,
                    epicKey: issueJson.fields[cf.'Epic Link'],
                    sprints: issueJson.fields[cf.Sprint]?.collect { Sprint.fromUglySerialization(it.toString()) },
                    changeEvaluation: issueJson.fields[cf.'Change Evaluation']?.value,
                    riskEvaluation: issueJson.fields[cf.'Risk Evaluation']?.value,
                    updated: toDate(issueJson.fields.updated),
                    created: toDate(issueJson.fields.created),
                    reporter: new Person(
                            key: issueJson.fields.reporter.key,
                            displayName: issueJson.fields.reporter.displayName,
                            emailAddress: issueJson.fields.reporter.emailAddress
                    ),
                    attachments: issueJson.fields.attachment.collect {
                        new Attachment(
                                id: it.id,
                                filename: it.filename,
                                created: toDate(it.created),
                                author: new Person(
                                        key: it.author.key,
                                        displayName: it.author.displayName,
                                        emailAddress: it.author.emailAddress
                                ),
                                size: it.size,
                                mimeType: it.mimeType,
                                content: it.content
                        )
                    },
                    comments: issueJson.fields.comment.comments.withIndex().collect { it, index ->

                        String renderedBody = issueJson.renderedFields.comment.comments[index].body

                        new Comment(
                                id: it.id,
                                author: new Person(
                                        key: it.author.key,
                                        displayName: it.author.displayName,
                                        emailAddress: it.author.emailAddress
                                ),
                                bodyHtml: renderedBody,
                                updateAuthor: new Person(
                                        key: it.updateAuthor.key,
                                        displayName: it.updateAuthor.displayName,
                                        emailAddress: it.updateAuthor.emailAddress
                                ),
                                created: toDate(it.created),
                                updated: toDate(it.updated)
                        )
                    },
                    subTasks: issueJson.fields.subtasks.collect { subTask ->
                        new Issue(
                                id: subTask.id,
                                key: subTask.key,
                                summary: subTask.fields.summary,
                                issueType: state.issueTypes.forId(subTask.fields.issuetype.id.toString())
                        )
                    },
                    issueLinks: issueJson.fields.issuelinks.collect { link ->
                        new IssueLink(
                                id: link.id,
                                type: state.issueLinkTypes.forId(link.type.id.toString()),
                                inwardIssueKey: link.inwardIssue?.key,
                                outwardIssueKey: link.outwardIssue?.key,
                                inward: link.inwardIssue ? new Issue(
                                        id: link.inwardIssue.id,
                                        key: link.inwardIssue.key,
                                        summary: link.inwardIssue.fields.summary,
                                        issueType: state.issueTypes.forId(link.inwardIssue.fields.issuetype.id.toString())
                                ) : null,
                                outward: link.outwardIssue ? new Issue(
                                        id: link.outwardIssue.id,
                                        key: link.outwardIssue.key,
                                        summary: link.outwardIssue.fields.summary,
                                        issueType: state.issueTypes.forId(link.outwardIssue.fields.issuetype.id.toString())
                                ) : null
                        )
                    }
            )
        }

        issues
    }

    private void addMissingEpics(List<Issue> issueList, ExportState state) {
        Issues issues = new Issues(issues: issueList)
        List<String> missingEpics = issues.missingEpics
        if (missingEpics) {
            log.info('Adding missing epics: {}', missingEpics.join(', '))

            String missingEpicsJql = "key in (${missingEpics.join(', ')})"
            def missingEpicsJson = jiraClient.searchAll(
                    jql: missingEpicsJql,
                    fields: state.queryFields,
                    expand: state.expand
            )
            issueList.addAll(extractIssues(missingEpicsJson, state))
        }
        else {
            log.info('No missing epics found.')
        }
        assert issues.missingEpics.size() == 0, 'There shall not be any missing epic here.'
    }

    static void logIssues(List<Issue> issues) {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        issues.each {
            pw.println "${it.key.padRight(6)} : ${it.summary} [${it.reporter.key} / ${it.reporter.displayName} / ${it.reporter.emailAddress}]"
        }
        pw.println('-' * 42)
        pw.println(issues.size() + (issues.size() > 1 ? ' issues' : ' issue'))

        pw.flush()
        log.debug sw.toString()
    }

    static void logFields(Fields fields) {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        pw.println 'ID'.padRight(30,'.') + ' : NAME'
        fields.fields.each {
            pw.println "${it.id.padRight(30, '.')} : ${it.name}"
        }

        pw.flush()
        log.debug sw.toString()
    }



}
