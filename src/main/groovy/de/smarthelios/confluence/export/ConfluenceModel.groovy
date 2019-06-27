package de.smarthelios.confluence.export

import de.smarthelios.atlassian.export.AtlassianModel
import de.smarthelios.confluence.export.model.Attachment
import de.smarthelios.confluence.export.model.Page

/**
 * JSON Serialization model for storing and loading Confluence specific model classes as raw data in exports.
 */
class ConfluenceModel {

    static Map toMap(Page page) {
        [
                id: page.id,
                exportView: page.exportView,
                title: page.title,
                linkWebUi: page.linkWebUi,
                children: page.children.collect { toMap(it) },
                attachments: page.attachments.collect { toMap(it) },
                images: page.images.collect { AtlassianModel.toMap(it) }
        ]
    }

    static Page pageFromMap(def pageMap) {
        if(pageMap) {
            new Page(
                    id: pageMap.id,
                    exportView: pageMap.exportView,
                    title: pageMap.title,
                    linkWebUi: pageMap.linkWebUi,
                    children: pageMap.children.collect { pageFromMap(it) },
                    attachments: pageMap.attachments.collect { attachmentFromMap(it) },
                    images: pageMap.images.collect { AtlassianModel.imageFromMap(it) }
            )
        }
        else {
            null
        }
    }

    static Map toMap(Attachment attachment) {
        [
                id: attachment.id,
                mediaType: attachment.mediaType,
                fileSize: attachment.fileSize,
                comment: attachment.comment,
                downloadUrl: attachment.downloadUrl,
                title: attachment.title,
                bytes: attachment.bytes ? attachment.bytes.encodeBase64().toString() : null
        ]
    }

    static Attachment attachmentFromMap(def attachmentMap) {
        if(attachmentMap) {
            new Attachment(
                    id: attachmentMap.id,
                    mediaType: attachmentMap.mediaType,
                    fileSize: attachmentMap.fileSize,
                    comment: attachmentMap.comment,
                    downloadUrl: attachmentMap.downloadUrl,
                    title: attachmentMap.title,
                    bytes: attachmentMap.bytes.decodeBase64()
            )
        }
        else {
            null
        }
    }
}
