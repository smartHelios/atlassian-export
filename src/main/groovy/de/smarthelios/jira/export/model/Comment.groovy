package de.smarthelios.jira.export.model

import de.smarthelios.atlassian.export.model.Image
import groovy.util.logging.Slf4j
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * A comment to a JIRA issue.
 */
@Slf4j
class Comment {
    String id
    Person author
    String bodyHtml
    Person updateAuthor
    Date created
    Date updated

    List<Image> images

    @Lazy
    Document queryDoc = Jsoup.parse(bodyHtml)

    List<String> getImageSources() {
        if(bodyHtml) {
            queryDoc.select('img')
                    .collect { it.attr('src') }
            // filter embedded images
                    .findAll { !it.startsWith('data:') }
        }
        else {
            log.error 'Trying to analyze empty bodyHtml for comment {}', id

            []
        }
    }

    List<Image> getImages() {
        if(bodyHtml) {
            // synchronize bodyHtml and images list
            if(null == images || images.size() != imageSources.size()) {
                images = imageSources.collect { new Image(downloadUrl: it) }
            }

            images
        }
        else {
            log.error 'Cannot return images for issue comment with empty bodyHtml [commentId:{}]', id

            []
        }
    }
}
