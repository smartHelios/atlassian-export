package de.smarthelios.confluence.export

import de.smarthelios.atlassian.export.convert.Html
import de.smarthelios.atlassian.export.filter.HtmlFilter
import de.smarthelios.atlassian.export.filter.ReplacementFilter
import de.smarthelios.atlassian.export.model.Image
import de.smarthelios.atlassian.export.model.ImageExportReplacement
import de.smarthelios.atlassian.io.Gif
import de.smarthelios.atlassian.io.Resource
import de.smarthelios.confluence.export.model.Attachment
import de.smarthelios.confluence.export.model.ExportMeta
import de.smarthelios.confluence.export.model.Page
import de.smarthelios.confluence.export.model.SpaceKeyPageTitle
import de.smarthelios.confluence.io.ConfluenceClient
import groovy.json.JsonOutput
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils

import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Export class providing functionality to export Confluence content.
 * Uses an instance of ConfluenceClient to access Confluence.
 *
 * There are two export methods. One which is only generating data from already given model objects. The other export
 * method is fetching the data from Confluence before handing over to the first one.
 * This division is made to support test and future scenarios where regeneration of HTML output without
 * fetching data may be desired.
 */
@Slf4j
class ConfluenceExport {

    static final String STYLES_DIR = 'styles'
    static final String ATTACHMENTS_DIR = 'attachments'
    static final String IMAGES_DIR = 'images'

    static final ImageExportReplacement TRANSPARENT_PIXEL = new ImageExportReplacement(
            filename: Gif.TRANSPARENT.filename,
            bytes: Gif.TRANSPARENT.bytes
    )

    private static final SimpleTemplateEngine templateEngine = new SimpleTemplateEngine()
    private static final Template pageTpl = templateEngine.createTemplate(Resource.confluenceExport('/template/page.html'))
    private static final Template indexTpl = templateEngine.createTemplate(Resource.confluenceExport('/template/index.html'))

    private static final Pattern startWithHashSign = ~/^\s*#.*/
    private static final Pattern startWithSlashPattern = ~/(?s)^\s*(?<slash>\/.*)/
    private static final Pattern isViewPageAction = ~/^\/pages\/viewpage\.action$/
    private static final Pattern isDisplayPath = ~/^\/display\/.*/


    private final ConfluenceClient confluenceClient

    ConfluenceExport(ConfluenceClient confluenceClient) {
        this.confluenceClient = confluenceClient
    }

    boolean export(String[] spaceKeyPageTitleArgs,
                   File dir,
                   ExportMeta exportMeta = new ExportMeta(),
                   List<HtmlFilter> filters = []) {
        if (dir.exists()) {
            log.error 'Export dir {} already exists. Will not export anything!', dir

            false
        }
        else {

            List<SpaceKeyPageTitle> spaceKeyPageTitles = buildSpaceKeyPageTitles(spaceKeyPageTitleArgs)
            exportMeta.spaceKeyPageTitles = spaceKeyPageTitles
            exportMeta.baseUrl = confluenceClient.baseUrl
            List<Page> roots = retrievePageForrest(spaceKeyPageTitles)

            export(roots, dir, exportMeta, filters)
        }
    }

    boolean export(List<Page> pageForrest,
                   File dir,
                   ExportMeta exportMeta = new ExportMeta(),
                   List<HtmlFilter> filters = []) {
        if (dir.exists()) {
            log.error 'Export dir {} already exists. Will not export anything!', dir

            false
        }
        else {
            log.info 'Exporting confluence content to dir {}', dir

            File attachmentsDir = new File(dir, ATTACHMENTS_DIR)
            File imagesDir = new File(dir, IMAGES_DIR)
            File cssDir = new File(dir, STYLES_DIR)

            dir.mkdirs()
            attachmentsDir.mkdirs()
            imagesDir.mkdirs()
            cssDir.mkdirs()

            processImages(pageForrest)

            exportJson(pageForrest, dir)

            generateStyles(cssDir)
            generateIndex(pageForrest, dir, exportMeta)

            exportAttachments(pageForrest, attachmentsDir)
            exportImages(pageForrest, imagesDir)

            List<HtmlFilter> pageFilters = buildStandardFilters(pageForrest, confluenceClient.baseUrl) + filters
            exportPages(pageForrest, dir, pageFilters)

            true
        }
    }

