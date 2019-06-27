package de.smarthelios.jira.export

import de.smarthelios.atlassian.export.filter.HtmlFilter
import de.smarthelios.atlassian.export.filter.ReplacementFilter
import de.smarthelios.atlassian.io.Resource
import de.smarthelios.jira.export.convert.JiraHtml
import de.smarthelios.jira.export.model.ExportMeta
import de.smarthelios.jira.export.model.Issue
import de.smarthelios.jira.export.model.IssueRelation
import de.smarthelios.jira.export.model.Issues
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.xml.MarkupBuilder

/**
 * All methods producing HTML for JIRA exports.
 */
final class JiraExportHtml {
    private static final Map<String, String> MENU = [
            'index.html':'Index',
            'labels.html':'Labels'
    ]

    private static final SimpleTemplateEngine templateEngine = new SimpleTemplateEngine()
    private static final Template issueTpl = templateEngine.createTemplate(Resource.jiraExport('/template/issue.html'))
    private static final Template indexTpl = templateEngine.createTemplate(Resource.jiraExport('/template/index.html'))
    private static final Template labelsTpl = templateEngine.createTemplate(Resource.jiraExport('/template/labels.html'))

    private JiraExportHtml() {}

    static String htmlNoEpicTree(Issues issues) {
        StringWriter sw = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(sw)

        List<Issue> nonEpicIssues = issues.issues.findAll { !(it.issueType.epic || it.issueType.subTask || it.epicKey) }
        List<IssueRelation> relations = issues.getRelations(IssueRelation.Kind.SUB_TASK)

        if(nonEpicIssues) {
            builder.ul(class:'issues') {

                nonEpicIssues.each { issue ->

                    li(class: 'issue') {
                        yieldIssue(builder, issue)

                        def subTasks = relations.findAll { relation ->
                            relation.kind == IssueRelation.Kind.SUB_TASK && relation.fromIssue.key == issue.key
                        }.collect {
                            it.toIssue
                        }.sort {
                            it.key
                        }

                        if (subTasks) {
                            ul(class: 'subTasks') {
                                subTasks.each { subTask ->
                                    li(class: 'subTask') {
                                        yieldIssue(builder, subTask)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        sw.toString()
    }

    static void yieldIssue(MarkupBuilder builder, Issue issue) {
        builder.img(src:"$JiraExport.IMAGES_DIR/${issue.issueType.iconImage.exportFilename}")
        builder.mkp.yield(' ')
        builder.a(href: issue.exportFilename,"${issue.key} - ${issue.summary}")
    }

    static String htmlEpicTree(Issues issues) {
        StringWriter sw = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(sw)

        List<IssueRelation> relations = issues.relations

        builder.ul(class: 'epics') {
            issues.select(issues.epicKeys).sort { it.summary }.each { epic ->

                li(class: 'epic') {
                    yieldIssue(builder, epic)

                    def epicIssues = relations.findAll { relation ->
                        relation.kind == IssueRelation.Kind.EPIC && relation.fromIssue.key == epic.key
                    }.collect {
                        it.toIssue
                    }.sort {
                        it.key
                    }

                    if (epicIssues) {

                        ul(class: 'issues') {
                            epicIssues.each { issue ->

                                li(class: 'issue') {
                                    yieldIssue(builder, issue)

                                    def subTasks = relations.findAll { relation ->
                                        relation.kind == IssueRelation.Kind.SUB_TASK && relation.fromIssue.key == issue.key
                                    }.collect {
                                        it.toIssue
                                    }.sort {
                                        it.key
                                    }

                                    if (subTasks) {

                                        ul(class: 'subTasks') {
                                            subTasks.each { subTask ->
                                                li(class: 'subTask') {
                                                    yieldIssue(builder, subTask)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        def epicTree = sw.toString()
        epicTree
    }

    static String htmlIndexRemaining(Issues issues) {
        List<IssueRelation> relations = issues.getRelations(IssueRelation.Kind.SUB_TASK)

        List<String> relatedSubTaskKeys = relations.collect { it.toIssue.key }
        List<Issue> unrelatedSubTasks = issues.subTasks.findAll { !(it.key in relatedSubTaskKeys) }

        StringWriter sw = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(sw)

        if (unrelatedSubTasks) {

            builder.ul(class: 'subTasks') {
                unrelatedSubTasks.each { subTask ->
                    li(class: 'subTask') {
                        yieldIssue(builder, subTask)
                    }
                }
            }
        }

        sw.toString()
    }

    static String htmlIndex(List<Issue> issueList, ExportMeta exportMeta) {
        Issues issues = new Issues(issues: issueList)

        String epicTree = htmlEpicTree(issues)
        String nonEpicTree = htmlNoEpicTree(issues)
        String remainingList = htmlIndexRemaining(issues)

        Map binding = [
                menu: MENU,
                stylesDir: JiraExport.STYLES_DIR,
                html: new JiraHtml(),
                exportMeta: exportMeta,
                epicTree: epicTree,
                noEpicTree: nonEpicTree,
                remainingList: remainingList
        ]
        indexTpl.make(binding).toString()
    }

    static String htmlLabelIndex(List<Issue> issueList) {
        Issues issues = new Issues(issues: issueList)

        List<String> labels = issues.labels

        StringWriter sw = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(sw)

        builder.ul {
            labels.each { label ->
                li(class:'index-label') {
                    span(class:'label', label)
                    ul(class:'issues') {
                        issues.issuesForLabel(label).each { issue ->

                            li(class:'issue') {
                                yieldIssue(builder, issue)
                            }

                        }
                    }

                }

            }

            li(class:'index-label non-labeled') {
                span(class:'label', '# unlabeled')
                ul(class:'issues') {
                    issues.withoutLabels.each { issue ->

                        li(class:'issue') {
                            yieldIssue(builder, issue)
                        }

                    }
                }
            }

        }
        String labelIndex = sw.toString()

        Map binding = [
                menu: MENU,
                stylesDir: JiraExport.STYLES_DIR,
                labelIndex: labelIndex
        ]
        labelsTpl.make(binding).toString()
    }

    static String htmlIssue(Issue issue, Issues issues, ExportMeta exportMeta = new ExportMeta()) {
        Map binding = [
                menu: MENU,
                issue:issue,
                issues: issues,
                exportMeta: exportMeta,
                html:new JiraHtml(),
                attachmentsDir: JiraExport.ATTACHMENTS_DIR,
                stylesDir: JiraExport.STYLES_DIR,
                imagesDir: JiraExport.IMAGES_DIR,
                htmlFilter: buildImageSrcFilter(issue)
        ]
        issueTpl.make(binding).toString()
    }

    static HtmlFilter buildImageSrcFilter(Issue issue) {
        Map<String, String> replacement = [:]

        issue.images.each { image ->
            replacement["src=\"${image.downloadUrl}\"".toString()] =
                    "src=\"${JiraExport.IMAGES_DIR}/${issue.key}/${image.exportFilename}\"".toString()
        }

        issue.comments.each { comment ->
            comment.images.each { image ->
                replacement["src=\"${image.downloadUrl}\"".toString()] =
                        "src=\"${JiraExport.IMAGES_DIR}/${issue.key}/${comment.id}/${image.exportFilename}\"".toString()
            }
        }

        new ReplacementFilter(replacement)
    }

}
