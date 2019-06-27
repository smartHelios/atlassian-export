package de.smarthelios.confluence.export.model

import de.smarthelios.atlassian.export.filter.HtmlFilter
import de.smarthelios.atlassian.export.model.Image
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import java.util.regex.Pattern

/**
 * A Confluence page.
 */
@Slf4j
class Page {

    static final Pattern EXPORTABLE_TITLE_REGEXP = ~/^(\w| |-|\.)+$/ //word characters, dots and spaces allowed

    String id
    String title
    String linkWebUi
    List<Page> children = []
    String exportView
    List<Attachment> attachments
    List<Image> images

    @Lazy
    Document queryDoc = Jsoup.parse(exportView)

    String getExportFileName() {
        if(exportableTitle) {
            // we can replace everything we want in the title for filename use
            // as uniqueness is guaranteed by attaching page id.
            StringUtils.replaceChars(title, ' .', '__') + "_${id}.html"
        }
        else {
            "${id}.html"
        }
    }

    boolean isExportableTitle() {
        EXPORTABLE_TITLE_REGEXP.matcher(title).matches()
    }

    String exportViewFiltered(List<HtmlFilter> filters) {
        filters.inject(exportView) { filtered, filter -> filter.apply(filtered) }
    }

    List<String> getImageSources() {
        if(exportView) {
            queryDoc.select('img')
                    .collect { it.attr('src') }
                    // filter embedded images
                    .findAll { !it.startsWith('data:') }
        }
        else {
            log.error 'Trying to analyze exportView image sources without content for page {}', id

            []
        }
    }

    List<String> getHrefs() {
        if(exportView) {
            queryDoc.select('a[href]')
                    .collect { it.attr('href') }
        }
        else {
            log.error 'Trying to analyze exportView hrefs without content for page {}', id

            []
        }
    }

    List<Image> getImages() {
        if(exportView) {
            // synchronize exportView content and images list
            if(null == images || images.size() != imageSources.size()) {
                images = imageSources.collect { new Image(downloadUrl: it) }
            }

            images
        }
        else {
            log.error 'Cannot return images for page without initialized exportView [pageId:{}]', id

            []
        }
    }

    static final List<Page> flatten(List<Page> pageForrest, List<Page> flat = []) {
        pageForrest.each {
            flat.add(it)
            flat.addAll(flatten(it.children))
        }

        flat
    }

}