    List<Page> retrievePageForrest(List<SpaceKeyPageTitle> spaceKeyPageTitles) {
        List<Page> roots = []
        spaceKeyPageTitles.each {
            Page page = confluenceClient.getPage(it.spaceKey, it.pageTitle)
            if(page) {
                roots << page
            }
        }

        confluenceClient.addChildTrees(roots)
        confluenceClient.updateExportViewAndTitle(roots)
        confluenceClient.addAttachments(roots)
        confluenceClient.downloadPageImages(roots)

        roots
    }

    static void generateStyles(File cssDir) {
        log.info 'Generating style files.'
        new File(cssDir, 'nav.css').text = Resource.atlassianExport('/style/nav.css')
        new File(cssDir, 'confluence-batch.css').text = Resource.confluenceExport('/style/confluence-batch.css')
    }

    static void generateIndex(List<Page> pageForrest, File dir, ExportMeta exportMeta) {
        log.info 'Generating page index.'
        new File(dir, 'index.html').text = htmlIndex(pageForrest, exportMeta)
    }

    static void exportPages(List<Page> pageForrest, File dir, List<HtmlFilter> filters = []) {
        Page.flatten(pageForrest).each { page ->
            log.info('Exporting page with title "{}"', page.title)

            Map<String,String> imageSrcReplacements = buildImageSrcReplacements(page)
            HtmlFilter imageSrcFilter = new ReplacementFilter(imageSrcReplacements)

            new File(dir, page.exportFileName).text = htmlPage(page, pageForrest, filters + imageSrcFilter)
        }
    }

