package de.smarthelios.jira.export.model

import de.smarthelios.atlassian.export.model.Image
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import java.util.regex.Pattern

/**
 * Model of a JIRA issue.
 */
@Slf4j
class Issue {
    static final Pattern EXPORTABLE_SUMMARY_REGEXP = ~/^(\w| |-|\.)+$/ //word characters, dots and spaces allowed

    String id
    String key
    //details
    IssueType issueType
    String summary
    List<Component> components
    List<String> labels
    String design
    String epicKey
    List<Sprint> sprints
    String changeEvaluation
    String riskEvaluation
    // attachments
    List<Attachment> attachments
    // comments
    List<Comment> comments
    // people
    Person reporter
    // dates
    Date created
    Date updated
    String descriptionHtml
    // relations
    List<Issue> subTasks
    List<IssueLink> issueLinks

    List<Image> images

    @Lazy
    Document queryDoc = Jsoup.parse(descriptionHtml)

    boolean isExportableSummary() {
        EXPORTABLE_SUMMARY_REGEXP.matcher(summary).matches()
    }

    String getExportFilename() {
        if(exportableSummary) {
            // we can replace everything we want in the summary for filename use
            // as uniqueness is guaranteed by prepending issue key
            "${key}_${StringUtils.replaceChars(summary, ' .', '__')}.html"
        }
        else {
            "${key}.html"
        }
    }

    List<String> getImageSources() {
        if(descriptionHtml) {
            queryDoc.select('img')
                    .collect { it.attr('src') }
                    // filter embedded images
                    .findAll { !it.startsWith('data:') }
        }
        else {
            log.info 'Trying to analyze empty descriptionHtml for issue {}', key

            []
        }
    }

    List<Image> getImages() {
        if(descriptionHtml) {
            // synchronize descriptionHtml and images list
            if(null == images || images.size() != imageSources.size()) {
                images = imageSources.collect { new Image(downloadUrl: it) }
            }

            images
        }
        else {
            log.info 'Cannot return images for issue with empty descriptionHtml [issueKey:{}]', key

            []
        }
    }
}
