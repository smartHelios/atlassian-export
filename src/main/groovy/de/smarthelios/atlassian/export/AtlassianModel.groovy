package de.smarthelios.atlassian.export

import de.smarthelios.atlassian.export.model.Image
import de.smarthelios.atlassian.export.model.ImageExportReplacement

/**
 * JSON Serialization model for storing and loading shared model classes as raw data in exports.
 */
class AtlassianModel {

    static Map toMap(Image image) {
        [
                mimeType: image.mimeType,
                downloadUrl: image.downloadUrl,
                bytes: image.bytes ? image.bytes.encodeBase64().toString() : null,
                namingHint: image.namingHint,
                replacement: image.replacement ? toMap(image.replacement) : null
        ]
    }

    static Image imageFromMap(def imageMap) {
        if(imageMap) {
            new Image(
                    mimeType: imageMap.mimeType,
                    downloadUrl: imageMap.downloadUrl,
                    bytes: imageMap.bytes.decodeBase64(),
                    namingHint: imageMap.namingHint,
                    replacement: replacementFromMap(imageMap.replacement)
            )
        }
        else {
            null
        }
    }

    static Map toMap(ImageExportReplacement replacement) {
        [
                bytes: replacement.bytes ? replacement.bytes.encodeBase64().toString() : null,
                src: replacement.src,
                filename: replacement.filename
        ]
    }

    static ImageExportReplacement replacementFromMap(def replacementMap) {
        if(replacementMap) {
            new ImageExportReplacement(
                    bytes: replacementMap.bytes ? replacementMap.bytes.decodeBase64() : null,
                    src: replacementMap.src,
                    filename: replacementMap.filename
            )
        }
        else {
            null
        }
    }
}
