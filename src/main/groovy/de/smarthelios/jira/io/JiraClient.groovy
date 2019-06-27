package de.smarthelios.jira.io

import de.smarthelios.atlassian.export.convert.Convert
import de.smarthelios.atlassian.export.model.Image
import de.smarthelios.atlassian.io.Gif
import de.smarthelios.atlassian.io.HttpClient
import de.smarthelios.atlassian.io.ImageCache
import de.smarthelios.atlassian.io.MimeTypeBytes
import de.smarthelios.jira.export.model.Attachment
import de.smarthelios.jira.export.model.Comment
import de.smarthelios.jira.export.model.Field
import de.smarthelios.jira.export.model.Fields
import de.smarthelios.jira.export.model.Issue
import de.smarthelios.jira.export.model.IssueLinkType
import de.smarthelios.jira.export.model.IssueLinkTypes
import de.smarthelios.jira.export.model.IssueType
import de.smarthelios.jira.export.model.IssueTypes
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

/**
 * JIRA specific client to access JIRA REST API.
 */
@Slf4j
class JiraClient extends HttpClient{

    static final String CONTENT_TYPE_JSON = ContentType.APPLICATION_JSON.toString()

    // green pixel gif
    static final MimeTypeBytes DRY_RUN_BYTES = new MimeTypeBytes(
            bytes: Gif.TEXT_DRY_RUN.bytes,
            mimeType:ContentType.IMAGE_GIF.mimeType
    )

    JsonSlurper slurper = new JsonSlurper()

    // fakes downloads for faster execution
    boolean dryRun = false


    JiraClient(String hostname, String username, String password, int port=443, String scheme='https') {
        super(hostname, username, password, port, scheme)
    }

    def doJsonPostRendered(String path, Map<String, Object> jsonData) {
        log.debug('Start JSON post with rendered data to path {}', path)

        URI uri = uriBuilder(path).build()
        String jsonRequest = JsonOutput.toJson(jsonData)

        HttpPost httpPost = new HttpPost(uri)
        httpPost.setHeader('Content-Type', CONTENT_TYPE_JSON)
        httpPost.setHeader('Accept', CONTENT_TYPE_JSON)
        httpPost.setEntity(new StringEntity(jsonRequest))

        String jsonText = doPost(httpPost)
        log.debug('Got JSON:\n{}', JsonOutput.prettyPrint(jsonText))

        slurper.parseText(jsonText)
    }

    def doJsonGetRendered(String path, Map<String, String> params = [:]) {
        log.debug('Start JSON get with rendered data to path {}', path)

        String jsonText = doGet(path, params)
        log.debug('Got JSON:\n{}', JsonOutput.prettyPrint(jsonText))

        slurper.parseText(jsonText)
    }

    def search(String jql, int startAt = 0) {
        log.info('Searching with JQL query "{}"', jql)

        search([
                jql:jql,
                startAt:startAt
        ])
    }

    def search(Map<String, Object> params, int startAt = 0) {
        log.info('Searching with params and startAt {}', startAt)

        params['startAt'] = startAt

        doJsonPostRendered('/rest/api/2/search', params)
    }

    def searchAll(Map<String, Object> params) {
        List<Object> searchResults = []

        def json = search(params)
        searchResults << json
        int startAt = json.startAt
        int lastSize = json.issues.size()
        int total = json.total

        while(!(startAt + lastSize >= total)) {
            json = search(params, startAt + lastSize)
            searchResults << json
            startAt = json.startAt
            lastSize = json.issues.size()
            total = json.total
        }

        def allIssues = []
        searchResults.each { allIssues.addAll( it.issues as Collection ) }

        return [
                startAt: 0,
                total: allIssues.size(),
                issues: allIssues
        ]
    }

    List<Field> field() {
        log.info('Retrieving fields')

        def fieldJson = doJsonGetRendered('/rest/api/2/field')

        fieldJson.collect { new Field(id: it.id, name: it.name) }
    }

    Fields fields() {
        new Fields(fields: field())
    }

