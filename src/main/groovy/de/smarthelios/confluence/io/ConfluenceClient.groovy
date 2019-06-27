package de.smarthelios.confluence.io

import de.smarthelios.atlassian.export.model.Image
import de.smarthelios.atlassian.io.HttpClient
import de.smarthelios.confluence.export.model.Attachment
import de.smarthelios.confluence.export.model.Page
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

/**
 * Confluence specific client to access Confluence REST API.
 */
@Slf4j
class ConfluenceClient extends HttpClient {

    //TODO: client does not implement paging.
    static final DEFAULT_RESULT_LIMIT = '100'

    private JsonSlurper slurper

    ConfluenceClient(String hostname, String username, String password, int port = 443, String scheme = 'https') {
        super(hostname, username, password, port, scheme)
        slurper = new JsonSlurper()
    }

    Page getPage(String spaceKey, String title) {
        log.info 'Retrieving spaceKey "{}" page "{}"', spaceKey, title
        def json = doGetParsedJson('/rest/api/content', [
            type: 'page',
            spaceKey: spaceKey,
            title: title,
            limit: '1'
        ])

        Page page
        if(json.results.size() < 1) {
            log.error 'No page found for spaceKey "{}" page "{}"', spaceKey, title

            page = null
        }
        else {
            page = new Page(
                    id: json.results.first().id,
                    title: json.results.first().title,
                    linkWebUi: json.results.first()._links?.webui
            )
        }

        page
    }

    void updateExportViewAndTitle(List<Page> pages) {
        Page.flatten(pages).each {
            updateExportViewAndTitle(it)
        }
    }

    void updateExportViewAndTitle(Page page) {
        log.info 'Retrieving export_view and title for page with id {}.', page.id
        def json = doGetParsedJson("/rest/api/content/${page.id}", [expand:'body.export_view'])

        page.title = json.title
        page.exportView = json.body?.export_view?.value
    }

    void addChildTrees(List<Page> pages) {
        pages.each {
            addChildTree(it)
        }
    }

    void addChildTree(Page page) {
        log.info 'Getting children of page id {}.', page.id
        def json = doGetParsedJson("/rest/api/content/${page.id}/child/page", [
                limit:DEFAULT_RESULT_LIMIT,
                expand:'title'
        ])

        json.results.each {
            Page childPage = new Page(
                    id: it.id,
                    title: it.title,
                    linkWebUi: it._links.webui
            )

            // recursion!
            addChildTree(childPage)

            page.children << childPage
        }
    }

    void addAttachments(Page page) {
        log.info 'Getting attachments of page id {}.', page.id
        def json = doGetParsedJson("/rest/api/content/${page.id}/child/attachment", [
                limit:DEFAULT_RESULT_LIMIT
        ])

        List<Attachment> attachments = []
        json.results.each {
            attachments << new Attachment(
                    id: it.id,
                    mediaType: it.metadata.mediaType,
                    fileSize: it.extensions.fileSize,
                    comment: it.extensions.comment,
                    downloadUrl: it._links.download,
                    title: it.title
            )
        }
        page.attachments = attachments
    }

    void addAttachments(List<Page> pageForrest, boolean addBytes = true) {
        Page.flatten(pageForrest).each {
            addAttachments(it)
            if(addBytes) {
                downloadAttachments(it)
            }
        }
    }

    void downloadAttachments(Page page) {
        downloadAttachments(page.attachments)
    }

    void downloadAttachments(List<Attachment> attachments) {
        for(Attachment attachment : attachments) {
            downloadAttachment(attachment)
        }
    }

    void downloadAttachment(Attachment attachment) {
        log.info 'Retrieving binary data of attachment with id {}', attachment.id
        attachment.bytes = doGetBytesForUrl(baseUrl + attachment.downloadUrl).bytes
    }

    void downloadPageImages(List<Page> pageForrest) {
        Page.flatten(pageForrest).each {
            downloadPageImages(it)
        }
    }

    void downloadPageImages(Page page) {
        downloadImages(page.images)
    }

    void downloadImages(List<Image> images) {
        images.each {
            downloadImage(it)
        }
    }

    void downloadImage(Image image) {
        log.info 'Retrieving image binary data of image with downloadUrl {}', image.downloadUrl
        if(isRoadmapPluginImgSrc(image.downloadUrl)) {
            image.mimeTypeBytes = doGetBytesForUrl(baseUrl + image.downloadUrl)
            image.namingHint = 'roadmap'
        }
        else if(isJiraConfluenceMacroImgSrc(image.downloadUrl)) {
            log.info 'Identified JIRA macro image URL "{}', image.downloadUrl
            image.namingHint = 'jira'
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
    }

    String getSpace() {
        doGet('/rest/api/space')
    }

    String getAccessMode() {
        doGet('/rest/api/accessmode')
    }

    static final boolean isRoadmapPluginImgSrc(String downloadUrl) {
        downloadUrl.startsWith('/plugins/servlet/roadmap')
    }

    static final boolean isJiraConfluenceMacroImgSrc(String downloadUrl) {
        downloadUrl in ['$iconUrl','$statusIcon']
    }
}