    static void processImages(List<Page> pageForrest) {
        def pages = Page.flatten(pageForrest)

        pages.each { imagePage ->
            imagePage.images.each { image ->

                if(ConfluenceClient.isJiraConfluenceMacroImgSrc(image.downloadUrl)) {
                    log.info 'Will replace JIRA macro image "{}" by transparent pixel gif', image.downloadUrl
                    image.replacement = TRANSPARENT_PIXEL
                }
                else {
                    // prefer image page attachments in search for replacements
                    def pageSearchList = [imagePage] + (pages - imagePage)

                    pageSearchList.each { attachmentPage ->
                        attachmentPage.attachments.each { attachment ->
                            if (image.bytes == attachment.bytes) {
                                log.info 'Will replace image "{}" by equal attachment "{}"', image.downloadUrl, attachment.exportFileName
                                image.replacement = new ImageExportReplacement(
                                        src: attachmentExportPath(attachmentPage, attachment)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    static void exportJson(List<Page> pageForrest, File dir) {
        File jsonExportFile = new File(dir, 'atlassian-confluence-export.json')
        jsonExportFile.text = JsonOutput.prettyPrint(
                JsonOutput.toJson(pageForrest.collect { ConfluenceModel.toMap(it) })
        )
    }

    static void exportImages(List<Page> pageForrest, File dir) {
        Page.flatten(pageForrest).each { page ->

            File imagesDir = new File(dir, page.id)
            imagesDir.mkdirs()

            page.images.each { image ->

                if(image.replacement) {
                    if(image.replacement.bytes) {
                        log.info('Exporting replacement image {} for pageId={} image {}', image.replacement.filename, page.id, image.downloadUrl)
                        new File(imagesDir, image.replacement.filename).bytes = image.replacement.bytes
                    }
                    else {
                        log.info('Export ignores data for pageId={} image {}.', page.id, image.downloadUrl)
                    }
                }
                else {
                    log.info('Exporting page[id:{}] image {}', page.id, image.downloadUrl)
                    new File(imagesDir, image.exportFilename).bytes = image.bytes
                }
            }
        }
    }

    static void exportAttachments(List<Page> pageForrest, File dir) {
        Page.flatten(pageForrest).each { page ->

            File attachmentsDir = new File(dir, page.id)
            attachmentsDir.mkdirs()

            page.attachments.each { attachment ->
                log.info('Exporting attachment "{}" of page "{}"', attachment.title, page.title)

                new File(attachmentsDir, attachment.exportFileName).bytes = attachment.bytes
            }
        }
    }

    static String htmlIndex(List<Page> pageForrest, ExportMeta exportMeta) {
        Map binding = [
                stylesDir: STYLES_DIR,
                html: new Html(),
                nav: htmlMenu(pageForrest),
                exportMeta: exportMeta
        ]
        indexTpl.make(binding).toString()
    }

    static String htmlPage(Page page, List<Page> pageForrest = [], List<HtmlFilter> filters) {
        Map binding = [
                page: page,
                stylesDir: STYLES_DIR,
                nav: htmlMenu(pageForrest, page.id),
                content: page.exportViewFiltered(filters),
                attachments: htmlAttachmentTable(page)
        ]
        pageTpl.make(binding).toString()
    }

    static String htmlMenu(List<Page> pages, String currentPageId = '', MarkupBuilder builder = null) {
        StringWriter writer = null

        boolean isMenuRoot
        if(!builder) {
            writer = new StringWriter()
            builder = new MarkupBuilder(writer)
            isMenuRoot = true
        }
        else {
            isMenuRoot = false
        }

        if(pages) {
            builder.ul {

                if(isMenuRoot) {
                    li {
                        a(href:'index.html', 'Page Index')
                    }
                }

                pages.each { Page page ->
                    li {

                        if(page.id == currentPageId) {
                            b(class:"currentPage", page.title)
                        }
                        else {
                            a(href:page.exportFileName, page.title)
                        }

                        htmlMenu(page.children, currentPageId, builder)
                    }
                }
            }
        }
        else {
            builder.mkp.yield('')
        }

        writer ? writer.toString() : ''
    }

    static String htmlAttachmentTable(Page page) {
        StringWriter writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)

        builder.h4('Attachments')
        builder.div(class:'table-wrap') {
            table(class:'confluenceTable') {
                tbody {
                    tr {
                        th(class:'confluenceTh', 'Title')
                        th(class:'confluenceTh', 'Comment')
                    }

                    if(page.attachments) {
                        for(Attachment attachment : page.attachments) {
                            tr {
                                td(class:'confluenceTd') {
                                    a(href:attachmentExportPath(page, attachment), attachment.title)
                                }
                                td(class:'confluenceTd', attachment.comment)
                            }
                        }
                    }
                    else {
                        tr {
                            td(class:'confluenceTd',colspan:2, 'No attachments available.')
                        }
                    }
                }
            }
        }

        writer.toString()
    }

    static String attachmentExportPath(Page page, Attachment attachment) {
        "$ATTACHMENTS_DIR/${page.id}/${attachment.exportFileName}"
    }

    static String imageExportPath(Page page, Image image) {
        "$IMAGES_DIR/${page.id}/${image.exportFilename}"
    }

    static String imageExportPath(Page page, ImageExportReplacement exportReplacement) {
        "$IMAGES_DIR/${page.id}/${exportReplacement.filename}"
    }

    static Map<String,String> buildImageSrcReplacements(Page page) {
        page.images.collectEntries { image ->

            String newSrc
            if (image.replacement) {
                newSrc = image.replacement.src ?: imageExportPath(page, image.replacement)
            } else {
                newSrc = imageExportPath(page, image)
            }

            ["src=\"${image.downloadUrl}\"".toString(), "src=\"${newSrc}\"".toString()]
        }
    }

    static List<HtmlFilter> buildStandardFilters(List<Page> pageForrest,
                                                 String confluenceBaseUrl) {
        List<HtmlFilter> list = [
                buildHrefFilter(pageForrest, confluenceBaseUrl)
        ]
        list
    }

    static List<SpaceKeyPageTitle> buildSpaceKeyPageTitles(String[] args) {
        List<SpaceKeyPageTitle> spaceKeyPageTitles = []

        if(args.size() % 2 == 1) {
            log.warn 'Odd number of spaceKey/pageTitle parameters given. Will ignore "{}".', args.last()
        }

        int tupleCount = args.size().intdiv(2).toInteger()
        for(int i in 0..<tupleCount) {
            spaceKeyPageTitles << new SpaceKeyPageTitle(spaceKey: args[i*2], pageTitle: args[i*2+1])
        }

        spaceKeyPageTitles
    }


    static HtmlFilter buildHrefFilter(List<Page> pageForrest, String baseUrl) {

        // when do we have to replace an href?
        // if it starts with a hash sign then ignore it as local anchor ref
        // else
        //   if it starts with a "/", then it is sanitized replacement = baseURl + href
        //   afterwards:
        //   if it matches baseUrl + /pages/viewpage.action?pageId=<exported_page_id>,
        //     then
        //       identify page by pageId,
        //       save a potentially existing anchor
        //       and replacement = <exportFilename>#anchor
        //   if it matches baseUrl + /display/<exported_page_link_web_ui>,
        //     then
        //       identify page by /display path
        //       save a potentially existing anchor
        //       and replacement = <exportFilename>#anchor
        //   otherwise: keep original href

        List<Page> pages = Page.flatten(pageForrest)

        List<String> searchList = []
        List<String> replacementList = []
        pages.each { page ->
            log.debug 'Checking page [id:{},title:"{}",linkWebUi:"{}"] for replaceable hrefs.', page.id, page.title, page.linkWebUi

            page.hrefs.each { href ->
                log.debug 'Start checking href "{}".', href

                if(href ==~ startWithHashSign) {
                    log.info 'Will not replace identified page local section link "{}".', href
                }
                else {

                    String replacement
                    Matcher startWithSlash = startWithSlashPattern.matcher(href)
                    if (startWithSlash.matches()) {
                        replacement = baseUrl + startWithSlash.group('slash')
                    } else {
                        replacement = href
                    }

                    URL url
                    try {
                        url = replacement.toURL()
                    }
                    catch (MalformedURLException ignored) {
                        log.error 'Found malformed URL in href="{}" of page "{}".', href, page.title
                        url = null
                    }

                    if (url && replacement.startsWith(baseUrl)) {

                        if (url.path ==~ isViewPageAction) {
                            List<NameValuePair> queryParams = URLEncodedUtils.parse(url.toURI(), StandardCharsets.UTF_8)
                            // TODO: check for potential problem with non UTF-8 encoded confluence content
                            String pageId = queryParams.find { it.name == 'pageId' }?.value
                            String anchorRef = url.ref

                            Page replacementPage = pages.find { it.id == pageId }
                            if (replacementPage) {
                                replacement = replacementPage.exportFileName + (anchorRef ?: '')
                            }
                        } else if (url.path ==~ isDisplayPath) {
                            String anchorRef = url.ref

                            Page replacementPage = pages.find { it.linkWebUi == url.path }
                            if (replacementPage) {
                                replacement = replacementPage.exportFileName + (anchorRef ?: '')
                            }
                        }
                    }

                    if (replacement == href) {
                        log.debug 'No replacement for href "{}"', href
                    } else {
                        log.debug 'Found replacement for href: "{}" to "{}"', href, replacement
                        searchList << /href="$href"/.toString()
                        replacementList << /href="$replacement"/.toString()
                        // because jSoup sanitizes ampersands in attributes we have to add the ampersand entities version too
                        searchList << /href="${href.replaceAll(/&/, /&amp;/)}"/.toString()
                        replacementList << /href="$replacement"/.toString()
                    }

                }

            }
        }

        assert searchList.size() == replacementList.size(), 'There should never be a result where searchList and replacementList differ in size.'

        new ReplacementFilter(searchList, replacementList)
    }
}