    List<IssueType> issueType() {
        log.info('Retrieving issue types.')

        def typeJson = doJsonGetRendered('/rest/api/2/issuetype')

        typeJson.collect { new IssueType(
                id: it.id,
                name: it.name,
                description: it.description,
                iconUrl: it.iconUrl,
                avatarId: it.avatarId,
                subtask: it.subtask
        ) }
    }

    IssueTypes issueTypes() {
        new IssueTypes(issueTypes: issueType())
    }

    void downloadIssueTypeImages(List<IssueType> issueTypes) {
        issueTypes.each { downloadIssueTypeImage(it) }
    }

    void downloadIssueTypeImage(IssueType issueType) {
        issueType.iconImage = new Image(
                downloadUrl: issueType.iconUrl,
                namingHint: Convert.sanitizeForFileBasename(issueType.name)
        )
        downloadImage(issueType.iconImage)
    }

    List<IssueLinkType> issueLinkType() {
        log.info('Retrieving issue link types.')

        def typeJson = doJsonGetRendered('/rest/api/2/issueLinkType')

        typeJson.issueLinkTypes.collect { new IssueLinkType(
                id: it.id,
                name: it.name,
                inward: it.inward,
                outward: it.outward
        ) }
    }

    IssueLinkTypes issueLinkTypes() {
        new IssueLinkTypes(types: issueLinkType())
    }

    def issue(String issueIdOrKey) {
        log.info('Retrieving single issue with id/key {}', issueIdOrKey)

        doGet("/rest/api/2/issue/$issueIdOrKey")
    }

    void downloadIssueAttachments(List<Issue> issues) {
        issues.each { issue ->
            downloadIssueAttachments(issue)
        }
    }

    void downloadIssueAttachments(Issue issue) {
        downloadAttachments(issue.attachments)
    }

    void downloadAttachments(List<Attachment> attachments) {
        for(Attachment attachment : attachments) {
            downloadAttachment(attachment)
        }
    }

    boolean downloadAttachment(Attachment attachment) {
        log.info 'Retrieving binary data of attachment with id {}', attachment.id

        if(dryRun) {
            log.warn 'DRYRUN ACTIVE. Will mock attachment bytes for id {}', attachment.id
            attachment.bytes = DRY_RUN_BYTES.bytes
        }
        else {
            attachment.bytes = doGetBytesForUrl(attachment.content).bytes
        }
    }

    void downloadIssueImages(List<Issue> issues, ImageCache imageCache = null) {
        issues.each {
            downloadIssueImages(it, imageCache)
        }
    }

    void downloadIssueImages(Issue issue, ImageCache imageCache = null) {
        downloadImages(issue.images, imageCache)
        downloadCommentImages(issue.comments, imageCache)
    }

    void downloadCommentImages(List<Comment> comments, ImageCache imageCache = null) {
        comments.each { comment ->
            downloadCommentImages(comment, imageCache)
        }
    }

    void downloadCommentImages(Comment comment, ImageCache imageCache = null) {
        downloadImages(comment.images, imageCache)
    }

    void downloadImages(List<Image> images, ImageCache imageCache = null) {
        images.each {
            downloadImage(it, imageCache)
        }
    }

    void downloadImage(Image image, ImageCache imageCache = null) {
        log.info 'Retrieving image binary data of image with downloadUrl {}', image.downloadUrl

        byte[] bytes = imageCache?.getAt(image.downloadUrl)
        if(dryRun) {
            log.warn 'DRYRUN ACTIVE. Will mock image bytes for {}', image.downloadUrl
            image.mimeTypeBytes = DRY_RUN_BYTES
        }
        else if(bytes) {
            log.info 'Image cache hit for {}', image.downloadUrl
            image.bytes = bytes
        }
        else if(image.downloadUrl.startsWith(baseUrl)) {
            image.mimeTypeBytes = doGetBytesForUrl(image.downloadUrl)
        }
        else if(isURI(image.downloadUrl)) {
            image.mimeTypeBytes = doGetBytesForUrl(image.downloadUrl)
        }
        else {
            log.error 'Can not handle image downloadUrl "{}"', image.downloadUrl
        }

        if(imageCache && image.bytes) {
            imageCache[image.downloadUrl] = image.bytes
        }
    }

}
